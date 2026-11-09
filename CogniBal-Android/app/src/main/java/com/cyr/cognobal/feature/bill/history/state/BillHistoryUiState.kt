package com.cyr.cognobal.screen.feature.bill.history.state

import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference
import com.cyr.cognobal.screen.feature.bill.state.BillRowUi

internal data class BillHistoryUiState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val bills: List<BillRowUi> = emptyList(),
)
