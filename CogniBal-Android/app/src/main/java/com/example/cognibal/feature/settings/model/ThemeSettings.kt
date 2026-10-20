package com.example.cognibal.screen.feature.settings.model

import androidx.annotation.StringRes
import com.example.cognibal.R

/**
 * 暗色模式偏好枚举
 *
 * - [FOLLOW_SYSTEM]: 跟随系统外观
 * - [LIGHT]: 强制使用浅色（亮色）外观
 * - [DARK]: 强制使用深色（暗色）外观。未保存过偏好时用这一项
 *
 * 文案是字符串资源，切换语言后由界面重新读取。
 */
enum class DarkModePreference(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
) {
    FOLLOW_SYSTEM(
        id = "system",
        titleRes = R.string.theme_follow_system,
        subtitleRes = R.string.theme_follow_system_subtitle,
    ),
    LIGHT(
        id = "light",
        titleRes = R.string.theme_light,
        subtitleRes = R.string.theme_light_subtitle,
    ),
    DARK(
        id = "dark",
        titleRes = R.string.theme_dark,
        subtitleRes = R.string.theme_dark_subtitle,
    );

    companion object {
        fun fromId(id: String?): DarkModePreference {
            return entries.firstOrNull { it.id == id } ?: DARK
        }
    }
}

/**
 * 货币单位偏好
 */
enum class CurrencyPreference(
    val symbol: String,
    @StringRes val nameRes: Int,
) {
    CNY("¥", R.string.currency_cny),
    USD("$", R.string.currency_usd),
    EUR("€", R.string.currency_eur),
    GBP("£", R.string.currency_gbp),
}
