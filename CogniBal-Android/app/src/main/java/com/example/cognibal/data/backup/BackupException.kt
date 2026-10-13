package com.example.cognibal.data.backup

import com.example.cognibal.data.repository.LedgerReject

/**
 * 用户手动导入导出失败。这不是 Room 的 Migration：升级表结构仍走 migration 包。
 */
sealed class BackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Io(message: String, cause: Throwable? = null) : BackupException(message, cause)

    class Format(message: String) : BackupException(message)

    /** 整库文件的 schema 版本比当前应用新，这版应用打不开。 */
    class NewerDatabase(val fileVersion: Int) : BackupException(
        "备份来自更新的数据库版本 $fileVersion",
    )

    /** JSON 里的业务数据违反了账户、分类或账单规则。 */
    class Rejected(val reason: LedgerReject) : BackupException(reason.name)
}
