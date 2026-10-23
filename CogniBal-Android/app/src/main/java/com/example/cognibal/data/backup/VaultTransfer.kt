package com.example.cognibal.data.backup

import android.content.Context
import com.example.cognibal.data.backup.bluetooth.MigrationCrypto
import com.example.cognibal.data.backup.bluetooth.MigrationProtocol
import com.example.cognibal.data.backup.bluetooth.wipe
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.repository.onIo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 协议 2 的整库：数据库、WAL、SHM 和附件打成一个 zip，再用一次性数据密钥封装。
 *
 * 数据密钥只留在内存里，调用方用会话密钥包起来传输。这里不写口令备份，也不把密钥落到磁盘。
 * 发送方只读现有库文件，不替换自己的账本。
 */
@Singleton
class VaultTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val billDao: BillDao,
) {
    /** 在用条目指账单。只有预置账户和分类、没有账单时，不算可以迁移的库。 */
    suspend fun hasEntriesInUse(): Boolean = onIo { billDao.count() > 0 }

    suspend fun writeEncrypted(destination: File): SealedVault = onIo {
        val plain = File(workDirectory(), "plain.zip")
        val key = MigrationCrypto.dataKey()
        try {
            checkpoint()
            val databaseFile = File(databaseDirectory(context), AppDatabase.NAME)
            val schema = SqliteRestore.userVersion(databaseFile)
            writePlainZip(plain, schema, databaseFile)
            if (destination.exists() && !destination.delete()) {
                throw BackupException.Io("无法准备整库")
            }
            val hash = MigrationCrypto.seal(key, plain, destination)
            SealedVault(destination, key, hash, schema)
        } catch (exception: Exception) {
            key.wipe()
            destination.delete()
            throw exception
        } finally {
            plain.delete()
        }
    }

    /**
     * 校验通过后放进 pending。正在使用的库要等下次冷启动才换上。
     * 失败时 pending 会清掉，原来的账本不动。
     */
    fun discardPending() {
        SqliteRestore.discardPrepared(context)
    }

    suspend fun importEncrypted(source: File, dataKey: ByteArray, offeredSchema: Int) = onIo {
        val plain = File(workDirectory(), "incoming.zip")
        val extracted = File(workDirectory(), "incoming")
        try {
            extracted.deleteRecursively()
            extracted.mkdirs()
            MigrationCrypto.open(dataKey, source, plain)
            val unpacked = unpack(plain, extracted)
            if (unpacked.manifest.protocol != MigrationProtocol.PROTOCOL_VERSION) {
                throw BackupException.Format("整库协议版本不一致")
            }
            val fileSchema = SqliteRestore.userVersion(unpacked.database)
            if (unpacked.manifest.schema != fileSchema || offeredSchema != fileSchema) {
                throw BackupException.Format("整库版本对不上")
            }
            if (fileSchema > AppDatabase.VERSION) throw BackupException.NewerDatabase(fileSchema)
            SqliteRestore.stagePrepared(
                context = context,
                database = unpacked.database,
                wal = unpacked.wal,
                shm = unpacked.shm,
                attachments = unpacked.attachments,
            )
        } finally {
            plain.delete()
            extracted.deleteRecursively()
        }
    }

    private fun checkpoint() {
        val db = database.openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            cursor.moveToFirst()
        }
    }

    private fun writePlainZip(destination: File, schema: Int, databaseFile: File) {
        val manifest = json.encodeToString(
            VaultManifest.serializer(),
            VaultManifest(protocol = MigrationProtocol.PROTOCOL_VERSION, schema = schema),
        )
        ZipOutputStream(FileOutputStream(destination)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
            addFile(zip, databaseFile, AppDatabase.NAME)
            addIfExists(zip, File(databaseFile.parentFile, WAL), WAL)
            addIfExists(zip, File(databaseFile.parentFile, SHM), SHM)
            addAttachments(zip, File(context.filesDir, ATTACHMENTS))
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, name: String) {
        if (!file.isFile) throw BackupException.Io("找不到数据库文件")
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { input -> input.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addIfExists(zip: ZipOutputStream, file: File, name: String) {
        if (file.isFile) addFile(zip, file, name)
    }

    private fun addAttachments(zip: ZipOutputStream, root: File) {
        if (!root.isDirectory) return
        root.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            if (relative.isBlank() || relative.contains("..")) return@forEach
            zip.putNextEntry(ZipEntry("$ATTACHMENTS/$relative"))
            file.inputStream().use { input -> input.copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun unpack(zipFile: File, directory: File): Unpacked {
        var manifest: VaultManifest? = null
        var database: File? = null
        var wal: File? = null
        var shm: File? = null
        var sawAttachment = false
        var total = 0L
        ZipInputStream(zipFile.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                when (name) {
                    MANIFEST -> {
                        val text = readEntry(zip, total)
                        total += text.size
                        manifest = try {
                            json.decodeFromString(VaultManifest.serializer(), text.decodeToString())
                        } catch (_: Exception) {
                            throw BackupException.Format("整库清单无法读取")
                        }
                    }
                    AppDatabase.NAME -> {
                        val written = extract(zip, File(directory, AppDatabase.NAME), total)
                        database = written
                        total += written.length()
                    }
                    WAL -> {
                        val written = extract(zip, File(directory, WAL), total)
                        wal = written
                        total += written.length()
                    }
                    SHM -> {
                        val written = extract(zip, File(directory, SHM), total)
                        shm = written
                        total += written.length()
                    }
                    else -> {
                        val relative = attachmentRelative(name)
                        val target = File(File(directory, ATTACHMENTS), relative)
                        if (!target.canonicalFile.path.startsWith(File(directory, ATTACHMENTS).canonicalFile.path + File.separator)) {
                            throw BackupException.Format("附件路径无法识别")
                        }
                        target.parentFile?.mkdirs()
                        val written = extract(zip, target, total)
                        total += written.length()
                        sawAttachment = true
                    }
                }
                zip.closeEntry()
            }
        }
        val readyDatabase = database ?: throw BackupException.Format("整库里没有数据库")
        val readyManifest = manifest ?: throw BackupException.Format("整库里没有清单")
        return Unpacked(
            manifest = readyManifest,
            database = readyDatabase,
            wal = wal,
            shm = shm,
            attachments = if (sawAttachment) File(directory, ATTACHMENTS) else null,
        )
    }

    private fun attachmentRelative(name: String): String {
        val prefix = "$ATTACHMENTS/"
        if (!name.startsWith(prefix)) throw BackupException.Format("压缩包里有不能识别的文件")
        val relative = name.removePrefix(prefix)
        if (relative.isBlank() || relative.startsWith("/") || relative.contains("..")) {
            throw BackupException.Format("附件路径无法识别")
        }
        return relative
    }

    private fun readEntry(zip: ZipInputStream, already: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER)
        var total = already
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > EXTRACT_LIMIT) throw BackupException.Format("整库文件过大")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun extract(zip: ZipInputStream, target: File, already: Long): File {
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            var total = already
            while (true) {
                val read = zip.read(buffer)
                if (read < 0) break
                total += read
                if (total > EXTRACT_LIMIT) throw BackupException.Format("整库文件过大")
                output.write(buffer, 0, read)
            }
        }
        return target
    }

    private fun workDirectory(): File {
        val directory = File(context.cacheDir, "migration")
        if (!directory.exists() && !directory.mkdirs()) throw BackupException.Io("无法准备整库")
        return directory
    }

    private data class Unpacked(
        val manifest: VaultManifest,
        val database: File,
        val wal: File?,
        val shm: File?,
        val attachments: File?,
    )

    private companion object {
        const val MANIFEST: String = "manifest.json"
        const val WAL: String = "cognibal.db-wal"
        const val SHM: String = "cognibal.db-shm"
        const val ATTACHMENTS: String = "attachments"
        const val DEFAULT_BUFFER: Int = 8 * 1024
        const val EXTRACT_LIMIT: Long = 256L * 1024L * 1024L
        val json: Json = Json { ignoreUnknownKeys = true }
    }
}

class SealedVault(
    val file: File,
    val key: ByteArray,
    val sha256: ByteArray,
    val schema: Int,
) : Closeable {
    val size: Long
        get() = file.length()

    override fun close() {
        key.wipe()
        file.delete()
    }
}

@Serializable
internal data class VaultManifest(
    val protocol: Int,
    val schema: Int,
)
