package com.example.cognibal.screen.feature.bill.history.state

import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.screen.feature.bill.state.BillRowUi

internal data class BillHistoryUiState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val bills: List<BillRowUi> = emptyList(),
)
