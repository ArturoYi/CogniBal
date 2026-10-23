package com.example.cognibal.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.AccountDao
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.dao.BudgetDao
import com.example.cognibal.data.database.dao.CategoryDao
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.entity.Budget
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.database.model.CategoryType
import com.example.cognibal.data.repository.BudgetMath
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.data.repository.onIo
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 把四张业务表导出成 JSON，或用一份 JSON 整份替换当前账本。
 *
 * 文件格式版本是 [FORMAT]，和 [AppDatabase.VERSION] 无关。
 * 以后给 JSON 加必填字段时升 [FORMAT]；给 SQLite 加列时升数据库版本并写 Migration。
 * [AccountRecord.isDefault] 有默认值，旧备份没有这一项时按“不是当前展示”读入，
 * 导入前再收成恰好一个当前展示账户。
 *
 * 导入不会重新套用账单对余额的增减。文件里的 [AccountRecord.balance] 就是当时的缓存余额，
 * 期初余额没有单独的列，不能从账单反推。导入若再走 [com.example.cognibal.data.repository.BillRepository]，
 * 余额会被加两次。
 */
@Singleton
class JsonLedgerBackup @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
) {
    suspend fun write(output: OutputStream) = onIo {
        val snapshot = database.withTransaction {
            LedgerSnapshot(
                format = FORMAT,
                exportedAt = LedgerTime.encode(LedgerTime.now()),
                accounts = accountDao.getAll().map { it.toRecord() },
                categories = categoryDao.getAll().map { it.toRecord() },
                bills = billDao.getAll().map { it.toRecord() },
                budgets = budgetDao.getAll().map { it.toRecord() },
            )
        }
        output.bufferedWriter().use { writer ->
            writer.write(json.encodeToString(LedgerSnapshot.serializer(), snapshot))
        }
    }

    /**
     * 用文件里的四张表替换当前数据。调用方应先让用户确认，这里不会再弹界面。
     * 校验不过，或写入中途失败，原来的账本保持不变。
     */
    suspend fun replaceAll(input: InputStream) = onIo {
        val text = input.bufferedReader().use { it.readText() }
        val snapshot = try {
            json.decodeFromString(LedgerSnapshot.serializer(), text)
        } catch (_: kotlinx.serialization.SerializationException) {
            throw BackupException.Format("JSON 无法解析")
        } catch (exception: IllegalArgumentException) {
            throw BackupException.Format(exception.message ?: "JSON 无法解析")
        }
        if (snapshot.format != FORMAT) {
            throw BackupException.Format("不认识的备份格式 ${snapshot.format}")
        }
        val accounts = snapshot.accounts.map { it.toEntity() }.withSingleDefault()
        val categories = snapshot.categories.map { it.toEntity() }
        val bills = snapshot.bills.map { it.toEntity() }
        val budgets = snapshot.budgets.map { it.toEntity() }
        validate(accounts, categories, bills, budgets)
        database.withTransaction {
            billDao.deleteAll()
            budgetDao.deleteAll()
            categoryDao.deleteAll()
            accountDao.deleteAll()
            accountDao.insertAll(accounts)
            categoryDao.insertAll(categories)
            billDao.insertAll(bills)
            budgetDao.insertAll(budgets)
        }
    }

    private fun validate(
        accounts: List<Account>,
        categories: List<Category>,
        bills: List<Bill>,
        budgets: List<Budget>,
    ) {
        requireUniqueIds(accounts.map { it.id }, "账户")
        requireUniqueIds(categories.map { it.id }, "分类")
        requireUniqueIds(bills.map { it.id }, "账单")
        requireUniqueIds(budgets.map { it.id }, "预算")
        val accountIds = accounts.map { it.id }.toSet()
        val categoriesById = categories.associateBy { it.id }
        bills.forEach { bill ->
            if (bill.accountId !in accountIds) throw BackupException.Rejected(LedgerReject.AccountNotFound)
            when (bill.type) {
                BillType.Expense, BillType.Income -> {
                    if (bill.amount <= 0L) throw BackupException.Rejected(LedgerReject.InvalidAmount)
                    val categoryId = bill.categoryId ?: throw BackupException.Rejected(LedgerReject.CategoryNotFound)
                    val category = categoriesById[categoryId]
                        ?: throw BackupException.Rejected(LedgerReject.CategoryNotFound)
                    val expected = if (bill.type == BillType.Expense) CategoryType.Expense else CategoryType.Income
                    if (category.type != expected) {
                        throw BackupException.Rejected(LedgerReject.CategoryTypeMismatch)
                    }
                }
                BillType.Transfer -> {
                    if (bill.categoryId != null) throw BackupException.Rejected(LedgerReject.CategoryForbidden)
                    if (bill.amount == 0L) throw BackupException.Rejected(LedgerReject.InvalidAmount)
                }
            }
        }
        budgets.forEach { budget ->
            if (!budget.periodEnd.isAfter(budget.periodStart)) {
                throw BackupException.Rejected(LedgerReject.InvalidPeriod)
            }
            if (budget.limitAmount <= 0L) throw BackupException.Rejected(LedgerReject.InvalidLimit)
            if (!BudgetMath.alertPercentAllowed(budget.alertPercent)) {
                throw BackupException.Rejected(LedgerReject.InvalidAlertPercent)
            }
            val cycleDay = budget.cycleDay
            if (cycleDay != null && (budget.periodType != BudgetPeriodType.Month || cycleDay !in 1..31)) {
                throw BackupException.Rejected(LedgerReject.InvalidPeriod)
            }
            when (budget.budgetType) {
                BudgetType.Global -> {
                    if (budget.categoryId != null) throw BackupException.Rejected(LedgerReject.BudgetCategoryForbidden)
                }
                BudgetType.Category -> {
                    val categoryId = budget.categoryId ?: throw BackupException.Rejected(LedgerReject.BudgetCategoryRequired)
                    val category = categoriesById[categoryId]
                        ?: throw BackupException.Rejected(LedgerReject.CategoryNotFound)
                    if (category.type != CategoryType.Expense) {
                        throw BackupException.Rejected(LedgerReject.BudgetCategoryMustBeExpense)
                    }
                }
            }
        }
        val enabled = budgets.filter { it.isEnable }
        for (index in enabled.indices) {
            val left = enabled[index]
            for (otherIndex in index + 1 until enabled.size) {
                val right = enabled[otherIndex]
                val sameScope = BudgetMath.sameDimension(
                    left.budgetType,
                    left.categoryId,
                    right.budgetType,
                    right.categoryId,
                )
                val overlap = BudgetMath.overlaps(
                    left.periodStart,
                    left.periodEnd,
                    right.periodStart,
                    right.periodEnd,
                )
                if (sameScope && overlap) throw BackupException.Rejected(LedgerReject.BudgetOverlap)
            }
        }
    }

    private fun requireUniqueIds(ids: List<Long>, label: String) {
        if (ids.any { it <= 0L }) throw BackupException.Format("$label id 必须大于 0")
        if (ids.size != ids.toSet().size) throw BackupException.Format("$label id 重复")
    }

    private companion object {
        const val FORMAT: Int = 1

        val json: Json = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = false
        }
    }
}

