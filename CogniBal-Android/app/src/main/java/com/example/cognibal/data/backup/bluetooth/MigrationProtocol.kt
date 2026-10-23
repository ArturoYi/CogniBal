package com.example.cognibal.data.backup.bluetooth

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * 两台本应用之间的迁移帧。
 *
 * 身份特征是 20 字节：魔数 BLCK、协议版本、15 字节设备号。
 * 控制帧是大端长度、1 字节类型和载荷。版本不一致就断开。
 */
internal object MigrationProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("626c636b-0002-4d69-8000-000000000001")
    val IDENTITY_UUID: UUID = UUID.fromString("626c636b-0002-4d69-8000-000000000002")
    val CONTROL_UUID: UUID = UUID.fromString("626c636b-0002-4d69-8000-000000000003")
    val DATA_UUID: UUID = UUID.fromString("626c636b-0002-4d69-8000-000000000004")
    const val CCCD_UUID: String = "00002902-0000-1000-8000-00805f9b34fb"

    const val PROTOCOL_VERSION: Int = 2
    const val IDENTITY_BYTES: Int = 20
    const val DEVICE_ID_BYTES: Int = 15
    const val WINDOW_BYTES: Int = 16 * 1024
    const val MAX_FRAME: Int = 512
    const val CONFIRM_MILLIS: Long = 60_000L
    const val SCAN_MILLIS: Long = 60_000L
    /** 96 MiB。更大的整库改走文件导出。 */
    const val MAX_TRANSFER_BYTES: Long = 96L * 1024L * 1024L

    const val REJECT_PROTOCOL: Int = 1
    const val REJECT_SCHEMA: Int = 2
    const val REJECT_HASH: Int = 3
    const val REJECT_TOO_LARGE: Int = 4
    const val REJECT_IMPORT: Int = 5

    private val magic = byteArrayOf('B'.code.toByte(), 'L'.code.toByte(), 'C'.code.toByte(), 'K'.code.toByte())

    private const val TYPE_PUBKEY: Int = 1
    private const val TYPE_IDENTITY: Int = 2
    private const val TYPE_MATCH: Int = 3
    private const val TYPE_MISMATCH: Int = 4
    private const val TYPE_REPLACE_OK: Int = 5
    private const val TYPE_ABORT: Int = 6
    private const val TYPE_OFFER: Int = 7
    private const val TYPE_ACCEPT: Int = 8
    private const val TYPE_REJECT: Int = 9
    private const val TYPE_FINISH: Int = 10
    private const val TYPE_DONE_OK: Int = 11
    private const val TYPE_ACK: Int = 12

    fun identity(deviceId: ByteArray): ByteArray {
        check(deviceId.size == DEVICE_ID_BYTES)
        return magic + byteArrayOf(PROTOCOL_VERSION.toByte()) + deviceId
    }

    fun parseIdentity(bytes: ByteArray, phase: String): DeviceIdentity {
        if (bytes.size != IDENTITY_BYTES || !bytes.copyOfRange(0, 4).contentEquals(magic)) {
            throw MigrationException(MigrationException.Kind.Protocol, phase, "身份特征不是本应用")
        }
        val version = bytes[4].toInt() and 0xFF
        if (version != PROTOCOL_VERSION) {
            throw MigrationException(MigrationException.Kind.Version, phase, "对方协议版本不一致")
        }
        return DeviceIdentity(bytes.copyOfRange(5, IDENTITY_BYTES))
    }

    fun encode(frame: MigrationFrame): ByteArray {
        val payload = payloadOf(frame)
        if (1 + payload.size > MAX_FRAME) {
            throw MigrationException(MigrationException.Kind.Protocol, "transfer", "控制帧过长")
        }
        val out = ByteBuffer.allocate(4 + 1 + payload.size).order(ByteOrder.BIG_ENDIAN)
        out.putInt(1 + payload.size)
        out.put(typeOf(frame).toByte())
        out.put(payload)
        return out.array()
    }

    fun decode(body: ByteArray): MigrationFrame {
        if (body.isEmpty()) throw MigrationException(MigrationException.Kind.Protocol, "transfer", "控制帧是空的")
        val type = body[0].toInt() and 0xFF
        val payload = body.copyOfRange(1, body.size)
        return when (type) {
            TYPE_PUBKEY -> {
                if (payload.size != 32) protocol()
                MigrationFrame.Pubkey(payload.copyOf())
            }
            TYPE_IDENTITY -> {
                parseIdentity(payload, "identity")
                MigrationFrame.Identity(payload.copyOf())
            }
            TYPE_MATCH -> empty(payload, MigrationFrame.Match)
            TYPE_MISMATCH -> empty(payload, MigrationFrame.Mismatch)
            TYPE_REPLACE_OK -> empty(payload, MigrationFrame.ReplaceOk)
            TYPE_ABORT -> empty(payload, MigrationFrame.Abort)
            TYPE_OFFER -> decodeOffer(payload)
            TYPE_ACCEPT -> empty(payload, MigrationFrame.Accept)
            TYPE_REJECT -> {
                if (payload.size != 1) protocol()
                MigrationFrame.Reject(payload[0].toInt() and 0xFF)
            }
            TYPE_FINISH -> empty(payload, MigrationFrame.Finish)
            TYPE_DONE_OK -> empty(payload, MigrationFrame.DoneOk)
            TYPE_ACK -> {
                if (payload.size != 8) protocol()
                MigrationFrame.Ack(ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN).long)
            }
            else -> protocol()
        }
    }

    private fun decodeOffer(payload: ByteArray): MigrationFrame.Offer {
        if (payload.size < 8 + 32 + 4 + 2) protocol()
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN)
        val size = buffer.long
        val hash = ByteArray(32)
        buffer.get(hash)
        val schema = buffer.int
        val wrappedSize = buffer.short.toInt() and 0xFFFF
        if (wrappedSize > 128 || buffer.remaining() != wrappedSize) protocol()
        val wrapped = ByteArray(wrappedSize)
        buffer.get(wrapped)
        return MigrationFrame.Offer(size, hash, schema, wrapped)
    }

    private fun payloadOf(frame: MigrationFrame): ByteArray = when (frame) {
        is MigrationFrame.Pubkey -> frame.bytes
        is MigrationFrame.Identity -> frame.bytes
        MigrationFrame.Match,
        MigrationFrame.Mismatch,
        MigrationFrame.ReplaceOk,
        MigrationFrame.Abort,
        MigrationFrame.Accept,
        MigrationFrame.Finish,
        MigrationFrame.DoneOk,
        -> ByteArray(0)
        is MigrationFrame.Offer -> bytes(8 + 32 + 4 + 2 + frame.wrappedKey.size) {
            putLong(frame.size)
            put(frame.sha256)
            putInt(frame.schema)
            putShort(frame.wrappedKey.size.toShort())
            put(frame.wrappedKey)
        }
        is MigrationFrame.Reject -> byteArrayOf(frame.reason.toByte())
        is MigrationFrame.Ack -> bytes(8) { putLong(frame.received) }
    }

    private fun typeOf(frame: MigrationFrame): Int = when (frame) {
        is MigrationFrame.Pubkey -> TYPE_PUBKEY
        is MigrationFrame.Identity -> TYPE_IDENTITY
        MigrationFrame.Match -> TYPE_MATCH
        MigrationFrame.Mismatch -> TYPE_MISMATCH
        MigrationFrame.ReplaceOk -> TYPE_REPLACE_OK
        MigrationFrame.Abort -> TYPE_ABORT
        is MigrationFrame.Offer -> TYPE_OFFER
        MigrationFrame.Accept -> TYPE_ACCEPT
        is MigrationFrame.Reject -> TYPE_REJECT
        MigrationFrame.Finish -> TYPE_FINISH
        MigrationFrame.DoneOk -> TYPE_DONE_OK
        is MigrationFrame.Ack -> TYPE_ACK
    }

    private fun empty(payload: ByteArray, frame: MigrationFrame): MigrationFrame {
        if (payload.isNotEmpty()) protocol()
        return frame
    }

    private fun protocol(): Nothing {
        throw MigrationException(MigrationException.Kind.Protocol, "transfer", "控制帧无法识别")
    }

    private fun bytes(size: Int, fill: ByteBuffer.() -> Unit): ByteArray {
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        buffer.fill()
        return buffer.array()
    }
}

internal data class DeviceIdentity(val deviceId: ByteArray)

internal sealed interface MigrationFrame {
    data class Pubkey(val bytes: ByteArray) : MigrationFrame
    data class Identity(val bytes: ByteArray) : MigrationFrame
    data object Match : MigrationFrame
    data object Mismatch : MigrationFrame
    data object ReplaceOk : MigrationFrame
    data object Abort : MigrationFrame
    data class Offer(
        val size: Long,
        val sha256: ByteArray,
        val schema: Int,
        val wrappedKey: ByteArray,
    ) : MigrationFrame
    data object Accept : MigrationFrame
    data class Reject(val reason: Int) : MigrationFrame
    data object Finish : MigrationFrame
    data object DoneOk : MigrationFrame
    data class Ack(val received: Long) : MigrationFrame
}

class MigrationException(
    val kind: Kind,
    val phase: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    enum class Kind {
        Unavailable,
        Permission,
        Disabled,
        Connection,
        Protocol,
        Version,
        Self,
        TooLarge,
        Schema,
        Hash,
        Import,
        Rejected,
        Timeout,
        NotFound,
    }
}
