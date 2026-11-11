package com.cyr.cognobal.screen.feature.settings.model

import com.cyr.cognobal.ui.theme.AppThemeStyle

/**
 * 应用偏好的完整快照。
 *
 * DataStore 里仍是分散的键值；Repository 读出后收成这一份，界面和 ViewModel 只接触它。
 * 默认值就是首次安装、文件损坏或键缺失时的表现：柔和蓝，暗色。
 */
data class AppPreferences(
    val themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
    val darkMode: DarkModePreference = DarkModePreference.LIGHT,
    val currency: CurrencyPreference = CurrencyPreference.CNY,
    /** 接近或超过预算时是否推送通知。默认关闭。 */
    val budgetNotify: Boolean = false,
    /** 记账首页是否显示预算。默认显示。 */
    val budgetHomeVisible: Boolean = true,
    /**
     * 退到后台时用深色盖住界面。回到前台自己揭开。
     * 默认打开，避免账本出现在最近任务里。
     */
    val hideContentInBackground: Boolean = true,
    /** 已同意的协议版本。空字符串表示还没同意当前文本。 */
    val acceptedLegalVersion: String = "",
) {
    /** 把“跟随系统 / 强制浅色 / 强制暗色”收成主题实际要用的深浅。 */
    fun useDarkTheme(isSystemInDarkTheme: Boolean): Boolean = when (darkMode) {
        DarkModePreference.FOLLOW_SYSTEM -> isSystemInDarkTheme
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
}
