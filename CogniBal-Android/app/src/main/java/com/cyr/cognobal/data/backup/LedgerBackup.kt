package com.cyr.cognobal.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.cyr.cognobal.data.repository.onIo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * 设置页用的账本备份。
 *
 * 导出的是加密 zip，里面是四张业务表的 JSON。余额以文件里的账户余额为准。
 * 主题、语言、货币不在这个包里。本机只保留一份正式备份，新备份会换掉旧文件。
 * 导入可以从这份本机备份恢复，也可以从系统文件选择器所选的压缩包恢复。
 * 导入在一个事务里整份替换。压缩包密码写在代码里，导入导出共用。内容不合格时，原来的账本不动。
 */
@Singleton
class LedgerBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jsonBackup: JsonLedgerBackup,
) {
    private val gate = kotlinx.coroutines.sync.Mutex()

    init {
        // 上次备份写到一半被杀掉时，缓存里可能还留着明文 JSON。
        plaintextFile().delete()
        BackupFiles.keepOnly(archiveFile())
    }

    fun archiveFile(): File = File(backupDirectory(), BackupFiles.TRANSFER_NAME)

    suspend fun savedAtMillis(): Long? = onIo {
        val file = archiveFile()
        if (!file.isFile || file.length() <= 0L) {
            null
        } else {
            file.lastModified().takeIf { it > 0L }
        }
    }

    suspend fun exists(): Boolean = savedAtMillis() != null

    /** 蓝牙接收先写到这个临时文件，校验通过后再换上正式备份。 */
    fun prepareIncoming(): File {
        val file = File(backupDirectory(), "${BackupFiles.TRANSFER_NAME}.incoming")
        if (file.exists() && !file.delete()) throw BackupException.Io("无法准备接收备份")
        return file
    }

    suspend fun create() = onIo {
        gate.lock()
        val plaintext = plaintextFile()
        val staged = stagedFile()
        var committed = false
        try {
            try {
                plaintext.parentFile?.mkdirs()
                plaintext.delete()
                staged.delete()
                FileOutputStream(plaintext).use { output -> jsonBackup.write(output) }
                EncryptedLedgerArchive.write(plaintext, staged)
                BackupFiles.replace(archiveFile(), staged)
                committed = true
            } catch (exception: BackupException) {
                throw exception
            } catch (exception: java.io.IOException) {
                throw BackupException.Io("无法写入备份", exception)
            }
        } finally {
            plaintext.delete()
            if (!committed && staged.exists()) staged.delete()
            gate.unlock()
        }
    }

    suspend fun importLocal() = onIo {
        gate.lock()
        try {
            val source = archiveFile()
            if (!source.isFile) throw BackupException.Io("没有可以导入的备份")
            restoreFromFile(source)
        } finally {
            gate.unlock()
        }
    }

    /** 从系统文件选择器拿到的压缩包恢复。先拷到缓存再解密，避免直接对着内容提供者读 zip。 */
    suspend fun importUri(uri: Uri) = onIo {
        gate.lock()
        val work = importWorkFile()
        try {
            work.parentFile?.mkdirs()
            work.delete()
            copyFromUri(uri, work)
            restoreFromFile(work)
        } catch (exception: BackupException) {
            throw exception
        } catch (exception: SecurityException) {
            throw BackupException.Io("无法读取所选位置", exception)
        } catch (exception: java.io.IOException) {
            throw BackupException.Io("无法读取所选位置", exception)
        } finally {
            work.delete()
            gate.unlock()
        }
    }

    suspend fun installReceived(staged: File) = withContext(NonCancellable) {
        onIo {
            gate.lock()
            try {
                if (!staged.isFile || staged.length() <= 0L || staged.length() > BackupFiles.MAX_BYTES) {
                    throw BackupException.Format("备份文件无法读取")
                }
                if (!BackupFiles.looksLikeZip(staged)) throw BackupException.Format("备份不是压缩包")
                BackupFiles.replace(archiveFile(), staged)
            } finally {
                gate.unlock()
            }
        }
    }

    /** 复制一份到缓存再交给系统分享。私有目录里的正式备份不放进 FileProvider。 */
    suspend fun shareUri(): Uri = onIo {
        gate.lock()
        try {
            val source = archiveFile()
            if (!source.isFile || source.length() <= 0L) {
                throw BackupException.Io("没有可以分享的备份")
            }
            val share = shareFile(source.lastModified())
            try {
                source.copyTo(share, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", share)
            } catch (exception: BackupException) {
                throw exception
            } catch (exception: java.io.IOException) {
                throw BackupException.Io("无法分享备份", exception)
            } catch (exception: IllegalArgumentException) {
                throw BackupException.Io("无法分享备份", exception)
            }
        } finally {
            gate.unlock()
        }
    }

    private suspend fun restoreFromFile(source: File) {
        if (!source.isFile || source.length() <= 0L || source.length() > BackupFiles.MAX_BYTES) {
            throw BackupException.Format("备份文件无法读取")
        }
        val json = EncryptedLedgerArchive.read(source)
        try {
            jsonBackup.replaceAll(ByteArrayInputStream(json))
        } finally {
            json.fill(0)
        }
    }

    private fun copyFromUri(uri: Uri, destination: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw BackupException.Io("无法读取所选位置")
        input.use { stream ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(COPY_BUFFER)
                var total = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > BackupFiles.MAX_BYTES) {
                        destination.delete()
                        throw BackupException.Format("备份文件过大")
                    }
                    output.write(buffer, 0, read)
                }
                if (total <= 0L) {
                    destination.delete()
                    throw BackupException.Format("备份文件无法读取")
                }
            }
        }
    }

    private fun backupDirectory(): File = File(context.filesDir, "backups").apply { mkdirs() }

    private fun plaintextFile(): File = File(File(context.cacheDir, "backup-work"), "ledger.json")

    private fun importWorkFile(): File = File(File(context.cacheDir, "backup-work"), "import.zip")

    private fun stagedFile(): File = File(backupDirectory(), "${BackupFiles.TRANSFER_NAME}.staged")

    private fun shareFile(modifiedAt: Long): File {
        val directory = File(context.cacheDir, "backup-share")
        if (!directory.exists() && !directory.mkdirs()) throw BackupException.Io("无法准备分享")
        directory.listFiles()?.forEach { child ->
            if (child.isFile) child.delete()
        }
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(modifiedAt))
        return File(directory, "cognibal-$stamp.zip")
    }

    private companion object {
        const val COPY_BUFFER: Int = 8 * 1024
    }
}
