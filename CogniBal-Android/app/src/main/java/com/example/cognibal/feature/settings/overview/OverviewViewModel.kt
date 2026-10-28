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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import com.example.cognibal.screen.feature.settings.model.AppPreferences
import com.example.cognibal.screen.feature.settings.overview.state.OverviewCategoryOption
import com.example.cognibal.screen.feature.settings.overview.state.OverviewDayAmount
import com.example.cognibal.screen.feature.settings.overview.state.OverviewKind
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiEvent
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiState
import com.example.cognibal.screen.feature.settings.overview.state.matches

/**
 * 总览页。一个月的日历，点某一天看当天账单。
 *
 * 格子怎么排跟语言有关，留给界面。这里只给有账单的日期和选中那天的流水。
 * 筛选只作用在当天流水上，月汇总和日历圆点保持整月。
 * 点完成或清除才改筛选。当天流水多时，整理结果放到默认调度器，避免把界面卡住。
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

    val uiState: StateFlow<OverviewUiState> = combine(
        selection.flatMapLatest(::observeFacts),
        categories.observeAll(),
        preferences.preferences,
        dayFilter,
    ) { fact, categoryRows, prefs, filter ->
        // 一天的流水可能很多。放到默认调度器上整理，主线程继续转按钮上的等待。
        withContext(Dispatchers.Default) {
            overviewState(fact, categoryRows, prefs, filter)
        }
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
            is OverviewUiEvent.ApplyFilter -> dayFilter.value = DayFilter(
                kind = event.kind,
                categoryIds = event.categoryIds,
            )
            OverviewUiEvent.ClearFilter -> dayFilter.value = DayFilter()
        }
    }

    private fun shiftMonth(delta: Long) {
        selection.update { current ->
            monthSelection(current.month.plusMonths(delta), current.day)
        }
    }

    private fun observeFacts(selection: OverviewSelection): Flow<OverviewFacts> {
        val month = selection.month
        val day = selection.day
        return combine(
            stats.observeDaily(month),
            stats.observePeriod(month.startInclusive(), month.endExclusive()),
            stats.observeDay(day),
        ) { daily, period, dayBills ->
            OverviewFacts(
                month = month,
                day = day,
                daily = daily,
                period = period,
                dayBills = dayBills,
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

private fun overviewState(
    fact: OverviewFacts,
    categoryRows: List<Category>,
    prefs: AppPreferences,
    filter: DayFilter,
): OverviewUiState {
    val (balance, overflow) = balanceOf(fact.period.incomeAmount, fact.period.expenseAmount)
    val categoryOptions = categoryRows
        .sortedWith(compareBy<Category>({ it.type.code }, { it.sort }, { it.id }))
        .map { category ->
            OverviewCategoryOption(
                id = category.id,
                name = category.name,
                expense = category.type == CategoryType.Expense,
            )
        }
    val visibleIds = categoryOptions
        .filter { option -> option.matches(filter.kind) }
        .map { option -> option.id }
        .toSet()
    val selectedIds = filter.categoryIds.intersect(visibleIds)
    val filteredBills = fact.dayBills.filter { bill ->
        bill.matches(filter.kind) && (selectedIds.isEmpty() || bill.categoryId in selectedIds)
    }
    return OverviewUiState(
        loading = false,
        month = fact.month,
        selectedDay = fact.day,
        canGoNext = fact.month < YearMonth.now(),
        currencySymbol = prefs.currency.symbol,
        income = fact.period.incomeAmount,
        expense = fact.period.expenseAmount,
        balance = balance,
        balanceOverflow = overflow,
        days = fact.daily.mapNotNull { summary -> summary.toDayAmount() },
        bills = billsToRows(filteredBills, categoryRows.associateBy { it.id }),
        dayHasBills = fact.dayBills.isNotEmpty(),
        kind = filter.kind,
        categories = categoryOptions,
        selectedCategoryIds = selectedIds,
        filterActive = filter.kind != OverviewKind.All || selectedIds.isNotEmpty(),
    )
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
    val month: YearMonth,
    val day: LocalDate,
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
