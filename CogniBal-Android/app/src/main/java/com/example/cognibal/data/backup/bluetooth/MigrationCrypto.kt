package com.example.cognibal.data.backup.bluetooth

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters

/**
 * 一次迁移用的临时密钥。
 *
 * 双方各生成一把 X25519，推出同一个会话密钥和同一组 6 位数字。
 * 数据密钥只用这个会话密钥包起来传给对方，不进口令备份，也不写进文件。
 */
internal object MigrationCrypto {
    private val random = SecureRandom()
    private val keyAad = "CogniBal-data-key-v2".encodeToByteArray()
    private val envelopeMagic = byteArrayOf('C'.code.toByte(), 'G'.code.toByte(), 'B'.code.toByte(), 'L'.code.toByte())

    fun ephemeral(): EphemeralExchange = EphemeralExchange.generate()

    fun session(sharedSecret: ByteArray, localPublic: ByteArray, peerPublic: ByteArray): SessionMaterial {
        val salt = sha256(canonical(localPublic, peerPublic))
        val sessionKey = hkdf(sharedSecret, salt, "CogniBal-migrate-v2".encodeToByteArray(), 32)
        val codeBytes = hkdf(sessionKey, ByteArray(0), "CogniBal-migrate-code".encodeToByteArray(), 4)
        val number = ((codeBytes[0].toLong() and 0xFF) shl 24) or
            ((codeBytes[1].toLong() and 0xFF) shl 16) or
            ((codeBytes[2].toLong() and 0xFF) shl 8) or
            (codeBytes[3].toLong() and 0xFF)
        codeBytes.wipe()
        val digits = (number % 1_000_000L).toString().padStart(6, '0')
        return SessionMaterial(sessionKey, digits)
    }

    fun dataKey(): ByteArray = randomBytes(32)

    fun wrapKey(sessionKey: ByteArray, dataKey: ByteArray): ByteArray {
        return encryptBytes(sessionKey, dataKey, keyAad)
    }

    fun unwrapKey(sessionKey: ByteArray, wrapped: ByteArray): ByteArray {
        val key = decryptBytes(sessionKey, wrapped, keyAad)
        if (key.size != 32) {
            key.wipe()
            throw MigrationException(MigrationException.Kind.Protocol, "import", "数据密钥长度不对")
        }
        return key
    }

