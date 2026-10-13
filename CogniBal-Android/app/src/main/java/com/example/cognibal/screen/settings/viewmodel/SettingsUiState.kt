package com.example.cognibal.screen.settings.viewmodel

import com.example.cognibal.screen.settings.model.AppPreferences
import com.example.cognibal.screen.settings.model.CurrencyPreference
import com.example.cognibal.screen.settings.model.DarkModePreference
import com.example.cognibal.ui.theme.AppThemeStyle

/**
 * 设置页一次性渲染所需的状态。偏好来自 DataStore，弹窗和缓存文案只存在于 ViewModel。
 */
internal data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val cacheSizeText: String = DEFAULT_CACHE_SIZE,
    val daysInUse: Int = 18,
    val totalRecordsCount: Int = 42,
    val totalSurplus: Double = 12850.00,
    val isSurplusVisible: Boolean = false,
    val showAppearanceDialog: Boolean = false,
    val showBackupDialog: Boolean = false,
    val showSecurityDialog: Boolean = false,
    val showMoreDialog: Boolean = false,
    val showCurrencyDialog: Boolean = false,
    val showClearCacheDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
)

/** 界面向上递交的操作。ViewModel 决定改状态还是写仓库。 */
internal sealed interface SettingsUiEvent {
    data class DarkModeChanged(val mode: DarkModePreference) : SettingsUiEvent
    data class ThemeStyleChanged(val style: AppThemeStyle) : SettingsUiEvent
    data class HapticFeedbackChanged(val enabled: Boolean) : SettingsUiEvent
    data class CurrencySelected(val currency: CurrencyPreference) : SettingsUiEvent

    data object ToggleSurplusVisibility : SettingsUiEvent
    data object OpenAppearanceDialog : SettingsUiEvent
    data object DismissAppearanceDialog : SettingsUiEvent
    data object OpenBackupDialog : SettingsUiEvent
    data object DismissBackupDialog : SettingsUiEvent
    data object OpenSecurityDialog : SettingsUiEvent
    data object DismissSecurityDialog : SettingsUiEvent
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
