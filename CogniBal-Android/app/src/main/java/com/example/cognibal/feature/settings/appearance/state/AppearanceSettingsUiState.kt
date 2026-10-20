package com.example.cognibal.screen.feature.settings.appearance.state

import com.example.cognibal.screen.feature.settings.model.DarkModePreference
import com.example.cognibal.ui.theme.AppThemeStyle

/** 选中的语言。`languageTag` 为空表示跟随系统。 */
internal data class LanguageChoice(
    val languageTag: String?,
)

internal data class AppearanceSettingsUiState(
    val showLanguageDialog: Boolean = false,
    val languageConfirmation: LanguageChoice? = null,
    val languageSwitch: LanguageChoice? = null,
    val darkMode: DarkModePreference = DarkModePreference.DARK,
    val themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
)

/** 界面向上递交的操作。语言写入应用内区域，主题写入仓库。 */
internal sealed interface AppearanceSettingsUiEvent {
    data object LanguageClicked : AppearanceSettingsUiEvent
    data object DismissLanguageDialog : AppearanceSettingsUiEvent

    /** `null` 表示跟随系统。先进入确认，不立刻写语言。 */
    data class LanguageSelected(val languageTag: String?) : AppearanceSettingsUiEvent
    data object DismissLanguageConfirmation : AppearanceSettingsUiEvent
    data object ConfirmLanguageSwitch : AppearanceSettingsUiEvent

    /** 盖层已经不透明，并且语言弹窗已经关掉之后，再写入语言。 */
    data object ApplyLanguageSwitch : AppearanceSettingsUiEvent
    data object LanguageSwitchFinished : AppearanceSettingsUiEvent
    data class DarkModeChanged(val mode: DarkModePreference) : AppearanceSettingsUiEvent
    data class ThemeStyleChanged(val style: AppThemeStyle) : AppearanceSettingsUiEvent
}
