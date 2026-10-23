package com.example.cognibal.screen.feature.settings.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.query.CategoryAmountSummary
import com.example.cognibal.data.preferences.AppPreferencesRepository
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
 */
internal class StatisticsViewModel(
    private val stats: StatsRepository,
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
        return combine(
            stats.observePeriod(start, end),
            breakdown(kind, BillType.Expense, start, end),
            breakdown(kind, BillType.Income, start, end),
            categories.observeAll(),
            preferences.preferences,
        ) { period, expenseRows, incomeRows, categoryRows, prefs ->
            val (total, overflow) = when (kind) {
                StatisticsKind.Expense -> period.expenseAmount to false
                StatisticsKind.Income -> period.incomeAmount to false
                StatisticsKind.All -> balanceOf(period.incomeAmount, period.expenseAmount)
            }
            val sections = statisticsSections(
                kind = kind,
                expenseRows = expenseRows,
                incomeRows = incomeRows,
                categories = categoryRows,
                expenseTotal = period.expenseAmount,
                incomeTotal = period.incomeAmount,
            )
            StatisticsUiState(
                loading = false,
                month = month,
                canGoNext = month < YearMonth.now(),
                kind = kind,
                currencySymbol = prefs.currency.symbol,
                total = total,
                totalOverflow = overflow,
                slices = statisticsSlices(
                    kind = kind,
                    sections = sections,
                    expenseTotal = period.expenseAmount,
                    incomeTotal = period.incomeAmount,
                ),
                sections = sections,
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
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}
