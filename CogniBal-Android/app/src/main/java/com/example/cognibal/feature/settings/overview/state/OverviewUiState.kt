package com.example.cognibal.screen.feature.settings.overview.state

import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import java.time.LocalDate
import java.time.YearMonth

internal data class OverviewDayAmount(
    val day: LocalDate,
    val expense: Long,
    val income: Long,
)

internal data class OverviewUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate = LocalDate.now(),
    val canGoNext: Boolean = false,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val income: Long = 0L,
    val expense: Long = 0L,
    val balance: Long = 0L,
    val balanceOverflow: Boolean = false,
    val days: List<OverviewDayAmount> = emptyList(),
    val bills: List<BillRowUi> = emptyList(),
)

internal sealed interface OverviewUiEvent {
    data object PreviousMonth : OverviewUiEvent
    data object NextMonth : OverviewUiEvent
    data class SelectDay(val day: LocalDate) : OverviewUiEvent
}
