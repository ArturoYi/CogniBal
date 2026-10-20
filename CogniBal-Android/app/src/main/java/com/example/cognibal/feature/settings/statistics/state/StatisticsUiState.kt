package com.example.cognibal.screen.feature.settings.statistics.state

import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import java.time.YearMonth

internal enum class StatisticsKind {
    Expense,
    Income,
}

internal data class CategoryShareUi(
    val id: Long,
    val name: String?,
    val missing: Boolean,
    val iconKey: String,
    val colorKey: String,
    val amount: Long,
    val ratio: Float,
)

internal data class StatisticsUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val canGoNext: Boolean = false,
    val kind: StatisticsKind = StatisticsKind.Expense,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val total: Long = 0L,
    val categories: List<CategoryShareUi> = emptyList(),
)

internal sealed interface StatisticsUiEvent {
    data object PreviousMonth : StatisticsUiEvent
    data object NextMonth : StatisticsUiEvent
    data object ShowExpense : StatisticsUiEvent
    data object ShowIncome : StatisticsUiEvent
}
