package com.cyr.cognobal.screen.feature.bill.history.state

import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference
import com.cyr.cognobal.screen.feature.bill.state.BillRowUi
import java.time.LocalDate

internal data class BillHistoryUiState(
    val loading: Boolean = true,
    val currencySymbol: String = CurrencyPreference.CNY.symbol,
    val bills: List<BillRowUi> = emptyList(),
    /** 用来把今天的日期写成「今天」。午夜后会换。 */
    val today: LocalDate = LocalDate.now(),
)
