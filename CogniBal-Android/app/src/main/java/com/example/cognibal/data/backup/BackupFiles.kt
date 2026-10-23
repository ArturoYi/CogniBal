package com.example.cognibal.data.backup

import java.io.File
import java.io.InputStream

/**
 * 备份文件的大小上限和替换方式。
 *
 * 加密包先写到旁边的临时文件，再换上正式文件。换的过程中途失败时，原来的包还在。
 */
internal object BackupFiles {
    const val ENTRY_NAME: String = "ledger.json"
    const val TRANSFER_NAME: String = "cognibal-ledger.zip"
    const val MAX_BYTES: Long = 32L * 1024L * 1024L

    fun looksLikeZip(file: File): Boolean {
        if (file.length() < 4L) return false
        val header = ByteArray(4)
        val read = file.inputStream().use { input -> input.read(header) }
        if (read != header.size) return false
        return header[0] == 0x50.toByte() &&
            header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()
    }

    fun readAtMost(input: InputStream, maxBytes: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw BackupException.Format("备份文件过大")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    fun replace(target: File, staged: File) {
        val parent = target.parentFile ?: throw BackupException.Io("无法保存备份")
        if (!parent.exists() && !parent.mkdirs()) throw BackupException.Io("无法保存备份")
        if (!staged.isFile) throw BackupException.Io("没有生成备份")
        val previous = File(parent, "${target.name}.previous")
        if (previous.exists() && !previous.delete()) throw BackupException.Io("无法保存备份")
        val hadTarget = target.exists()
        if (hadTarget && !target.renameTo(previous)) {
            throw BackupException.Io("无法替换原来的备份")
        }
        if (!staged.renameTo(target)) {
            if (hadTarget) previous.renameTo(target)
            throw BackupException.Io("无法保存备份")
        }
        if (previous.exists()) previous.delete()
    }

    private const val DEFAULT_BUFFER: Int = 8 * 1024
}

internal fun CharArray.wipe() {
    fill('\u0000')
}
