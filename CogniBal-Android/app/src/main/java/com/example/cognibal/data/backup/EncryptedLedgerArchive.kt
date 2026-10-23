package com.example.cognibal.data.backup

import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod

/**
 * 把账本 JSON 收成 AES-256 加密 zip。
 *
 * 备份和导入共用 [ARCHIVE_PASSWORD]，不向用户询问。压缩包里只有 [BackupFiles.ENTRY_NAME] 这一条，
 * 而且必须是 AES。没有加密的包直接拒绝。
 * 蓝牙迁移用的数据密钥不写进这个口令包，也不由这里生成。
 */
internal object EncryptedLedgerArchive {
    private const val ARCHIVE_PASSWORD: String = "CogniBalLedger"

    fun write(jsonFile: File, destination: File) {
        if (destination.exists() && !destination.delete()) {
            throw BackupException.Io("无法写入备份")
        }
        val parameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            isEncryptFiles = true
            fileNameInZip = BackupFiles.ENTRY_NAME
        }
        val owned = ARCHIVE_PASSWORD.toCharArray()
        try {
            val zip = ZipFile(destination, owned)
            try {
                zip.addFile(jsonFile, parameters)
            } catch (exception: ZipException) {
                destination.delete()
                throw BackupException.Io("无法加密备份", exception)
            } finally {
                zip.close()
            }
        } finally {
            owned.wipe()
        }
        if (!destination.isFile || destination.length() <= 0L) {
            destination.delete()
            throw BackupException.Io("无法加密备份")
        }
    }

    fun read(source: File): ByteArray {
        if (!BackupFiles.looksLikeZip(source)) throw BackupException.Format("备份不是压缩包")
        val owned = ARCHIVE_PASSWORD.toCharArray()
        try {
            val zip = ZipFile(source, owned)
            try {
                if (!zip.isValidZipFile) throw BackupException.Format("备份不是压缩包")
                val header = zip.getFileHeader(BackupFiles.ENTRY_NAME)
                    ?: throw BackupException.Format("备份里没有账本")
                if (!header.isEncrypted || header.encryptionMethod != EncryptionMethod.AES) {
                    throw BackupException.Format("备份没有按约定加密")
                }
                return zip.getInputStream(header).use { input ->
                    BackupFiles.readAtMost(input, BackupFiles.MAX_BYTES)
                }
            } catch (exception: BackupException) {
                throw exception
            } catch (_: ZipException) {
                throw BackupException.Format("备份无法读取")
            } catch (exception: java.io.IOException) {
                throw BackupException.Io("无法读取备份", exception)
            } finally {
                zip.close()
            }
        } finally {
            owned.wipe()
        }
    }
}
