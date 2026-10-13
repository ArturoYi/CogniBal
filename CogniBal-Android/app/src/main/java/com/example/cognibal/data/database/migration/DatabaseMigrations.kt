package com.example.cognibal.data.database.migration

import androidx.room.migration.Migration

/**
 * 手写的 Schema 迁移。和用户在备份页做的导入导出不是一回事：
 * 这里只在应用升级、Room 发现旧库版本低于 [com.example.cognibal.data.database.AppDatabase.VERSION] 时自动执行。
 *
 * 当前版本是 1，第一次安装会按 Entity 直接建库，没有更旧的库需要搬。
 * 所以 [ALL] 是空的，也不使用 autoMigrations。
 *
 * 下次改表时：
 * 1. 把数据库版本加 1。
 * 2. 在这里增加 `MIGRATION_1_2` 这种对象，SQL 只做字段或表的差异。
 * 3. 放进 [ALL]。已经安装过版本 1 的设备会走这段 SQL，新安装仍然直接建最新结构。
 */
object DatabaseMigrations {
    val ALL: Array<Migration> = emptyArray()
}
