package com.example.cognibal.screen.feature.settings.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.model.CategoryType
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
import com.example.cognibal.screen.feature.settings.overview.state.OverviewCategoryOption
import com.example.cognibal.screen.feature.settings.overview.state.OverviewDayAmount
import com.example.cognibal.screen.feature.settings.overview.state.OverviewKind
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiEvent
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiState

/**
 * 总览页。一个月的日历，点某一天看当天账单。
 *
 * 格子怎么排跟语言有关，留给界面。这里只给有账单的日期和选中那天的流水。
 * 筛选只作用在当天流水上，月汇总和日历圆点保持整月。
 */
internal class OverviewViewModel(
    private val stats: StatsRepository,
    private val categories: CategoryRepository,
    private val preferences: AppPreferencesRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(
        OverviewSelection(month = YearMonth.now(), day = LocalDate.now()),
    )
    private val dayFilter = MutableStateFlow(DayFilter())

    val uiState: StateFlow<OverviewUiState> = combine(selection, dayFilter) { current, filter ->
        current to filter
    }.flatMapLatest { (current, filter) ->
        observe(current, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OverviewUiState(),
    )

    fun onEvent(event: OverviewUiEvent) {
        when (event) {
            OverviewUiEvent.PreviousMonth -> shiftMonth(-1)
            OverviewUiEvent.NextMonth -> shiftMonth(1)
            is OverviewUiEvent.SelectMonth -> selection.update { current ->
                monthSelection(event.month, current.day)
            }
            is OverviewUiEvent.SelectDay -> selection.update { current ->
                if (YearMonth.from(event.day) != current.month) current else current.copy(day = event.day)
            }
            is OverviewUiEvent.SetKind -> dayFilter.update { it.copy(kind = event.kind) }
            is OverviewUiEvent.ToggleCategory -> dayFilter.update { current ->
                val ids = if (event.categoryId in current.categoryIds) {
                    current.categoryIds - event.categoryId
                } else {
                    current.categoryIds + event.categoryId
                }
                current.copy(categoryIds = ids)
            }
            OverviewUiEvent.ClearFilter -> dayFilter.value = DayFilter()
        }
    }

    private fun shiftMonth(delta: Long) {
        selection.update { current ->
            monthSelection(current.month.plusMonths(delta), current.day)
        }
    }

    private fun observe(selection: OverviewSelection, filter: DayFilter): Flow<OverviewUiState> {
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
            val visibleCategories = categoryRows
                .filter { category -> category.matches(filter.kind) }
                .sortedWith(compareBy<Category>({ it.type.code }, { it.sort }, { it.id }))
                .map { category ->
                    OverviewCategoryOption(
                        id = category.id,
                        name = category.name,
                        expense = category.type == CategoryType.Expense,
                    )
                }
            val selectedIds = filter.categoryIds.intersect(visibleCategories.map { it.id }.toSet())
            val filteredBills = fact.dayBills.filter { bill ->
                bill.matches(filter.kind) && (selectedIds.isEmpty() || bill.categoryId in selectedIds)
            }
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
                bills = billsToRows(filteredBills, categoryRows.associateBy { it.id }),
                dayHasBills = fact.dayBills.isNotEmpty(),
                kind = filter.kind,
                categories = visibleCategories,
                selectedCategoryIds = selectedIds,
                filterActive = filter.kind != OverviewKind.All || selectedIds.isNotEmpty(),
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

private fun monthSelection(month: YearMonth, day: LocalDate): OverviewSelection {
    val safeMonth = month.cappedToCurrent()
    val safeDay = safeMonth.atDay(day.dayOfMonth.coerceAtMost(safeMonth.lengthOfMonth()))
    return OverviewSelection(month = safeMonth, day = safeDay)
}

private data class DayFilter(
    val kind: OverviewKind = OverviewKind.All,
    val categoryIds: Set<Long> = emptySet(),
)

private fun Category.matches(kind: OverviewKind): Boolean = when (kind) {
    OverviewKind.All -> true
    OverviewKind.Expense -> type == CategoryType.Expense
    OverviewKind.Income -> type == CategoryType.Income
}

private fun Bill.matches(kind: OverviewKind): Boolean = when (kind) {
    OverviewKind.All -> true
    OverviewKind.Expense -> type == BillType.Expense
    OverviewKind.Income -> type == BillType.Income
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
