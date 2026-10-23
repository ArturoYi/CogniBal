package com.example.cognibal.data.backup.bluetooth

import com.example.cognibal.data.backup.BackupException
import com.example.cognibal.data.backup.SealedVault
import com.example.cognibal.data.backup.VaultTransfer
import com.example.cognibal.data.database.AppDatabase
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 连上之后的迁移步骤：核对身份、交换一次性密钥、两边确认 6 位数字，
 * 接收端再确认整库替换，然后发送协议 2 的加密包。发送端的原库不在这里改。
 */
internal class MigrationSession(
    private val vault: VaultTransfer,
    private val localIdentity: ByteArray,
    private val gate: MigrationGate,
    private val workDirectory: File,
) {
    suspend fun runSender(link: BleLink) {
        gate.onConnected()
        val remote = MigrationProtocol.parseIdentity(link.remoteIdentity(), "identity")
        rejectSelf(remote.deviceId)
        link.send(MigrationFrame.Identity(localIdentity))
        exchange(link, sender = true)
    }

    suspend fun runReceiver(link: BleLink) {
        gate.onConnected()
        val announced = link.receive() as? MigrationFrame.Identity
            ?: throw MigrationException(MigrationException.Kind.Protocol, "identity", "对方没有报上身份")
        val remote = MigrationProtocol.parseIdentity(announced.bytes, "identity")
        rejectSelf(remote.deviceId)
        exchange(link, sender = false)
    }

    private suspend fun exchange(link: BleLink, sender: Boolean) {
        val keys = MigrationCrypto.ephemeral()
        var material: SessionMaterial? = null
        var sealed: SealedVault? = null
        try {
            val peerPublic = if (sender) {
                link.send(MigrationFrame.Pubkey(keys.publicKey))
                (link.receive() as? MigrationFrame.Pubkey)?.bytes
            } else {
                val received = (link.receive() as? MigrationFrame.Pubkey)?.bytes
                link.send(MigrationFrame.Pubkey(keys.publicKey))
                received
            } ?: throw MigrationException(MigrationException.Kind.Protocol, "confirm", "没有收到对方公钥")
            val secret = keys.sharedSecret(peerPublic)
            material = MigrationCrypto.session(secret, keys.publicKey, peerPublic)
            secret.wipe()
            confirmDigits(link, material.digits)
            if (sender) {
                gate.onWaitReplace()
                when (val reply = link.receive()) {
                    MigrationFrame.ReplaceOk -> Unit
                    MigrationFrame.Abort -> rejected("对方取消了替换")
                    else -> protocol("replace", "没有等到替换确认")
                }
                sealed = sendVault(link, material)
            } else {
                gate.onWaitReplace()
                val accepted = gate.confirmReplace()
                if (!accepted) {
                    link.send(MigrationFrame.Abort)
                    rejected("没有确认替换")
                }
                link.send(MigrationFrame.ReplaceOk)
                receiveVault(link, material)
            }
        } finally {
            material?.key?.wipe()
            keys.publicKey.wipe()
            sealed?.close()
        }
    }

    private suspend fun confirmDigits(link: BleLink, digits: String) {
        gate.onCode(digits)
        try {
            withTimeout(MigrationProtocol.CONFIRM_MILLIS) {
                coroutineScope {
                    val peer = async { link.receive() }
                    val same = gate.confirmCode()
                    if (!same) {
                        link.send(MigrationFrame.Mismatch)
                        rejected("数字不一致")
                    }
                    link.send(MigrationFrame.Match)
                    when (val reply = peer.await()) {
                        MigrationFrame.Match -> Unit
                        MigrationFrame.Mismatch,
                        MigrationFrame.Abort,
                        -> rejected("对方中止了核对")
                        else -> protocol("confirm", "核对回应无法识别")
                    }
                }
            }
        } catch (exception: TimeoutCancellationException) {
            withContext(NonCancellable) {
                try {
                    link.send(MigrationFrame.Abort)
                } catch (_: Exception) {
                }
            }
            throw MigrationException(MigrationException.Kind.Timeout, "confirm", "核对超时", exception)
        }
    }

    private suspend fun sendVault(link: BleLink, material: SessionMaterial): SealedVault {
        gate.onTransfer()
        val envelope = File(workDirectory, "envelope.bin")
        val sealed = try {
            vault.writeEncrypted(envelope)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            envelope.delete()
            throw MigrationException(MigrationException.Kind.Import, "seal", "无法准备整库", exception)
        }
        if (sealed.size <= 0L || sealed.size > MigrationProtocol.MAX_TRANSFER_BYTES) {
            sealed.close()
            link.send(MigrationFrame.Reject(MigrationProtocol.REJECT_TOO_LARGE))
            throw MigrationException(MigrationException.Kind.TooLarge, "seal", "整库超过 96 MiB")
        }
        try {
            val wrapped = MigrationCrypto.wrapKey(material.key, sealed.key)
            try {
                link.send(
                    MigrationFrame.Offer(
                        size = sealed.size,
                        sha256 = sealed.sha256,
                        schema = sealed.schema,
                        wrappedKey = wrapped,
                    ),
                )
            } finally {
                wrapped.wipe()
            }
            when (val reply = link.receive()) {
                MigrationFrame.Accept -> Unit
                is MigrationFrame.Reject -> throw reply.toFailure()
                MigrationFrame.Abort -> rejected("对方中止了迁移")
                else -> protocol("transfer", "对方没有接受整库")
            }
            sealed.file.inputStream().use { input ->
                var sent = 0L
                val buffer = ByteArray(MigrationProtocol.WINDOW_BYTES)
                while (sent < sealed.size) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    link.writeData(buffer, read)
                    sent += read
                    when (val ack = link.receive()) {
                        is MigrationFrame.Ack -> if (ack.received != sent) {
                            protocol("transfer", "确认的进度对不上")
                        }
                        is MigrationFrame.Reject -> throw ack.toFailure()
                        MigrationFrame.Abort -> rejected("对方中止了迁移")
                        else -> protocol("transfer", "缺少数据确认")
                    }
                    gate.onProgress(sent, sealed.size)
                }
                if (sent != sealed.size) protocol("transfer", "整库没有发完")
            }
            link.send(MigrationFrame.Finish)
            when (val done = link.receive()) {
                MigrationFrame.DoneOk -> Unit
                is MigrationFrame.Reject -> throw done.toFailure()
                MigrationFrame.Abort -> rejected("对方中止了迁移")
                else -> protocol("import", "对方没有确认导入")
            }
            return sealed
        } catch (exception: Exception) {
            sealed.close()
            throw exception
        }
    }

    private suspend fun receiveVault(link: BleLink, material: SessionMaterial) {
        gate.onTransfer()
        val offer = when (val frame = link.receive()) {
            is MigrationFrame.Offer -> frame
            is MigrationFrame.Reject -> throw frame.toFailure()
            MigrationFrame.Abort -> rejected("对方中止了迁移")
            else -> protocol("transfer", "没有收到整库说明")
        }
        if (offer.size <= 0L || offer.size > MigrationProtocol.MAX_TRANSFER_BYTES || offer.sha256.size != 32) {
            link.send(MigrationFrame.Reject(MigrationProtocol.REJECT_TOO_LARGE))
            throw MigrationException(MigrationException.Kind.TooLarge, "transfer", "整库超过 96 MiB")
        }
        if (offer.schema > AppDatabase.VERSION) {
            link.send(MigrationFrame.Reject(MigrationProtocol.REJECT_SCHEMA))
            throw MigrationException(MigrationException.Kind.Schema, "transfer", "对方的数据版本更新")
        }
        link.send(MigrationFrame.Accept)
        val envelope = File(workDirectory, "envelope.bin")
        if (envelope.exists()) envelope.delete()
        link.receiveData(envelope, offer.size) { received ->
            link.send(MigrationFrame.Ack(received))
            gate.onProgress(received, offer.size)
        }
        when (val finish = link.receive()) {
            MigrationFrame.Finish -> Unit
            is MigrationFrame.Reject -> throw finish.toFailure()
            MigrationFrame.Abort -> rejected("对方中止了迁移")
            else -> protocol("transfer", "对方没有结束发送")
        }
        val actual = MigrationCrypto.sha256(envelope)
        if (!MessageDigest.isEqual(actual, offer.sha256)) {
            link.send(MigrationFrame.Reject(MigrationProtocol.REJECT_HASH))
            throw MigrationException(MigrationException.Kind.Hash, "import", "整库校验没有通过")
        }
        val dataKey = MigrationCrypto.unwrapKey(material.key, offer.wrappedKey)
        var staged = false
        try {
            vault.importEncrypted(envelope, dataKey, offer.schema)
            staged = true
            link.send(MigrationFrame.DoneOk)
        } catch (exception: BackupException.NewerDatabase) {
            link.send(MigrationFrame.Reject(MigrationProtocol.REJECT_SCHEMA))
            throw MigrationException(MigrationException.Kind.Schema, "import", "对方的数据版本更新", exception)
        } catch (exception: CancellationException) {
            if (staged) vault.discardPending()
            throw exception
        } catch (exception: MigrationException) {
            if (staged) vault.discardPending()
            if (!staged) link.send(MigrationFrame.Reject(rejectReason(exception.kind)))
            throw exception
        } catch (exception: Exception) {
            if (staged) vault.discardPending()
            if (!staged) {
                link.send(MigrationFrame.Reject(MigrationProtocol.REJECT_IMPORT))
            }
            throw MigrationException(MigrationException.Kind.Import, "import", "导入没有完成", exception)
        } finally {
            dataKey.wipe()
            envelope.delete()
        }
    }

    private fun rejectSelf(deviceId: ByteArray) {
        val local = localIdentity.copyOfRange(5, MigrationProtocol.IDENTITY_BYTES)
        if (deviceId.contentEquals(local)) {
            throw MigrationException(MigrationException.Kind.Self, "identity", "连到了本机")
        }
    }

    private fun rejected(message: String): Nothing {
        throw MigrationException(MigrationException.Kind.Rejected, "confirm", message)
    }

    private fun protocol(phase: String, message: String): Nothing {
        throw MigrationException(MigrationException.Kind.Protocol, phase, message)
    }

    private fun rejectReason(kind: MigrationException.Kind): Int = when (kind) {
        MigrationException.Kind.Schema -> MigrationProtocol.REJECT_SCHEMA
        MigrationException.Kind.Hash -> MigrationProtocol.REJECT_HASH
        MigrationException.Kind.TooLarge -> MigrationProtocol.REJECT_TOO_LARGE
        MigrationException.Kind.Version,
        MigrationException.Kind.Protocol,
        -> MigrationProtocol.REJECT_PROTOCOL
        else -> MigrationProtocol.REJECT_IMPORT
    }

    private fun MigrationFrame.Reject.toFailure(): MigrationException {
        val kind = when (reason) {
            MigrationProtocol.REJECT_SCHEMA -> MigrationException.Kind.Schema
            MigrationProtocol.REJECT_HASH -> MigrationException.Kind.Hash
            MigrationProtocol.REJECT_TOO_LARGE -> MigrationException.Kind.TooLarge
            MigrationProtocol.REJECT_IMPORT -> MigrationException.Kind.Import
            else -> MigrationException.Kind.Protocol
        }
        return MigrationException(kind, "transfer", "对方拒绝了这次迁移")
    }
}

internal interface MigrationGate {
    fun onConnected()

    fun onCode(digits: String)

    suspend fun confirmCode(): Boolean

    fun onWaitReplace()

    suspend fun confirmReplace(): Boolean

    fun onTransfer()

    fun onProgress(sent: Long, total: Long)
}
