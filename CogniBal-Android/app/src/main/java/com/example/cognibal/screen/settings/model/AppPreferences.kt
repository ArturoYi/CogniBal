package com.example.cognibal.screen.settings.model

import com.example.cognibal.ui.theme.AppThemeStyle

/**
 * 应用偏好的完整快照。
 *
 * DataStore 里仍是分散的键值；Repository 读出后收成这一份，界面和 ViewModel 只接触它。
 * 默认值就是首次安装、文件损坏或键缺失时的表现：暖奶油青绿，暗色。
 */
data class AppPreferences(
    val themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
    val darkMode: DarkModePreference = DarkModePreference.DARK,
    val hapticFeedback: Boolean = true,
    val currency: CurrencyPreference = CurrencyPreference.CNY,
) {
    /** 把“跟随系统 / 强制浅色 / 强制暗色”收成主题实际要用的深浅。 */
    fun useDarkTheme(isSystemInDarkTheme: Boolean): Boolean = when (darkMode) {
        DarkModePreference.FOLLOW_SYSTEM -> isSystemInDarkTheme
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
}
