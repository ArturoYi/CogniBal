package com.cyr.cognobal.widget

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders

private val NightSurface = Color(0xFF181B22)
private val NightInk = Color(0xFFF4F6FA)
private val NightMute = Color(0xFFB4BCCB)
private val NightLine = Color(0xFF252A36)
private val NightFill = Color(0xFF222732)
private val NightBrand = Color(0xFFE0B47D)

/**
 * 桌面卡片固定为暗色：曜石底、浅色字、香槟金强调。
 * 亮色和暗色槽位用同一套色，不跟系统日夜切换。
 */
@Composable
internal fun LedgerGlanceTheme(content: @Composable () -> Unit) {
    val colors = darkScheme()
    GlanceTheme(
        colors = ColorProviders(light = colors, dark = colors),
        content = content,
    )
}

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
