package com.example.cognibal.data.repository

import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.BillDao
import com.example.cognibal.data.database.dao.BudgetDao
import com.example.cognibal.data.database.dao.CategoryDao
import com.example.cognibal.data.database.entity.Budget
import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.database.model.CategoryType
import com.example.cognibal.data.database.query.BudgetSpendRow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 预算。已花费不写入本表，[observeProgress] 把预算行和账单实时合计拼在一起。
 *
 * 一笔支出可以同时计入总预算和它的分类预算。
 * 同一范围里，两个启用预算的周期不能相交，否则这一笔会扣两次。
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val database: AppDatabase,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val billDao: BillDao,
) {
    fun observeAll(): Flow<List<Budget>> = budgetDao.observeAll()

    fun observeEnabled(): Flow<List<Budget>> = budgetDao.observeEnabled()

    fun observeProgress(): Flow<List<BudgetProgress>> = combine(
        budgetDao.observeAll(),
        budgetDao.observeSpent(),
    ) { budgets, spent ->
        val spentById = spent.associateBy(BudgetSpendRow::budgetId)
        budgets.map { budget ->
            BudgetProgress(
                budget = budget,
                spentAmount = spentById[budget.id]?.spentAmount ?: 0L,
            )
        }
    }

    /**
     * 当前周期还没有行时，按同一范围里最近一条仍启用的预算复制一条。
     * 只补今天所在的周期，不回填中间空掉的月份。已经结束的行不动。
     */
    suspend fun ensureCurrentPeriods() = onIo {
        database.withTransaction {
            val now = LedgerTime.now()
            val all = budgetDao.getAll()
            val pending = ArrayList<Budget>()
            val groups = all.groupBy { row ->
                DimensionKey(row.budgetType, row.categoryId, row.periodType)
            }
            for (rows in groups.values) {
                val covered = rows.any { row ->
                    BudgetMath.contains(row.periodStart, row.periodEnd, now)
                }
                if (covered) continue
                val previous = rows
                    .filter { row -> row.isEnable && !row.periodEnd.isAfter(now) }
                    .maxWithOrNull(compareBy<Budget> { it.periodEnd }.thenBy { it.id })
                    ?: continue
                val day = when (previous.periodType) {
                    BudgetPeriodType.Month -> previous.cycleDay ?: previous.periodStart.dayOfMonth
                    else -> null
                }
                val range = if (day != null && day in 1..31) {
                    BudgetPeriods.monthContaining(day, now.toLocalDate())
                } else {
                    BudgetPeriods.around(previous.periodType, now.toLocalDate())
                }
                if (!BudgetMath.contains(range.startInclusive, range.endExclusive, now)) continue
                val candidate = previous.copy(
                    id = 0,
                    alertPercent = if (BudgetMath.alertPercentAllowed(previous.alertPercent)) {
                        previous.alertPercent
                    } else {
                        BudgetMath.DEFAULT_ALERT_PERCENT
                    },
                    cycleDay = day?.takeIf { previous.periodType == BudgetPeriodType.Month },
                    periodStart = range.startInclusive,
                    periodEnd = range.endExclusive,
                    isEnable = true,
                    createTime = now,
                    updateTime = now,
                )
                val occupied = all + pending
                if (conflicts(candidate, occupied)) continue
                pending += candidate
            }
            if (pending.isNotEmpty()) budgetDao.insertAll(pending)
        }
    }

    /**
     * 这笔支出如果记在 [billTime]，会命中哪些启用预算。
     * 总预算和匹配的分类预算可以同时出现。同一范围不会出现两条。
     * [amount] 只用于推算记下之后的已花费，这里不写库。
     */
    suspend fun impactsOfExpense(
        categoryId: Long,
        billTime: LocalDateTime,
        amount: Long,
    ): List<BudgetImpact> = onIo {
        val moment = LedgerTime.normalize(billTime)
        budgetDao.getAll()
            .filter { budget ->
                budget.isEnable &&
                    BudgetMath.contains(budget.periodStart, budget.periodEnd, moment) &&
                    (
                        budget.budgetType == BudgetType.Global ||
                            budget.categoryId == categoryId
                        )
            }
            .map { budget ->
                val spent = spentOf(budget)
                BudgetImpact(budget = budget, spentBefore = spent, addedAmount = amount)
            }
    }

    suspend fun insert(draft: BudgetDraft, enabled: Boolean = true): Long = onIo {
        database.withTransaction {
            val now = LedgerTime.now()
            val row = draft.toRow(existing = null, now = now).copy(isEnable = enabled)
            requireNoConflict(row)
            budgetDao.insert(row)
        }
    }

    suspend fun update(id: Long, draft: BudgetDraft, enabled: Boolean) = onIo {
        database.withTransaction {
            val existing = budgetDao.getById(id) ?: throw LedgerException(LedgerReject.BudgetNotFound)
            val now = LedgerTime.now()
            val row = draft.toRow(existing = existing, now = now).copy(isEnable = enabled)
            requireNoConflict(row)
            budgetDao.update(row)
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = onIo {
        database.withTransaction {
            val existing = budgetDao.getById(id) ?: throw LedgerException(LedgerReject.BudgetNotFound)
            if (existing.isEnable == enabled) return@withTransaction
            val row = existing.copy(isEnable = enabled, updateTime = LedgerTime.now())
            requireNoConflict(row)
            budgetDao.update(row)
        }
    }

    suspend fun delete(id: Long) = onIo {
        if (budgetDao.getById(id) == null) throw LedgerException(LedgerReject.BudgetNotFound)
        budgetDao.deleteById(id)
    }

    private suspend fun spentOf(budget: Budget): Long = when (budget.budgetType) {
        BudgetType.Global -> billDao.sumExpense(budget.periodStart, budget.periodEnd)
        BudgetType.Category -> {
            val categoryId = budget.categoryId ?: return 0L
            billDao.sumCategoryExpense(categoryId, budget.periodStart, budget.periodEnd)
        }
    }

    private suspend fun requireNoConflict(row: Budget) {
        if (!row.isEnable) return
        if (conflicts(row, budgetDao.getAll())) {
            throw LedgerException(LedgerReject.BudgetOverlap)
        }
    }

    private fun conflicts(row: Budget, others: List<Budget>): Boolean = others.any { other ->
        val sameRow = row.id != 0L && row.id == other.id
        !sameRow &&
            other.isEnable &&
            BudgetMath.sameDimension(
                row.budgetType,
                row.categoryId,
                other.budgetType,
                other.categoryId,
            ) &&
            BudgetMath.overlaps(row.periodStart, row.periodEnd, other.periodStart, other.periodEnd)
    }

    private suspend fun BudgetDraft.toRow(existing: Budget?, now: LocalDateTime): Budget {
        if (limitAmount <= 0L || limitAmount > LedgerLimits.MAX_MINOR_AMOUNT) {
            throw LedgerException(LedgerReject.InvalidLimit)
        }
        if (!BudgetMath.alertPercentAllowed(alertPercent)) {
            throw LedgerException(LedgerReject.InvalidAlertPercent)
        }
        val start = LedgerTime.normalize(periodStart)
        val end = LedgerTime.normalize(periodEnd)
        if (!end.isAfter(start)) throw LedgerException(LedgerReject.InvalidPeriod)
        val day = when (periodType) {
            BudgetPeriodType.Month -> {
                val value = cycleDay ?: BudgetPeriods.CALENDAR_MONTH_DAY
                if (value !in 1..31) throw LedgerException(LedgerReject.InvalidPeriod)
                value
            }
            else -> {
                if (cycleDay != null) throw LedgerException(LedgerReject.InvalidPeriod)
                null
            }
        }
        val category = when (budgetType) {
            BudgetType.Global -> {
                if (categoryId != null) throw LedgerException(LedgerReject.BudgetCategoryForbidden)
                null
            }
            BudgetType.Category -> {
                val id = categoryId ?: throw LedgerException(LedgerReject.BudgetCategoryRequired)
                val found = categoryDao.getById(id) ?: throw LedgerException(LedgerReject.CategoryNotFound)
                if (found.type != CategoryType.Expense) {
                    throw LedgerException(LedgerReject.BudgetCategoryMustBeExpense)
                }
                if (!found.isEnable && id != existing?.categoryId) {
                    throw LedgerException(LedgerReject.CategoryDisabled)
                }
                found
            }
        }
        return Budget(
            id = existing?.id ?: 0,
            budgetType = budgetType,
            categoryId = category?.id,
            limitAmount = limitAmount,
            alertPercent = alertPercent,
            periodType = periodType,
            cycleDay = day,
            periodStart = start,
            periodEnd = end,
            isEnable = existing?.isEnable ?: true,
            remark = LedgerText.cleanRemark(remark),
            createTime = existing?.createTime ?: now,
            updateTime = now,
        )
    }
}

private data class DimensionKey(
    val type: BudgetType,
    val categoryId: Long?,
    val periodType: BudgetPeriodType,
)

/**
 * 一条预算和它此刻的已花费。已花费来自账单合计，不是预算表上的列。
 * 超支时 [remaining] 为负。
 */
data class BudgetProgress(
    val budget: Budget,
    val spentAmount: Long,
) {
    val remaining: Long = budget.limitAmount - spentAmount

    fun level(): BudgetLevel = BudgetMath.level(spentAmount, budget.limitAmount, budget.alertPercent)

    /** 达到预警线或已经超支。不写提醒表。 */
    fun reachedAlert(): Boolean = level() != BudgetLevel.Normal

    /** 给进度条用的比例。存储和比较仍用整数分，不用这个小数。 */
    fun progressFraction(): Float = BudgetMath.progressFraction(spentAmount, budget.limitAmount)
}

/**
 * 一笔尚未写入的支出会把某条预算推到哪里。
 * [spentAfter] 溢出时视为已经超支。
 */
data class BudgetImpact(
    val budget: Budget,
    val spentBefore: Long,
    val addedAmount: Long,
) {
    val spentAfter: Long = try {
        Math.addExact(spentBefore, addedAmount)
    } catch (_: ArithmeticException) {
        Long.MAX_VALUE
    }

    fun levelBefore(): BudgetLevel =
        BudgetMath.level(spentBefore, budget.limitAmount, budget.alertPercent)

    fun levelAfter(): BudgetLevel =
        BudgetMath.level(spentAfter, budget.limitAmount, budget.alertPercent)
}
