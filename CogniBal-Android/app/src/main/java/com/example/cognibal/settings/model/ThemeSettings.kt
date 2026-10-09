package com.example.cognibal.settings.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.cognibal.ui.theme.AppThemeStyle

/**
 * 暗色模式偏好枚举
 *
 * - [FOLLOW_SYSTEM]: 跟随系统外观（默认）
 * - [LIGHT]: 强制使用浅色（亮色）外观
 * - [DARK]: 强制使用深色（暗色）外观
 */
enum class DarkModePreference(
    val id: String,
    val title: String,
    val subtitle: String,
) {
    FOLLOW_SYSTEM(
        id = "system",
        title = "跟随系统",
        subtitle = "随系统深浅外观自动切换",
    ),
    LIGHT(
        id = "light",
        title = "浅色模式",
        subtitle = "清爽明亮，适合白天阅读",
    ),
    DARK(
        id = "dark",
        title = "暗色模式",
        subtitle = "温润暗沉，夜间更加护眼",
    );

    companion object {
        fun fromId(id: String?): DarkModePreference {
            return entries.firstOrNull { it.id == id } ?: FOLLOW_SYSTEM
        }
    }
}

/**
 * 货币单位偏好
 */
enum class CurrencyPreference(
    val symbol: String,
    val displayName: String,
) {
    CNY("¥", "人民币 (¥)"),
    USD("$", "美元 ($)"),
    EUR("€", "欧元 (€)"),
    GBP("£", "英镑 (£)"),
}
