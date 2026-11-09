package com.cyr.cognobal.screen.feature.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cyr.cognobal.data.database.LedgerTime
import com.cyr.cognobal.data.database.entity.Account
import com.cyr.cognobal.data.database.entity.Category
import com.cyr.cognobal.data.database.model.BudgetType
import com.cyr.cognobal.data.database.query.PeriodAmountSummary
import com.cyr.cognobal.data.preferences.AppPreferencesRepository
import com.cyr.cognobal.data.repository.AccountRepository
import com.cyr.cognobal.data.repository.BillRepository
import com.cyr.cognobal.data.repository.BudgetMath
import com.cyr.cognobal.data.repository.BudgetProgress
import com.cyr.cognobal.data.repository.BudgetRepository
import com.cyr.cognobal.data.repository.CategoryRepository
import com.cyr.cognobal.data.repository.StatsRepository
import com.cyr.cognobal.data.repository.assetAccountOrder
import com.cyr.cognobal.data.repository.balanceOf
import com.cyr.cognobal.data.repository.sumMinorAmounts
import com.cyr.cognobal.di.LedgerEntryPoint
import com.cyr.cognobal.time.TimeManager
import dagger.hilt.android.EntryPointAccessors
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import com.cyr.cognobal.screen.feature.bill.state.AccountBarState
import com.cyr.cognobal.screen.feature.bill.state.AccountItemUi
import com.cyr.cognobal.screen.feature.bill.state.BillHomeUiEvent
import com.cyr.cognobal.screen.feature.bill.state.BillHomeUiState
import com.cyr.cognobal.screen.feature.bill.state.BudgetProgressUi
import com.cyr.cognobal.screen.feature.bill.state.billsToRows
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 记账首页。
 *
 * 当前账户记在 is_default。总资产是全部账户余额之和。
 * 本月收支和最近 7 个自然日的账单只看当前账户。本地午夜会重新取这两个窗口。
 * 首页预算只画当前周期里启用的总预算。关掉后整块不出现，设置页可以再打开。
 */
internal class BillHomeViewModel(
    private val accounts: AccountRepository,
    private val bills: BillRepository,
    private val budgets: BudgetRepository,
    private val categories: CategoryRepository,
    private val stats: StatsRepository,
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            try {
                budgets.ensureCurrentPeriods()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // 没补上下个周期时，首页仍显示已经覆盖今天的预算。
            }
        }
    }

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
        val (total, totalOverflow) = sumMinorAmounts(bar.accounts.map { it.balance })
        var seenToday = false
        return TimeManager.observeToday().flatMapLatest { today ->
            val month = YearMonth.from(today)
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
                bills.observeAccountBetween(
                    currentId,
                    today.minusDays(RECENT_SPAN_DAYS - 1).atStartOfDay(),
                    today.plusDays(1).atStartOfDay(),
                )
            }
            combine(
                combine(period, recent, ::Pair),
                combine(
                    budgets.observeProgress(),
                    categories.observeAll(),
                    preferences.preferences,
                    ::Triple,
                ),
            ) { (monthSummary, latest), (progress, categoryRows, prefs) ->
                val names = categoryRows.associateBy { it.id }
                val (balance, monthOverflow) = balanceOf(
                    monthSummary.incomeAmount,
                    monthSummary.expenseAmount,
                )
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
                    showBudget = prefs.budgetHomeVisible,
                    recent = billsToRows(latest, names),
                    today = today,
                )
            }.onStart {
                // 打开首页时 init 已经补过当前周期。之后只在跨日时再补，避免和启动补写抢同一行。
                if (seenToday) {
                    try {
                        budgets.ensureCurrentPeriods()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        // 新周期没写上时，首页仍显示已经覆盖今天的预算。
                    }
                }
                seenToday = true
            }
        }
    }

    fun onEvent(event: BillHomeUiEvent) {
        when (event) {
            is BillHomeUiEvent.Select -> viewModelScope.launch { accounts.setDisplayed(event.accountId) }
            BillHomeUiEvent.HideBudget -> viewModelScope.launch { preferences.setBudgetHomeVisible(false) }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** 含今天在内的自然日天数。本地午夜会重新取这个窗口。 */
        private const val RECENT_SPAN_DAYS = 7L

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
            val categoryId = item.budget.categoryId
            BudgetProgressUi(
                id = item.budget.id,
                categoryName = categoryId?.let { categories[it]?.name },
                missingCategory = item.budget.budgetType == BudgetType.Category &&
                    (categoryId == null || categories[categoryId] == null),
                spent = spent,
                limit = limit,
                ratio = BudgetMath.progressFraction(spent, limit),
                level = item.level(),
            )
        }
}

private fun accountBarState(rows: List<Account>, symbol: String): AccountBarState {
    val ordered = rows.sortedWith(assetAccountOrder(Account::isEnable, Account::id))
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

