package com.example.cognibal.screen.feature.settings.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.data.database.entity.Category
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.database.query.CategoryAmountSummary
import com.example.cognibal.data.preferences.AppPreferencesRepository
import com.example.cognibal.data.repository.CategoryRepository
import com.example.cognibal.data.repository.StatsRepository
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.tool.cappedToCurrent
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
import kotlinx.coroutines.flow.update
import com.example.cognibal.screen.feature.settings.statistics.state.CategoryShareUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsKind
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsUiEvent
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsUiState

/**
 * 统计页。看一个月里支出或收入的分类构成。
 *
 * 月份和收支类型留在这里。金额来自账单汇总，分类名来自分类表。
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
        val start = currentMonth.startInclusive()
        val end = currentMonth.endExclusive()
        combine(
            stats.observePeriod(start, end),
            stats.observeCategoryBreakdown(currentKind.toBillType(), start, end),
            categories.observeAll(),
            preferences.preferences,
        ) { period, breakdown, categoryRows, prefs ->
            val total = when (currentKind) {
                StatisticsKind.Expense -> period.expenseAmount
                StatisticsKind.Income -> period.incomeAmount
            }
            StatisticsUiState(
                loading = false,
                month = currentMonth,
                canGoNext = currentMonth < YearMonth.now(),
                kind = currentKind,
                currencySymbol = prefs.currency.symbol,
                total = total,
                categories = categoryShares(breakdown, categoryRows, total),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = StatisticsUiState(),
    )

    fun onEvent(event: StatisticsUiEvent) {
        when (event) {
            StatisticsUiEvent.PreviousMonth -> month.update { it.minusMonths(1) }
            StatisticsUiEvent.NextMonth -> month.update { it.plusMonths(1).cappedToCurrent() }
            StatisticsUiEvent.ShowExpense -> kind.value = StatisticsKind.Expense
            StatisticsUiEvent.ShowIncome -> kind.value = StatisticsKind.Income
        }
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

private fun StatisticsKind.toBillType(): BillType = when (this) {
    StatisticsKind.Expense -> BillType.Expense
    StatisticsKind.Income -> BillType.Income
}

private fun categoryShares(
    rows: List<CategoryAmountSummary>,
    categories: List<Category>,
    total: Long,
): List<CategoryShareUi> {
    val byId = categories.associateBy { it.id }
    return rows
        .filter { row -> row.amount > 0L }
        .sortedByDescending { row -> row.amount }
        .map { row ->
            val category = byId[row.categoryId]
            val ratio = if (total <= 0L) {
                0f
            } else {
                (row.amount.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
            }
            CategoryShareUi(
                id = row.categoryId,
                name = category?.name,
                missing = category == null,
                iconKey = category?.icon ?: "",
                colorKey = category?.color ?: "#FF8A8A8A",
                amount = row.amount,
                ratio = ratio,
            )
        }
}
