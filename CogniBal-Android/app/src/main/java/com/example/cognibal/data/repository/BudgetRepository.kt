package com.example.cognibal.data.repository

import androidx.room.withTransaction
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.dao.BudgetDao
import com.example.cognibal.data.database.dao.CategoryDao
import com.example.cognibal.data.database.entity.Budget
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.database.model.CategoryType
import com.example.cognibal.data.database.query.BudgetSpendRow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 预算。已花费不写入本表，[observeProgress] 把预算行和账单实时合计拼在一起。
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val database: AppDatabase,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
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

    suspend fun insert(draft: BudgetDraft): Long = onIo {
        database.withTransaction {
            val now = LedgerTime.now()
            budgetDao.insert(draft.toRow(existing = null, now = now))
        }
    }

    suspend fun update(id: Long, draft: BudgetDraft) = onIo {
        database.withTransaction {
            val existing = budgetDao.getById(id) ?: throw LedgerException(LedgerReject.BudgetNotFound)
            budgetDao.update(draft.toRow(existing = existing, now = LedgerTime.now()))
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = onIo {
        val existing = budgetDao.getById(id) ?: throw LedgerException(LedgerReject.BudgetNotFound)
        if (existing.isEnable == enabled) return@onIo
        budgetDao.update(existing.copy(isEnable = enabled, updateTime = LedgerTime.now()))
    }

    suspend fun delete(id: Long) = onIo {
        if (budgetDao.getById(id) == null) throw LedgerException(LedgerReject.BudgetNotFound)
        budgetDao.deleteById(id)
    }

    private suspend fun BudgetDraft.toRow(existing: Budget?, now: java.time.LocalDateTime): Budget {
        if (limitAmount <= 0L || limitAmount > LedgerLimits.MAX_MINOR_AMOUNT) {
            throw LedgerException(LedgerReject.InvalidLimit)
        }
        if (alertPercent !in 0..100) throw LedgerException(LedgerReject.InvalidAlertPercent)
        val start = LedgerTime.normalize(periodStart)
        val end = LedgerTime.normalize(periodEnd)
        if (!end.isAfter(start)) throw LedgerException(LedgerReject.InvalidPeriod)
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
            periodStart = start,
            periodEnd = end,
            isEnable = existing?.isEnable ?: true,
            remark = LedgerText.cleanRemark(remark),
            createTime = existing?.createTime ?: now,
            updateTime = now,
        )
    }
}

/**
 * 一条预算和它此刻的已花费。已花费来自账单合计，不是预算表上的列。
 * 超支时 [remaining] 为负。[reachedAlert] 只做比较，不写提醒表。
 */
data class BudgetProgress(
    val budget: Budget,
    val spentAmount: Long,
) {
    val remaining: Long = budget.limitAmount - spentAmount

    fun reachedAlert(): Boolean {
        if (budget.alertPercent <= 0 || budget.limitAmount <= 0L) return false
        return BigDecimal.valueOf(spentAmount) * BigDecimal(100) >=
            BigDecimal.valueOf(budget.limitAmount) * BigDecimal(budget.alertPercent)
    }

    /** 给进度条用的比例。存储和比较仍用整数分，不用这个小数。 */
    fun spentRatio(): Double {
        if (budget.limitAmount <= 0L) return 0.0
        return spentAmount.toDouble() / budget.limitAmount.toDouble()
    }
}
