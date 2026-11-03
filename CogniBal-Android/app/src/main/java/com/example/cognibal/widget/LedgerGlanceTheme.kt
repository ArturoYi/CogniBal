package com.example.cognibal.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF111111)
private val MuteLight = Color(0xFF666666)
private val LineLight = Color(0xFFE5E5E5)
private val FillLight = Color(0xFFF3F3F3)

private val Night = Color(0xFF111111)
private val NightInk = Color(0xFFFFFFFF)
private val MuteNight = Color(0xFFA3A3A3)
private val LineNight = Color(0xFF2C2C2C)
private val FillNight = Color(0xFF242424)

/**
 * 桌面卡片只用黑白，深浅跟系统走。浅色是白底黑字，夜间是黑底白字。
 * 卡片底色用 background，系统会按当前深浅选用对应的一档。
 */
@Composable
internal fun LedgerGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(
            light = lightScheme(),
            dark = darkScheme(),
        ),
        content = content,
    )
}

private fun lightScheme() = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = FillLight,
    onPrimaryContainer = Ink,
    secondary = FillLight,
    onSecondary = Ink,
    secondaryContainer = Paper,
    onSecondaryContainer = Ink,
    tertiary = MuteLight,
    onTertiary = Paper,
    tertiaryContainer = FillLight,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = FillLight,
    onSurfaceVariant = MuteLight,
    outline = LineLight,
    error = Ink,
    onError = Paper,
    errorContainer = FillLight,
    onErrorContainer = Ink,
    inverseSurface = Night,
    inverseOnSurface = NightInk,
    inversePrimary = NightInk,
)

private fun darkScheme() = darkColorScheme(
    primary = NightInk,
    onPrimary = Night,
    primaryContainer = FillNight,
    onPrimaryContainer = NightInk,
    secondary = FillNight,
    onSecondary = NightInk,
    secondaryContainer = Night,
    onSecondaryContainer = NightInk,
    tertiary = MuteNight,
    onTertiary = Night,
    tertiaryContainer = FillNight,
    onTertiaryContainer = NightInk,
    background = Night,
    onBackground = NightInk,
    surface = Night,
    onSurface = NightInk,
    surfaceVariant = FillNight,
    onSurfaceVariant = MuteNight,
    outline = LineNight,
    error = NightInk,
    onError = Night,
    errorContainer = FillNight,
    onErrorContainer = NightInk,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    inversePrimary = Ink,
)
