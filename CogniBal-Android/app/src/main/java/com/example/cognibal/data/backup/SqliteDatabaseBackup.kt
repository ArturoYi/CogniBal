package com.example.cognibal.data.backup

import android.content.Context
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.repository.onIo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SQLite 整库备份：主库加上 WAL、SHM。
 *
 * 这是文件级复制，不经过 Entity。恢复时不能在已经打开的连接上把文件换掉，
 * 所以先把文件放到 pending，下次进程启动、Room 打开之前再换上。
 * 换上之后如果文件版本比当前应用旧，Room 会走 [com.example.cognibal.data.database.migration.DatabaseMigrations]；
 * 比当前应用新则留下错误说明，继续用原来的库。
 */
@Singleton
class SqliteDatabaseBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {
    suspend fun writeZip(output: OutputStream) = onIo {
        checkpoint()
        val directory = databaseDirectory(context)
        ZipOutputStream(output).use { zip ->
            add(zip, File(directory, AppDatabase.NAME), AppDatabase.NAME)
            addIfExists(zip, File(directory, WAL), WAL)
            addIfExists(zip, File(directory, SHM), SHM)
        }
    }

    /** 校验 zip 后放到 pending。返回前数据库仍是旧内容，进程下次冷启动才换上。 */
    suspend fun stageFromZip(input: InputStream) = onIo {
        SqliteRestore.stageFromZip(context, input)
    }

    /**
     * `wal_checkpoint(FULL)` 尽量把 WAL 收进主库。收完到复制之间仍可能有新写入，
     * 所以 zip 里仍然带上当时的 WAL 和 SHM，而不是只拷主库。
     */
    private fun checkpoint() {
        val db = database.openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun add(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.isFile) throw BackupException.Io("找不到数据库文件")
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input -> input.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addIfExists(zip: ZipOutputStream, file: File, entryName: String) {
        if (file.isFile) add(zip, file, entryName)
    }

    private companion object {
        const val WAL: String = "cognibal.db-wal"
        const val SHM: String = "cognibal.db-shm"
    }
}

/**
 * 在 Room 打开数据库之前调用 [applyPending]。不要从界面直接换正在使用的库文件。
 */
internal object SqliteRestore {
    private const val WAL: String = "cognibal.db-wal"
    private const val SHM: String = "cognibal.db-shm"
    private const val PENDING_DB: String = "cognibal.db.pending"
    private const val PENDING_WAL: String = "cognibal.db-wal.pending"
    private const val PENDING_SHM: String = "cognibal.db-shm.pending"
    private const val MARKER: String = "cognibal.restore.ready"
    private const val ERROR_FILE: String = "cognibal.restore.error"
    private const val MAX_BYTES: Long = 256L * 1024L * 1024L

    fun applyPending(context: Context) {
        val directory = databaseDirectory(context)
        val marker = File(directory, MARKER)
        val pendingDb = File(directory, PENDING_DB)
        if (!marker.isFile || !pendingDb.isFile) {
            if (marker.exists() && !pendingDb.exists()) marker.delete()
            return
        }
        try {
            val version = readSqliteUserVersion(pendingDb)
            if (version <= 0) {
                clearPending(directory)
                writeError(directory, "备份不是本应用的账单库")
                return
            }
            if (version > AppDatabase.VERSION) {
                clearPending(directory)
                writeError(directory, "备份来自更新的数据库版本 $version")
                return
            }
            pendingDb.copyTo(File(directory, AppDatabase.NAME), overwrite = true)
            copyOrDelete(File(directory, PENDING_WAL), File(directory, WAL))
            copyOrDelete(File(directory, PENDING_SHM), File(directory, SHM))
            clearPending(directory)
        } catch (exception: BackupException) {
            clearPending(directory)
            writeError(directory, exception.message ?: "整库恢复失败")
        }
    }

