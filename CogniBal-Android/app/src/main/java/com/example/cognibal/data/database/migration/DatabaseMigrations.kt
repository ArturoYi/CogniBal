package com.example.cognibal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 手写的 Schema 迁移。和用户在备份页做的导入导出不是一回事：
 * 这里只在应用升级、Room 发现旧库版本低于 [com.example.cognibal.data.database.AppDatabase.VERSION] 时自动执行。
 *
 * 新安装会按 Entity 直接建最新结构，不会走这里。
 * 已经装过旧版的设备按版本差执行 [ALL] 里的脚本。
 */
object DatabaseMigrations {
    /**
     * 账户增加 is_default。旧库没有“当前展示”这一列，先标成 0，
     * 再把最早的一个启用账户标成当前展示；一个启用的都没有时，用 id 最小的那个。
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `account` ADD COLUMN `is_default` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """
                UPDATE `account`
                SET `is_default` = 1
                WHERE `id` = (
                    SELECT `id` FROM `account`
                    ORDER BY `isEnable` DESC, `id` ASC
                    LIMIT 1
                )
                """.trimIndent(),
            )
        }
    }

    /**
     * 账单增加 `(type, categoryId, billTime)`。
     * 分类预算按这个范围求和，避免每笔支出都扫完整张账单表。
     */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_bill_type_categoryId_billTime`
                ON `bill` (`type`, `categoryId`, `billTime`)
                """.trimIndent(),
            )
        }
    }

    /** 月预算可以不从 1 日开始。空值表示旧数据，按当时的周期起点。 */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `budget` ADD COLUMN `cycleDay` INTEGER")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
