package com.example.cognibal.data.backup

import android.content.Context
import android.net.Uri
import com.example.cognibal.data.repository.onIo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过系统文件选择器（SAF）读写备份。界面拿到 Uri 后调用这里，不自己复制数据库文件。
 *
 * 整库导入只是把文件准备好，要杀掉进程再打开才会换成新库。
 * JSON 导入会立刻替换当前账本，并让正在监听的 Flow 刷新。
 */
@Singleton
class DocumentBackupGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sqliteBackup: SqliteDatabaseBackup,
    private val jsonBackup: JsonLedgerBackup,
) {
    suspend fun exportSqlite(uri: Uri) = write(uri) { sqliteBackup.writeZip(it) }

    suspend fun importSqlite(uri: Uri) = read(uri) { sqliteBackup.stageFromZip(it) }

    suspend fun exportJson(uri: Uri) = write(uri) { jsonBackup.write(it) }

    suspend fun importJson(uri: Uri) = read(uri) { jsonBackup.replaceAll(it) }

    /** 上次冷启动没能换上的整库备份说明。读过一次就清掉。 */
    fun takeSqliteRestoreError(): String? = SqliteRestore.takeError(context)

    private suspend fun write(uri: Uri, body: suspend (java.io.OutputStream) -> Unit) = onIo {
        try {
            context.contentResolver.openOutputStream(uri)?.use { output -> body(output) }
                ?: throw BackupException.Io("无法写入所选位置")
        } catch (exception: IOException) {
            throw BackupException.Io("写入备份失败", exception)
        }
    }

    private suspend fun read(uri: Uri, body: suspend (java.io.InputStream) -> Unit) = onIo {
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> body(input) }
                ?: throw BackupException.Io("无法读取所选位置")
        } catch (exception: IOException) {
            throw BackupException.Io("读取备份失败", exception)
        }
    }
}
