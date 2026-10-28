package com.example.cognibal.screen.feature.settings.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.entity.Account
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.query.CategoryAmountSummary
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.AccountRepository
import com.example.cognibal.data.repository.BillRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.data.repository.StatsRepository
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsKind
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsUiEvent
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsUiState
import com.example.cognibal.screen.feature.settings.tool.balanceOf
import com.example.cognibal.screen.feature.settings.tool.cappedToCurrent
import com.example.cognibal.screen.feature.settings.tool.endExclusive
import com.example.cognibal.screen.feature.settings.tool.startInclusive
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * 统计页。看一个月里的分类构成。
 *
 * 月份和三档留在这里。全部档同时要支出和收入的分类；单档只订阅那一档。
 * 图的扇区在这里算好，界面不再对金额做聚合。
 * 同月账单一起备好，点开分类时再按分类取出。
 */
internal class StatisticsViewModel(
    private val stats: StatsRepository,
    private val bills: BillRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val kind = MutableStateFlow(StatisticsKind.Expense)

    val uiState: StateFlow<StatisticsUiState> = combine(month, kind) { currentMonth, currentKind ->
        currentMonth to currentKind
    }.flatMapLatest { (currentMonth, currentKind) ->
        observe(currentMonth, currentKind)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StatisticsUiState(),
    )

    fun onEvent(event: StatisticsUiEvent) {
        when (event) {
            StatisticsUiEvent.PreviousMonth -> month.update { it.minusMonths(1) }
            StatisticsUiEvent.NextMonth -> month.update { it.plusMonths(1).cappedToCurrent() }
            is StatisticsUiEvent.SelectKind -> kind.value = event.kind
        }
    }

    private fun observe(month: YearMonth, kind: StatisticsKind): Flow<StatisticsUiState> {
        val start = month.startInclusive()
        val end = month.endExclusive()
        val totals = combine(
            stats.observePeriod(start, end),
            breakdown(kind, BillType.Expense, start, end),
            breakdown(kind, BillType.Income, start, end),
        ) { period, expenseRows, incomeRows ->
            PeriodBreakdown(period, expenseRows, incomeRows)
        }
        val names = combine(
            categories.observeAll(),
            accounts.observeAll(),
            preferences.preferences,
        ) { categoryRows, accountRows, prefs ->
            LedgerNames(categoryRows, accountRows, prefs.currency.symbol)
        }
        return combine(totals, names, bills.observeBetween(start, end)) { breakdown, book, billRows ->
            val (total, overflow) = when (kind) {
                StatisticsKind.Expense -> breakdown.period.expenseAmount to false
                StatisticsKind.Income -> breakdown.period.incomeAmount to false
                StatisticsKind.All -> balanceOf(
                    breakdown.period.incomeAmount,
                    breakdown.period.expenseAmount,
                )
            }
            val sections = statisticsSections(
                kind = kind,
                expenseRows = breakdown.expenseRows,
                incomeRows = breakdown.incomeRows,
                categories = book.categories,
                expenseTotal = breakdown.period.expenseAmount,
                incomeTotal = breakdown.period.incomeAmount,
            )
            StatisticsUiState(
                loading = false,
                month = month,
                canGoNext = month < YearMonth.now(),
                kind = kind,
                currencySymbol = book.currencySymbol,
                total = total,
                totalOverflow = overflow,
                slices = statisticsSlices(
                    kind = kind,
                    sections = sections,
                    expenseTotal = breakdown.period.expenseAmount,
                    incomeTotal = breakdown.period.incomeAmount,
                ),
                sections = sections,
                bills = statisticsBills(billRows, book.accounts, kind),
            )
        }
    }

    /** 单档不订阅另一档，避免翻月时多打一轮分类汇总。 */
    private fun breakdown(
        kind: StatisticsKind,
        type: BillType,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Flow<List<CategoryAmountSummary>> {
        val needed = when (kind) {
            StatisticsKind.All -> true
            StatisticsKind.Expense -> type == BillType.Expense
            StatisticsKind.Income -> type == BillType.Income
        }
        if (!needed) return flowOf(emptyList<CategoryAmountSummary>())
        return stats.observeCategoryBreakdown(type, start, end)
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("StatisticsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                StatisticsViewModel(
                    stats = entry.statsRepository(),
                    bills = entry.billRepository(),
                    accounts = entry.accountRepository(),
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}

private data class PeriodBreakdown(
    val period: PeriodAmountSummary,
    val expenseRows: List<CategoryAmountSummary>,
    val incomeRows: List<CategoryAmountSummary>,
)

private data class LedgerNames(
    val categories: List<Category>,
    val accounts: List<Account>,
    val currencySymbol: String,
)
