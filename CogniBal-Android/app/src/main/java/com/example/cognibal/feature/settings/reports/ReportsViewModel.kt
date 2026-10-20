package com.example.cognibal.screen.feature.settings.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.query.MonthTrendSummary
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.StatsRepository
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.tool.balanceOf
import com.example.cognibal.screen.feature.settings.tool.endExclusive
import com.example.cognibal.screen.feature.settings.tool.startInclusive
import dagger.hilt.android.EntryPointAccessors
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import com.example.cognibal.screen.feature.settings.reports.state.ReportMonthUi
import com.example.cognibal.screen.feature.settings.reports.state.ReportSpan
import com.example.cognibal.screen.feature.settings.reports.state.ReportsUiEvent
import com.example.cognibal.screen.feature.settings.reports.state.ReportsUiState

/**
 * 报表页。近 6 个月或 12 个月的收支趋势。
 *
 * 没有账单的月份也补一行 0，柱状图的横轴才是连续的。
 */
internal class ReportsViewModel(
    private val stats: StatsRepository,
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    private val span = MutableStateFlow(ReportSpan.SixMonths)

    val uiState: StateFlow<ReportsUiState> = span.flatMapLatest { current ->
        val end = YearMonth.now()
        val start = end.minusMonths((current.monthCount - 1).toLong())
        combine(
            stats.observeMonthlyTrend(start.startInclusive(), end.endExclusive()),
            preferences.preferences,
        ) { rows, prefs ->
            ReportsUiState(
                loading = false,
                span = current,
                currencySymbol = prefs.currency.symbol,
                months = trendMonths(current, rows),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ReportsUiState(),
    )

    fun onEvent(event: ReportsUiEvent) {
        when (event) {
            is ReportsUiEvent.SelectSpan -> span.value = event.span
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("ReportsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                ReportsViewModel(
                    stats = entry.statsRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}

private fun trendMonths(span: ReportSpan, rows: List<MonthTrendSummary>): List<ReportMonthUi> {
    val end = YearMonth.now()
    val start = end.minusMonths((span.monthCount - 1).toLong())
    val byMonth = rows.associateBy { row -> row.month }
    return (0 until span.monthCount).map { offset ->
        val month = start.plusMonths(offset.toLong())
        val row = byMonth[month.toString()]
        val income = row?.incomeAmount ?: 0L
        val expense = row?.expenseAmount ?: 0L
        val (balance, overflow) = balanceOf(income, expense)
        ReportMonthUi(
            month = month,
            income = income,
            expense = expense,
            balance = balance,
            balanceOverflow = overflow,
        )
    }
}