    /** 信封是魔数、版本、随机数和 AES-GCM 密文。返回值是整份文件的 SHA-256。 */
    fun seal(dataKey: ByteArray, plain: File, destination: File): ByteArray {
        val nonce = randomBytes(12)
        val cipher = cipher(Cipher.ENCRYPT_MODE, dataKey, nonce)
        val digest = MessageDigest.getInstance("SHA-256")
        FileOutputStream(destination).use { output ->
            val header = envelopeMagic + byteArrayOf(MigrationProtocol.PROTOCOL_VERSION.toByte()) + nonce
            output.write(header)
            digest.update(header)
            FileInputStream(plain).use { input ->
                val buffer = ByteArray(STREAM_BUFFER)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    val produced = cipher.update(buffer, 0, read)
                    if (produced != null && produced.isNotEmpty()) {
                        output.write(produced)
                        digest.update(produced)
                    }
                }
            }
            val tail = cipher.doFinal()
            output.write(tail)
            digest.update(tail)
        }
        nonce.wipe()
        return digest.digest()
    }

    fun open(dataKey: ByteArray, source: File, destination: File) {
        FileInputStream(source).use { input ->
            val header = ByteArray(ENVELOPE_HEADER)
            readFully(input, header)
            if (!header.copyOfRange(0, 4).contentEquals(envelopeMagic)) {
                throw MigrationException(MigrationException.Kind.Protocol, "import", "整库信封无法识别")
            }
            val version = header[4].toInt() and 0xFF
            if (version != MigrationProtocol.PROTOCOL_VERSION) {
                throw MigrationException(MigrationException.Kind.Version, "import", "整库协议版本不一致")
            }
            val nonce = header.copyOfRange(5, ENVELOPE_HEADER)
            val cipher = cipher(Cipher.DECRYPT_MODE, dataKey, nonce)
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(STREAM_BUFFER)
                var pending = ByteArray(0)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    val combined = pending + buffer.copyOf(read)
                    if (combined.size <= GCM_TAG) {
                        pending = combined
                        continue
                    }
                    val produce = combined.size - GCM_TAG
                    val plain = cipher.update(combined, 0, produce)
                    if (plain != null && plain.isNotEmpty()) output.write(plain)
                    pending = combined.copyOfRange(produce, combined.size)
                }
                val finalBytes = cipher.doFinal(pending)
                if (finalBytes.isNotEmpty()) output.write(finalBytes)
            }
            nonce.wipe()
        }
    }

    fun sha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(STREAM_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest()
    }

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher
    }

    private fun encryptBytes(key: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray {
        val nonce = randomBytes(12)
        val cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce)
        cipher.updateAAD(aad)
        val body = cipher.doFinal(plain)
        return nonce + body
    }

    private fun decryptBytes(key: ByteArray, message: ByteArray, aad: ByteArray): ByteArray {
        if (message.size <= 12 + GCM_TAG) {
            throw MigrationException(MigrationException.Kind.Protocol, "import", "数据密钥包无法识别")
        }
        val nonce = message.copyOfRange(0, 12)
        val body = message.copyOfRange(12, message.size)
        val cipher = cipher(Cipher.DECRYPT_MODE, key, nonce)
        cipher.updateAAD(aad)
        return try {
            cipher.doFinal(body)
        } catch (_: Exception) {
            throw MigrationException(MigrationException.Kind.Protocol, "import", "数据密钥无法解开")
        }
    }

    private fun canonical(first: ByteArray, second: ByteArray): ByteArray {
        return if (unsignedCompare(first, second) <= 0) first + second else second + first
    }

    private fun unsignedCompare(left: ByteArray, right: ByteArray): Int {
        val size = minOf(left.size, right.size)
        for (index in 0 until size) {
            val delta = (left[index].toInt() and 0xFF) - (right[index].toInt() and 0xFF)
            if (delta != 0) return delta
        }
        return left.size - right.size
    }

    private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val generator = HKDFBytesGenerator(SHA256Digest())
        generator.init(HKDFParameters(ikm, salt, info))
        val output = ByteArray(length)
        generator.generateBytes(output, 0, length)
        return output
    }

    private fun sha256(bytes: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(bytes)
    }

    private fun readFully(input: FileInputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read < 0) {
                throw MigrationException(MigrationException.Kind.Protocol, "import", "整库信封不完整")
            }
            offset += read
        }
    }

    private const val STREAM_BUFFER: Int = 64 * 1024
    private const val GCM_TAG: Int = 16
    private const val ENVELOPE_HEADER: Int = 17
}

internal class EphemeralExchange private constructor(
    private val privateKey: X25519PrivateKeyParameters,
    val publicKey: ByteArray,
) {
    fun sharedSecret(peerPublic: ByteArray): ByteArray {
        if (peerPublic.size != 32) {
            throw MigrationException(MigrationException.Kind.Protocol, "confirm", "对方公钥长度不对")
        }
        val agreement = X25519Agreement()
        agreement.init(privateKey)
        val secret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(peerPublic, 0), secret, 0)
        return secret
    }

    companion object {
        fun generate(): EphemeralExchange {
            val generator = X25519KeyPairGenerator()
            generator.init(X25519KeyGenerationParameters(SecureRandom()))
            val pair = generator.generateKeyPair()
            val privateKey = pair.private as X25519PrivateKeyParameters
            val publicKey = (pair.public as X25519PublicKeyParameters).encoded.copyOf()
            return EphemeralExchange(privateKey, publicKey)
        }
    }
}

internal class SessionMaterial(
    val key: ByteArray,
    val digits: String,
)

internal fun ByteArray.wipe() {
    fill(0)
}
