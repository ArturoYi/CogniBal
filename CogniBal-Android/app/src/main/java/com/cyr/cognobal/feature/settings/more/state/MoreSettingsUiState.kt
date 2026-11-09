package com.cyr.cognobal.screen.feature.settings.more.state

import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference

/** 换算没有完成时的原因。账单金额要么全部改完，要么一次都没提交。 */
internal enum class CurrencySwitchFailure {
    Overflow,
    TimedOut,
    Failed,
    Preference,
}

/** 更多设置页：默认货币。 */
internal data class MoreSettingsUiState(
    val currency: CurrencyPreference = CurrencyPreference.CNY,
    val showCurrencyDialog: Boolean = false,
    val rateTarget: CurrencyPreference? = null,
    val rateText: String = "",
    val rateReady: Boolean = false,
    val converting: Boolean = false,
    val failure: CurrencySwitchFailure? = null,
)

internal sealed interface MoreSettingsUiEvent {
    data object OpenCurrencyDialog : MoreSettingsUiEvent
    data object DismissCurrencyDialog : MoreSettingsUiEvent
    data class CurrencySelected(val currency: CurrencyPreference) : MoreSettingsUiEvent
    data class RateTextChanged(val text: String) : MoreSettingsUiEvent
    data object DismissRateDialog : MoreSettingsUiEvent
    data object ConfirmRate : MoreSettingsUiEvent
    data object DismissCurrencyFailure : MoreSettingsUiEvent
    data object RetryCurrencySwitch : MoreSettingsUiEvent
}
