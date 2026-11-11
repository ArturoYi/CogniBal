package com.cyr.cognobal.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

private val DaySurface = Color(0xFFFFFFFF)
private val DayInk = Color(0xFF18191E)
private val DayMute = Color(0xFF5A6070)
private val DayLine = Color(0xFFECEEF2)
private val DayBrandTint = Color(0xFFF6F7F9)
private val DayBrand = Color(0xFF1F2026)

private val NightSurface = Color(0xFF181B22)
private val NightInk = Color(0xFFF4F6FA)
private val NightMute = Color(0xFFB4BCCB)
private val NightLine = Color(0xFF252A36)
private val NightFill = Color(0xFF222732)
private val NightBrand = Color(0xFFE0B47D)

/**
 * 桌面微件同步应用「纯净白卡 + 曜石墨色 + 香槟金」设计语言。
 */
@Composable
internal fun LedgerGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(light = lightScheme(), dark = darkScheme()),
        content = content,
    )
}

private fun lightScheme() = lightColorScheme(
    primary = DayBrand,
    onPrimary = DaySurface,
    primaryContainer = DayBrandTint,
    onPrimaryContainer = DayInk,
    secondary = DayBrandTint,
    onSecondary = DayInk,
    secondaryContainer = DaySurface,
    onSecondaryContainer = DayInk,
    tertiary = DayMute,
    onTertiary = DaySurface,
    tertiaryContainer = DayBrandTint,
    onTertiaryContainer = DayInk,
    background = DaySurface,
    onBackground = DayInk,
    surface = DaySurface,
    onSurface = DayInk,
    surfaceVariant = DayBrandTint,
    onSurfaceVariant = DayMute,
    outline = DayLine,
    error = DayInk,
    onError = DaySurface,
    errorContainer = DayBrandTint,
    onErrorContainer = DayInk,
    inverseSurface = DayInk,
    inverseOnSurface = DaySurface,
    inversePrimary = DayBrand,
)

private fun darkScheme() = darkColorScheme(
    primary = NightBrand,
    onPrimary = NightSurface,
    primaryContainer = NightFill,
    onPrimaryContainer = NightBrand,
    secondary = NightFill,
    onSecondary = NightInk,
    secondaryContainer = NightSurface,
    onSecondaryContainer = NightInk,
    tertiary = NightMute,
    onTertiary = NightSurface,
    tertiaryContainer = NightFill,
    onTertiaryContainer = NightInk,
    background = NightSurface,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightFill,
    onSurfaceVariant = NightMute,
    outline = NightLine,
    error = NightInk,
    onError = NightSurface,
    errorContainer = NightFill,
    onErrorContainer = NightInk,
    inverseSurface = NightInk,
    inverseOnSurface = NightSurface,
    inversePrimary = NightSurface,
)
