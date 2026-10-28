package com.example.cognibal.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.example.cognibal.ui.theme.MonochromeDark_OnPrimary
import com.example.cognibal.ui.theme.MonochromeDark_OnSecondary
import com.example.cognibal.ui.theme.MonochromeDark_Primary
import com.example.cognibal.ui.theme.MonochromeDark_Secondary
import com.example.cognibal.ui.theme.MonochromeLight_OnPrimary
import com.example.cognibal.ui.theme.MonochromeLight_OnSecondary
import com.example.cognibal.ui.theme.MonochromeLight_Primary
import com.example.cognibal.ui.theme.MonochromeLight_Secondary
import com.example.cognibal.ui.theme.WarmCreamDark_CardBackground
import com.example.cognibal.ui.theme.WarmCreamDark_Divider
import com.example.cognibal.ui.theme.WarmCreamDark_Expense
import com.example.cognibal.ui.theme.WarmCreamDark_OnPrimary
import com.example.cognibal.ui.theme.WarmCreamDark_OnSecondary
import com.example.cognibal.ui.theme.WarmCreamDark_Primary
import com.example.cognibal.ui.theme.WarmCreamDark_Secondary
import com.example.cognibal.ui.theme.WarmCreamDark_TextPrimary
import com.example.cognibal.ui.theme.WarmCreamDark_TextSecondary
import com.example.cognibal.ui.theme.WarmCreamLight_CardBackground
import com.example.cognibal.ui.theme.WarmCreamLight_Divider
import com.example.cognibal.ui.theme.WarmCreamLight_Expense
import com.example.cognibal.ui.theme.WarmCreamLight_OnPrimary
import com.example.cognibal.ui.theme.WarmCreamLight_OnSecondary
import com.example.cognibal.ui.theme.WarmCreamLight_Primary
import com.example.cognibal.ui.theme.WarmCreamLight_Secondary
import com.example.cognibal.ui.theme.WarmCreamLight_TextPrimary
import com.example.cognibal.ui.theme.WarmCreamLight_TextSecondary
import com.example.cognibal.ui.theme.budgetNearColor

/**
 * 小组件跟应用同一套浅色卡片和主色，深浅跟系统走。
 *
 * 不用动态取色，避免桌面颜色和账本里的柔和蓝、墨色对不上。
 */
@Composable
internal fun LedgerGlanceTheme(
    monochrome: Boolean,
    content: @Composable () -> Unit,
) {
    GlanceTheme(
        colors = ColorProviders(
            light = lightScheme(monochrome),
            dark = darkScheme(monochrome),
        ),
        content = content,
    )
}

private fun lightScheme(monochrome: Boolean) = lightColorScheme(
    primary = if (monochrome) MonochromeLight_Primary else WarmCreamLight_Primary,
    onPrimary = if (monochrome) MonochromeLight_OnPrimary else WarmCreamLight_OnPrimary,
    secondary = if (monochrome) MonochromeLight_Secondary else WarmCreamLight_Secondary,
    onSecondary = if (monochrome) MonochromeLight_OnSecondary else WarmCreamLight_OnSecondary,
    tertiary = budgetNearColor(isDark = false),
    background = WarmCreamLight_CardBackground,
    surface = WarmCreamLight_CardBackground,
    surfaceVariant = WarmCreamLight_Secondary,
    onBackground = WarmCreamLight_TextPrimary,
    onSurface = WarmCreamLight_TextPrimary,
    onSurfaceVariant = WarmCreamLight_TextSecondary,
    outline = WarmCreamLight_Divider,
    error = WarmCreamLight_Expense,
)

private fun darkScheme(monochrome: Boolean) = darkColorScheme(
    primary = if (monochrome) MonochromeDark_Primary else WarmCreamDark_Primary,
    onPrimary = if (monochrome) MonochromeDark_OnPrimary else WarmCreamDark_OnPrimary,
    secondary = if (monochrome) MonochromeDark_Secondary else WarmCreamDark_Secondary,
    onSecondary = if (monochrome) MonochromeDark_OnSecondary else WarmCreamDark_OnSecondary,
    tertiary = budgetNearColor(isDark = true),
    background = WarmCreamDark_CardBackground,
    surface = WarmCreamDark_CardBackground,
    surfaceVariant = WarmCreamDark_Secondary,
    onBackground = WarmCreamDark_TextPrimary,
    onSurface = WarmCreamDark_TextPrimary,
    onSurfaceVariant = WarmCreamDark_TextSecondary,
    outline = WarmCreamDark_Divider,
    error = WarmCreamDark_Expense,
)
