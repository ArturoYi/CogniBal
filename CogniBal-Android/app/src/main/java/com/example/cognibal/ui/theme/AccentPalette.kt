package com.example.cognibal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 可切换的主色。页面底仍是中性灰，主题色只进按钮、选中态和图表。
 * 浅色用压深后仍能读的一档，夜间用更亮的一档，避免亮色写在浅底上。
 */
internal data class AccentInk(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
)

internal fun AppThemeStyle.accentInk(isDark: Boolean): AccentInk = when (this) {
    AppThemeStyle.WarmCream,
    AppThemeStyle.Monochrome,
    -> if (isDark) MonoDark else MonoLight
    AppThemeStyle.Vitality -> if (isDark) VitalityDark else VitalityLight
    AppThemeStyle.Neon -> if (isDark) NeonDark else NeonLight
    AppThemeStyle.Lime -> if (isDark) LimeDark else LimeLight
    AppThemeStyle.Cream -> if (isDark) CreamDark else CreamLight
}

internal fun AppColors.withAccent(style: AppThemeStyle): AppColors {
    val ink = style.accentInk(isDark)
    val container = lerp(background, ink.primary, if (isDark) 0.22f else 0.12f)
    return copy(
        primary = ink.primary,
        onPrimary = ink.onPrimary,
        primaryContainer = container,
        onPrimaryContainer = textPrimary,
        tagBackground = container,
        onTag = textPrimary,
        income = ink.primary,
        hero = ink.primary,
        onHero = ink.onPrimary,
        chartIncome = ink.secondary,
        chartExpense = ink.primary,
    )
}

internal fun ColorScheme.withAccent(style: AppThemeStyle, isDark: Boolean, onSurface: Color): ColorScheme {
    val ink = style.accentInk(isDark)
    val container = lerp(background, ink.primary, if (isDark) 0.22f else 0.12f)
    return copy(
        primary = ink.primary,
        onPrimary = ink.onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onSurface,
        secondary = ink.primary,
        onSecondary = ink.onPrimary,
        secondaryContainer = container,
        onSecondaryContainer = onSurface,
        tertiary = ink.secondary,
        onTertiary = ink.onSecondary,
        tertiaryContainer = container,
        onTertiaryContainer = onSurface,
    )
}

private val MonoLight = AccentInk(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFAFAFA),
    secondary = Color(0xFF52525B),
    onSecondary = Color(0xFFFAFAFA),
)

private val MonoDark = AccentInk(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF141414),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF141414),
)

private val VitalityLight = AccentInk(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
)

private val VitalityDark = AccentInk(
    primary = Color(0xFFFF6B35),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFF4ECDC4),
    onSecondary = Color(0xFF1A1A1A),
)

private val NeonLight = AccentInk(
    primary = Color(0xFF7E22CE),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFBE185D),
    onSecondary = Color(0xFFFFFFFF),
)

private val NeonDark = AccentInk(
    primary = Color(0xFFA855F7),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFEC4899),
    onSecondary = Color(0xFF1A1A1A),
)

private val LimeLight = AccentInk(
    primary = Color(0xFF4D7C0F),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF0E7490),
    onSecondary = Color(0xFFFFFFFF),
)

private val LimeDark = AccentInk(
    primary = Color(0xFF84CC16),
    onPrimary = Color(0xFF1A2E05),
    secondary = Color(0xFF06B6D4),
    onSecondary = Color(0xFF1A1A1A),
)

private val CreamLight = AccentInk(
    primary = Color(0xFF3D6B5A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6F5E48),
    onSecondary = Color(0xFFFFFFFF),
)

private val CreamDark = AccentInk(
    primary = Color(0xFF7D9A86),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFC4B59A),
    onSecondary = Color(0xFF1A1A1A),
)
