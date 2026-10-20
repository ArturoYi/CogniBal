package com.example.cognibal.screen.feature.settings.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.query.DayAmountSummary
import com.example.cognibal.data.database.query.PeriodAmountSummary
import com.example.cognibal.data.database.entity.Bill
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.data.repository.StatsRepository
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.bill.state.billsToRows
import com.example.cognibal.screen.feature.settings.tool.balanceOf
import com.example.cognibal.screen.feature.settings.tool.cappedToCurrent
import com.example.cognibal.screen.feature.settings.tool.endExclusive
import com.example.cognibal.screen.feature.settings.tool.startInclusive
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.example.cognibal.screen.feature.settings.overview.state.OverviewDayAmount
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiEvent
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiState

/**
 * 总览页。一个月的日历，点某一天看当天账单。
 *
 * 格子怎么排跟语言有关，留给界面。这里只给有账单的日期和选中那天的流水。
 */
internal class OverviewViewModel(
    private val stats: StatsRepository,
    private val categories: CategoryRepository,
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(
        OverviewSelection(month = YearMonth.now(), day = LocalDate.now()),
    )

    val uiState: StateFlow<OverviewUiState> = selection
        .flatMapLatest(::observe)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = OverviewUiState(),
        )

    fun onEvent(event: OverviewUiEvent) {
        when (event) {
            OverviewUiEvent.PreviousMonth -> shiftMonth(-1)
            OverviewUiEvent.NextMonth -> shiftMonth(1)
            is OverviewUiEvent.SelectDay -> selection.update { current ->
                if (YearMonth.from(event.day) != current.month) current else current.copy(day = event.day)
            }
        }
    }

    private fun shiftMonth(delta: Long) {
        selection.update { current ->
            val month = current.month.plusMonths(delta).cappedToCurrent()
            val day = month.atDay(current.day.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            OverviewSelection(month = month, day = day)
        }
    }

    private fun observe(selection: OverviewSelection): Flow<OverviewUiState> {
        val month = selection.month
        val day = selection.day
        val facts = combine(
            stats.observeDaily(month),
            stats.observePeriod(month.startInclusive(), month.endExclusive()),
            stats.observeDay(day),
        ) { daily, period, dayBills ->
            OverviewFacts(daily, period, dayBills)
        }
        return combine(
            facts,
            categories.observeAll(),
            preferences.preferences,
        ) { fact, categoryRows, prefs ->
            val (balance, overflow) = balanceOf(fact.period.incomeAmount, fact.period.expenseAmount)
            OverviewUiState(
                loading = false,
                month = month,
                selectedDay = day,
                canGoNext = month < YearMonth.now(),
                currencySymbol = prefs.currency.symbol,
                income = fact.period.incomeAmount,
                expense = fact.period.expenseAmount,
                balance = balance,
                balanceOverflow = overflow,
                days = fact.daily.mapNotNull { summary -> summary.toDayAmount() },
                bills = billsToRows(fact.dayBills, categoryRows.associateBy { it.id }),
            )
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("OverviewViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                OverviewViewModel(
                    stats = entry.statsRepository(),
                    categories = entry.categoryRepository(),
                    preferences = AppPreferencesRepository.create(application),
                )
            }
        }
    }
}

private data class OverviewSelection(
    val month: YearMonth,
    val day: LocalDate,
)

private data class OverviewFacts(
    val daily: List<DayAmountSummary>,
    val period: PeriodAmountSummary,
    val dayBills: List<Bill>,
)

private fun DayAmountSummary.toDayAmount(): OverviewDayAmount? {
    val date = try {
        LocalDate.parse(day)
    } catch (_: DateTimeParseException) {
        return null
    }
    return OverviewDayAmount(
        day = date,
        expense = expenseAmount,
        income = incomeAmount,
    )
}
