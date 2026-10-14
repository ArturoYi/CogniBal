package com.example.cognibal.screen.settings.viewmodel

import com.example.cognibal.screen.settings.model.AppPreferences
import com.example.cognibal.screen.settings.model.CurrencyPreference

/**
 * 设置列表一次性渲染所需的状态。偏好来自 DataStore，弹窗和缓存文案只存在于 ViewModel。
 * 二级页有自己的 ViewModel，不放在这里。
 */
internal data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val cacheSizeText: String = DEFAULT_CACHE_SIZE,
    val daysInUse: Int = 18,
    val totalRecordsCount: Int = 42,
    val totalSurplus: Double = 12850.00,
    val isSurplusVisible: Boolean = false,
    val showMoreDialog: Boolean = false,
    val showCurrencyDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
)

/** 设置列表向上递交的操作。外观页的主题不经过这里。 */
internal sealed interface SettingsUiEvent {
    data class HapticFeedbackChanged(val enabled: Boolean) : SettingsUiEvent
    data class CurrencySelected(val currency: CurrencyPreference) : SettingsUiEvent

    data object ToggleSurplusVisibility : SettingsUiEvent
    data object OpenMoreDialog : SettingsUiEvent
    data object DismissMoreDialog : SettingsUiEvent
    data object OpenCurrencyDialog : SettingsUiEvent
    data object DismissCurrencyDialog : SettingsUiEvent
    data object OpenClearCacheDialog : SettingsUiEvent
    data object DismissClearCacheDialog : SettingsUiEvent
    data object ConfirmClearCache : SettingsUiEvent
    data object OpenAboutDialog : SettingsUiEvent
    data object DismissAboutDialog : SettingsUiEvent
}

internal const val DEFAULT_CACHE_SIZE = "12.6 MB"
