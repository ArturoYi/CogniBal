package com.example.cognibal.screen.feature.settings.reports.state

import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import java.time.YearMonth

internal enum class ReportSpan(val monthCount: Int) {
    SixMonths(6),
    TwelveMonths(12),
}

internal data class ReportMonthUi(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
    val balance: Long,
    val balanceOverflow: Boolean,
)

internal data class ReportsUiState(
    val loading: Boolean = true,
    val span: ReportSpan = ReportSpan.SixMonths,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val months: List<ReportMonthUi> = emptyList(),
)

internal sealed interface ReportsUiEvent {
    data class SelectSpan(val span: ReportSpan) : ReportsUiEvent
}
