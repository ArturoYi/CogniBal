package com.example.cognibal.screen.feature.settings.more.state

import com.example.cognibal.screen.feature.settings.model.CurrencyPreference

internal const val DEFAULT_CACHE_SIZE = "12.6 MB"

/** 换算没有完成时的原因。账单金额要么全部改完，要么一次都没提交。 */
internal enum class CurrencySwitchFailure {
    Overflow,
    TimedOut,
    Failed,
    Preference,
}

/** 更多设置页：默认货币和临时缓存。 */
internal data class MoreSettingsUiState(
    val currency: CurrencyPreference = CurrencyPreference.CNY,
    val cacheSizeText: String = DEFAULT_CACHE_SIZE,
    val showCurrencyDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
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
    data object OpenClearCacheDialog : MoreSettingsUiEvent
    data object DismissClearCacheDialog : MoreSettingsUiEvent
    data object ConfirmClearCache : MoreSettingsUiEvent
}