    fun takeError(context: Context): String? {
        val file = File(databaseDirectory(context), ERROR_FILE)
        if (!file.isFile) return null
        val message = file.readText()
        file.delete()
        return message.ifBlank { null }
    }

    fun stageFromZip(context: Context, input: InputStream) {
        val directory = databaseDirectory(context)
        directory.mkdirs()
        clearPending(directory)
        val incomingDb = File(directory, "cognibal.db.incoming")
        val incomingWal = File(directory, "cognibal.db-wal.incoming")
        val incomingShm = File(directory, "cognibal.db-shm.incoming")
        var sawDatabase = false
        try {
            ZipInputStream(input).use { zip ->
                var total = 0L
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = when (entry.name) {
                        AppDatabase.NAME -> {
                            sawDatabase = true
                            incomingDb
                        }
                        WAL -> incomingWal
                        SHM -> incomingShm
                        else -> throw BackupException.Format("压缩包里有不能识别的文件")
                    }
                    if (entry.isDirectory) throw BackupException.Format("压缩包格式不对")
                    total = extract(zip, target, total)
                    zip.closeEntry()
                }
            }
            if (!sawDatabase) throw BackupException.Format("压缩包里没有数据库文件")
            val version = readSqliteUserVersion(incomingDb)
            if (version <= 0) throw BackupException.Format("备份不是本应用的账单库")
            if (version > AppDatabase.VERSION) throw BackupException.NewerDatabase(version)
            incomingDb.copyTo(File(directory, PENDING_DB), overwrite = true)
            if (incomingWal.isFile) incomingWal.copyTo(File(directory, PENDING_WAL), overwrite = true)
            if (incomingShm.isFile) incomingShm.copyTo(File(directory, PENDING_SHM), overwrite = true)
            File(directory, MARKER).writeText("ready")
        } catch (exception: BackupException) {
            clearPending(directory)
            throw exception
        } finally {
            incomingDb.delete()
            incomingWal.delete()
            incomingShm.delete()
        }
    }

    private fun extract(zip: InputStream, target: File, already: Long): Long {
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            var total = already
            while (true) {
                val read = zip.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_BYTES) throw BackupException.Format("备份文件过大")
                output.write(buffer, 0, read)
            }
            return total
        }
    }

    private fun copyOrDelete(pending: File, live: File) {
        if (pending.isFile) {
            pending.copyTo(live, overwrite = true)
        } else if (live.exists()) {
            live.delete()
        }
    }

    private fun clearPending(directory: File) {
        listOf(MARKER, PENDING_DB, PENDING_WAL, PENDING_SHM).forEach { name ->
            File(directory, name).delete()
        }
    }

    private fun writeError(directory: File, message: String) {
        File(directory, ERROR_FILE).writeText(message)
    }

    private fun readSqliteUserVersion(file: File): Int {
        if (file.length() < HEADER_BYTES) throw BackupException.Format("文件过短，不是 SQLite 数据库")
        val header = ByteArray(HEADER_BYTES)
        file.inputStream().use { input ->
            var offset = 0
            while (offset < header.size) {
                val read = input.read(header, offset, header.size - offset)
                if (read < 0) throw BackupException.Format("文件过短，不是 SQLite 数据库")
                offset += read
            }
        }
        val magic = "SQLite format 3\u0000".encodeToByteArray()
        if (!header.copyOf(magic.size).contentEquals(magic)) {
            throw BackupException.Format("文件不是 SQLite 数据库")
        }
        return ((header[60].toInt() and 0xFF) shl 24) or
            ((header[61].toInt() and 0xFF) shl 16) or
            ((header[62].toInt() and 0xFF) shl 8) or
            (header[63].toInt() and 0xFF)
    }

    private const val HEADER_BYTES: Int = 100
    private const val DEFAULT_BUFFER: Int = 8 * 1024
}

internal fun databaseDirectory(context: Context): File {
    return context.applicationContext.getDatabasePath(AppDatabase.NAME).parentFile
        ?: throw BackupException.Io("无法定位数据库目录")
}