@Serializable
private data class LedgerSnapshot(
    val format: Int,
    val exportedAt: String,
    val accounts: List<AccountRecord>,
    val categories: List<CategoryRecord>,
    val bills: List<BillRecord>,
    val budgets: List<BudgetRecord>,
)

@Serializable
private data class AccountRecord(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val type: Int,
    val balance: Long,
    val isEnable: Boolean,
    val isDefault: Boolean = false,
    val remark: String? = null,
    val createTime: String,
    val updateTime: String,
)

@Serializable
private data class CategoryRecord(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val type: Int,
    val sort: Int,
    val isEnable: Boolean,
)

@Serializable
private data class BillRecord(
    val id: Long,
    val amount: Long,
    val type: Int,
    val categoryId: Long? = null,
    val accountId: Long,
    val billTime: String,
    val remark: String? = null,
    val createTime: String,
    val updateTime: String,
)

@Serializable
private data class BudgetRecord(
    val id: Long,
    val budgetType: Int,
    val categoryId: Long? = null,
    val limitAmount: Long,
    val alertPercent: Int,
    val periodType: Int,
    val cycleDay: Int? = null,
    val periodStart: String,
    val periodEnd: String,
    val isEnable: Boolean,
    val remark: String? = null,
    val createTime: String,
    val updateTime: String,
)

