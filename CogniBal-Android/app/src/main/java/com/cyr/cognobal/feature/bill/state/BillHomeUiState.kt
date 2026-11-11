package com.cyr.cognobal.screen.feature.bill.state

import com.cyr.cognobal.data.database.entity.Bill
import com.cyr.cognobal.data.database.entity.Category
import com.cyr.cognobal.data.database.model.AccountType
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference
import com.cyr.cognobal.data.repository.BudgetLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal data class AccountBarState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val accounts: List<AccountItemUi> = emptyList(),
    val currentId: Long? = null,
)

internal data class AccountItemUi(
    val id: Long,
    val name: String,
    val type: AccountType,
    val iconKey: String,
    val colorKey: String,
    val balance: Long,
    val enabled: Boolean,
    val selected: Boolean,
)

internal data class BillRowUi(
    val id: Long,
    val categoryName: String?,
    val type: BillType,
    val time: LocalDateTime,
    val signedAmount: Long,
    val categoryIconKey: String? = null,
    val categoryColorKey: String? = null,
    /** 去掉首尾空白后仍有内容才保留。首页只给写了备注的行展示。 */
    val remark: String? = null,
)

internal data class BudgetProgressUi(
    val id: Long,
    /** 分类名。为空表示总预算，界面自己写文案。 */
    val categoryName: String?,
    val missingCategory: Boolean,
    val spent: Long,
    val limit: Long,
    val ratio: Float,
    val level: BudgetLevel,
)

internal data class BillHomeUiState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val accounts: List<AccountItemUi> = emptyList(),
    /** 顶栏正在展示的账户，对应 is_default。还没标时先用第一个启用账户。 */
    val currentId: Long? = null,
    val totalAssets: Long = 0L,
    val totalOverflow: Boolean = false,
    val monthIncome: Long = 0L,
    val monthExpense: Long = 0L,
    val monthBalance: Long = 0L,
    val monthOverflow: Boolean = false,
    val budgets: List<BudgetProgressUi> = emptyList(),
    /** 关掉后首页不排预算。设置页可以再打开。 */
    val showBudget: Boolean = true,
    val recent: List<BillRowUi> = emptyList(),
    /** 本月和近 7 天按这个本地日期取值。午夜换日后状态也会变，标题跟着换。 */
    val today: LocalDate = LocalDate.now(),
)

internal sealed interface BillHomeUiEvent {
    data class Select(val accountId: Long) : BillHomeUiEvent
    data object HideBudget : BillHomeUiEvent
}

/** 按自然日归组，日期和组内账单都从新到旧。 */
internal fun groupBillsByDay(bills: List<BillRowUi>): List<Pair<LocalDate, List<BillRowUi>>> =
    bills.groupBy { it.time.toLocalDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (day, rows) ->
            day to rows.sortedWith(
                compareByDescending<BillRowUi> { it.time }.thenByDescending { it.id },
            )
        }

internal data class BillDayGroup(
    val day: LocalDate,
    val bills: List<BillRowUi>,
)

internal data class BillMonthGroup(
    val month: YearMonth,
    val days: List<BillDayGroup>,
)

/** 先按月、再按日归组。月份、日期和当天账单都从新到旧。 */
internal fun groupBillsByMonth(bills: List<BillRowUi>): List<BillMonthGroup> =
    groupBillsByDay(bills)
        .groupBy { (day, _) -> YearMonth.from(day) }
        .entries
        .sortedByDescending { it.key }
        .map { (month, days) ->
            BillMonthGroup(
                month = month,
                days = days.map { (day, rows) -> BillDayGroup(day, rows) },
            )
        }

internal fun billsToRows(bills: List<Bill>, categories: Map<Long, Category>): List<BillRowUi> {
    return bills.map { bill ->
        val category = bill.categoryId?.let { categories[it] }
        BillRowUi(
            id = bill.id,
            categoryName = category?.name,
            type = bill.type,
            time = bill.billTime,
            signedAmount = when (bill.type) {
                BillType.Expense -> -bill.amount
                BillType.Income -> bill.amount
                BillType.Transfer -> bill.amount
            },
            categoryIconKey = category?.icon,
            categoryColorKey = category?.color,
            remark = bill.remark?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}
