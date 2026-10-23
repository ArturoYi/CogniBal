package com.example.cognibal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.cognibal.data.backup.SqliteRestore
import com.example.cognibal.data.database.dao.AccountDao
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.dao.BudgetDao
import com.example.cognibal.data.database.dao.CategoryDao
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.entity.Budget
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.migration.DatabaseMigrations
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.database.model.CategoryType

/**
 * 账单库，版本 [VERSION]。
 *
 * 只有账户、分类、账单、预算四张表。标签和提醒不在这一版里。
 * 打开前会先看有没有待换上的整库备份；那是用户导入，不是版本迁移。
 * 查询线程用 Room 自己的线程池。仓库的挂起函数在进入事务前切到 `Dispatchers.IO`，
 * 不再把 Room 的事务线程也换成 IO，避免事务里的查询和 IO 线程池互相等待。
 */
@Database(
    entities = [
        Account::class,
        Category::class,
        Bill::class,
        Budget::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(LedgerConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun billDao(): BillDao

    abstract fun budgetDao(): BudgetDao

    companion object {
        /** 第三版给账单加了分类预算用的索引。第四版给预算加了每月起始日。改表时加 1，并在 migration 包里补上对应脚本。 */
        const val VERSION: Int = 4

        const val NAME: String = "cognibal.db"

        fun open(context: Context): AppDatabase {
            checkLedgerCodes()
            val appContext = context.applicationContext
            SqliteRestore.applyPending(appContext)
            return Room.databaseBuilder(appContext, AppDatabase::class.java, NAME)
                .addMigrations(*DatabaseMigrations.ALL)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .openHelperFactory(ForeignKeyOpenHelperFactory())
                .build()
        }

        /**
         * 聚合 SQL 把类型写成了字面量。枚举的 code 如果被人改了，这里立刻失败，
         * 避免日历把支出算进收入。
         */
        private fun checkLedgerCodes() {
            check(CategoryType.Expense.code == 0 && CategoryType.Income.code == 1)
            check(BillType.Expense.code == 0 && BillType.Income.code == 1 && BillType.Transfer.code == 2)
            check(BudgetType.Global.code == 0 && BudgetType.Category.code == 1)
        }
    }
}

/**
 * Room 2.8 的 [RoomDatabase.Callback] 没有 onConfigure。外键必须在建表之前、
 * 每条连接刚打开时打开，所以包一层 OpenHelper，在 onConfigure 里打开外键。
 * 账户或分类仍被账单引用时，删除会被数据库拒绝。
 */
private class ForeignKeyOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    private val delegate = FrameworkSQLiteOpenHelperFactory()

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration,
    ): SupportSQLiteOpenHelper {
        val callback = object : SupportSQLiteOpenHelper.Callback(configuration.callback.version) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
                configuration.callback.onConfigure(db)
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                configuration.callback.onCreate(db)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                configuration.callback.onUpgrade(db, oldVersion, newVersion)
            }

            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                configuration.callback.onDowngrade(db, oldVersion, newVersion)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                configuration.callback.onOpen(db)
            }

            override fun onCorruption(db: SupportSQLiteDatabase) {
                configuration.callback.onCorruption(db)
            }
        }
        val wrapped = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(callback)
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(configuration.allowDataLossOnRecovery)
            .build()
        return delegate.create(wrapped)
    }
}