private fun Account.toRecord() = AccountRecord(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type.code,
    balance = balance,
    isEnable = isEnable,
    isDefault = isDefault,
    remark = remark,
    createTime = LedgerTime.encode(createTime),
    updateTime = LedgerTime.encode(updateTime),
)

private fun Category.toRecord() = CategoryRecord(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type.code,
    sort = sort,
    isEnable = isEnable,
)

private fun Bill.toRecord() = BillRecord(
    id = id,
    amount = amount,
    type = type.code,
    categoryId = categoryId,
    accountId = accountId,
    billTime = LedgerTime.encode(billTime),
    remark = remark,
    createTime = LedgerTime.encode(createTime),
    updateTime = LedgerTime.encode(updateTime),
)

private fun Budget.toRecord() = BudgetRecord(
    id = id,
    budgetType = budgetType.code,
    categoryId = categoryId,
    limitAmount = limitAmount,
    alertPercent = alertPercent,
    periodType = periodType.code,
    cycleDay = cycleDay,
    periodStart = LedgerTime.encode(periodStart),
    periodEnd = LedgerTime.encode(periodEnd),
    isEnable = isEnable,
    remark = remark,
    createTime = LedgerTime.encode(createTime),
    updateTime = LedgerTime.encode(updateTime),
)

private fun AccountRecord.toEntity(): Account = Account(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = code(AccountType::fromCode, type, "账户类型"),
    balance = balance,
    isEnable = isEnable,
    isDefault = isDefault,
    remark = remark,
    createTime = time(createTime),
    updateTime = time(updateTime),
)

/** 导入后只留一个当前展示账户。文件里没标时，用第一个启用账户，再没有就用第一条。 */
private fun List<Account>.withSingleDefault(): List<Account> {
    if (isEmpty()) return this
    val chosen = firstOrNull { it.isDefault }?.id
        ?: firstOrNull { it.isEnable }?.id
        ?: first().id
    return map { account -> account.copy(isDefault = account.id == chosen) }
}

private fun CategoryRecord.toEntity(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = code(CategoryType::fromCode, type, "分类类型"),
    sort = sort,
    isEnable = isEnable,
)

private fun BillRecord.toEntity(): Bill = Bill(
    id = id,
    amount = amount,
    type = code(BillType::fromCode, type, "账单类型"),
    categoryId = categoryId,
    accountId = accountId,
    billTime = time(billTime),
    remark = remark,
    createTime = time(createTime),
    updateTime = time(updateTime),
)

private fun BudgetRecord.toEntity(): Budget = Budget(
    id = id,
    budgetType = code(BudgetType::fromCode, budgetType, "预算类型"),
    categoryId = categoryId,
    limitAmount = limitAmount,
    alertPercent = alertPercent,
    periodType = code(BudgetPeriodType::fromCode, periodType, "预算周期"),
    cycleDay = cycleDay,
    periodStart = time(periodStart),
    periodEnd = time(periodEnd),
    isEnable = isEnable,
    remark = remark,
    createTime = time(createTime),
    updateTime = time(updateTime),
)

private fun time(value: String): LocalDateTime = try {
    LedgerTime.decode(value)
} catch (exception: IllegalArgumentException) {
    throw BackupException.Format(exception.message ?: "时间格式不对")
}

private fun <T> code(decode: (Int) -> T, value: Int, label: String): T = try {
    decode(value)
} catch (exception: IllegalArgumentException) {
    throw BackupException.Format(exception.message ?: "未知的$label")
}
