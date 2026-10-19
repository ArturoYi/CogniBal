package com.example.cognibal.screen.bill.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.LedgerTime
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.BudgetType
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.BillRepository
import com.example.cognibal.data.repository.BudgetProgress
import com.example.cognibal.data.repository.BudgetRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.data.repository.StatsRepository
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.settings.model.CurrencyPreference
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 记账首页。
 *
 * 当前账户记在 is_default。总资产是全部账户余额之和。
 * 本月收支和最近账单只看当前账户。预算不区分账户，只显示此刻落在周期里、并且启用的条目。
 */
internal class BillHomeViewModel(
    private val accounts: AccountRepository,
    private val bills: BillRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val stats: StatsRepository,
    preferences: AppPreferencesRepository,
) : ViewModel() {
    /**
     * 顶栏只依赖账户和货币。本月收支、最近账单还在查时，这里可以先有当前账户。
     * 首页初次订阅会先拿到 [AccountBarState.loading]，那一笔不能当成“没有账户”。
     */
    val accountBar: StateFlow<AccountBarState> = combine(
        accounts.observeAll(),
        preferences.preferences,
    ) { rows, prefs ->
        accountBarState(rows, prefs.currency.symbol)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AccountBarState(),
    )

    val uiState: StateFlow<BillHomeUiState> = accountBar
        .filter { bar -> !bar.loading }
        .flatMapLatest { bar -> homeContent(bar) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BillHomeUiState(),
        )

    private fun homeContent(bar: AccountBarState): Flow<BillHomeUiState> {
        val currentId = bar.currentId
        val (total, totalOverflow) = sumBalances(bar.accounts.map { it.balance })
        val month = YearMonth.now()
        val start = month.atDay(1).atStartOfDay()
        val end = month.plusMonths(1).atDay(1).atStartOfDay()
        val period = if (currentId == null) {
            flowOf(PeriodAmountSummary(0L, 0L))
        } else {
            stats.observeAccountPeriod(currentId, start, end)
        }
        val recent = if (currentId == null) {
            flowOf(emptyList())
        } else {
            bills.observeRecent(currentId, RECENT_LIMIT)
        }
        return combine(
            period,
            recent,
            budgets.observeProgress(),
            categories.observeAll(),
        ) { monthSummary, latest, progress, categoryRows ->
            val names = categoryRows.associateBy { it.id }
            val (balance, monthOverflow) = monthBalance(monthSummary)
            BillHomeUiState(
                loading = false,
                currencySymbol = bar.currencySymbol,
                accounts = bar.accounts,
                currentId = currentId,
                totalAssets = total,
                totalOverflow = totalOverflow,
                monthIncome = monthSummary.incomeAmount,
                monthExpense = monthSummary.expenseAmount,
                monthBalance = balance,
                monthOverflow = monthOverflow,
                budgets = activeBudgets(progress, names),
                recent = billsToRows(latest, names),
            )
        }
    }

    fun onEvent(event: BillHomeUiEvent) {
        when (event) {
            is BillHomeUiEvent.Select -> viewModelScope.launch { accounts.setDisplayed(event.accountId) }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val RECENT_LIMIT = 20

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("BillHomeViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                BillHomeViewModel(
                    accounts = entry.accountRepository(),
                    bills = entry.billRepository(),
                    budgets = entry.budgetRepository(),
                    categories = entry.categoryRepository(),
                    stats = entry.statsRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}

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
)

internal data class BudgetProgressUi(
    val id: Long,
    /** 分类名。为空表示总预算，界面自己写文案。 */
    val categoryName: String?,
    val missingCategory: Boolean,
    val spent: Long,
    val limit: Long,
    val ratio: Float,
    val over: Boolean,
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
    val recent: List<BillRowUi> = emptyList(),
)

internal sealed interface BillHomeUiEvent {
    data class Select(val accountId: Long) : BillHomeUiEvent
}

internal fun billsToRows(bills: List<Bill>, categories: Map<Long, Category>): List<BillRowUi> {
    return bills.map { bill ->
        BillRowUi(
            id = bill.id,
            categoryName = bill.categoryId?.let { categories[it]?.name },
            type = bill.type,
            time = bill.billTime,
            signedAmount = when (bill.type) {
                BillType.Expense -> -bill.amount
                BillType.Income -> bill.amount
                BillType.Transfer -> bill.amount
            },
        )
    }
}

private fun activeBudgets(
    progress: List<BudgetProgress>,
    categories: Map<Long, Category>,
): List<BudgetProgressUi> {
    val now = LedgerTime.now()
    return progress
        .filter { item ->
            item.budget.isEnable &&
                !now.isBefore(item.budget.periodStart) &&
                now.isBefore(item.budget.periodEnd)
        }
        .sortedBy { it.budget.id }
        .map { item ->
            val limit = item.budget.limitAmount
            val spent = item.spentAmount
            val ratio = if (limit <= 0L) {
                0f
            } else {
                (spent.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f)
            }
            val categoryId = item.budget.categoryId
            BudgetProgressUi(
                id = item.budget.id,
                categoryName = categoryId?.let { categories[it]?.name },
                missingCategory = item.budget.budgetType == BudgetType.Category &&
                    (categoryId == null || categories[categoryId] == null),
                spent = spent,
                limit = limit,
                ratio = ratio,
                over = limit > 0L && spent > limit,
            )
        }
}

private fun monthBalance(summary: PeriodAmountSummary): Pair<Long, Boolean> {
    return try {
        Math.subtractExact(summary.incomeAmount, summary.expenseAmount) to false
    } catch (_: ArithmeticException) {
        0L to true
    }
}

private fun accountBarState(rows: List<Account>, symbol: String): AccountBarState {
    val ordered = rows.sortedWith(
        compareByDescending<Account> { it.isEnable }.thenBy { it.id },
    )
    val currentId = ordered.firstOrNull { it.isDefault }?.id ?: ordered.firstOrNull()?.id
    return AccountBarState(
        loading = false,
        currencySymbol = symbol,
        currentId = currentId,
        accounts = ordered.map { account ->
            AccountItemUi(
                id = account.id,
                name = account.name,
                type = account.type,
                iconKey = account.icon,
                colorKey = account.color,
                balance = account.balance,
                enabled = account.isEnable,
                selected = account.id == currentId,
            )
        },
    )
}

private fun sumBalances(balances: List<Long>): Pair<Long, Boolean> {
    var total = 0L
    for (balance in balances) {
        try {
            total = Math.addExact(total, balance)
        } catch (_: ArithmeticException) {
            return 0L to true
        }
    }
    return total to false
}
