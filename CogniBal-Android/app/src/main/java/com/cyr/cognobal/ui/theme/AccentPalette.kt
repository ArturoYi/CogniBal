package com.cyr.cognobal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 可切换的主色。每一套只保留一个色相：按钮、选中、图表、结余卡都从这一支衍生。
 * 页面底仍是中性灰。浅色用压深后仍能读的一档，夜间用更亮的一档。
 * 账户色仍按用户选的那支写入库存，绘制时再锁进当前主色相。
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

/** 同一色相的浅档，给图表收入柱和主题预览第二点用。 */
private fun Color.sameHueLift(amount: Float): Color = lerp(this, Color.White, amount)

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

private val VitalityPrimaryLight = Color(0xFFC2410C)
private val VitalityLight = AccentInk(
    primary = VitalityPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = VitalityPrimaryLight.sameHueLift(0.42f),
    onSecondary = Color(0xFFFFFFFF),
)

private val VitalityPrimaryDark = Color(0xFFFF6B35)
private val VitalityDark = AccentInk(
    primary = VitalityPrimaryDark,
    onPrimary = Color(0xFF1A1A1A),
    secondary = VitalityPrimaryDark.sameHueLift(0.28f),
    onSecondary = Color(0xFF1A1A1A),
)

private val NeonPrimaryLight = Color(0xFF7E22CE)
private val NeonLight = AccentInk(
    primary = NeonPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = NeonPrimaryLight.sameHueLift(0.42f),
    onSecondary = Color(0xFFFFFFFF),
)

private val NeonPrimaryDark = Color(0xFFA855F7)
private val NeonDark = AccentInk(
    primary = NeonPrimaryDark,
    onPrimary = Color(0xFF1A1A1A),
    secondary = NeonPrimaryDark.sameHueLift(0.28f),
    onSecondary = Color(0xFF1A1A1A),
)

private val LimePrimaryLight = Color(0xFF4D7C0F)
private val LimeLight = AccentInk(
    primary = LimePrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = LimePrimaryLight.sameHueLift(0.42f),
    onSecondary = Color(0xFFFFFFFF),
)

private val LimePrimaryDark = Color(0xFF84CC16)
private val LimeDark = AccentInk(
    primary = LimePrimaryDark,
    onPrimary = Color(0xFF1A2E05),
    secondary = LimePrimaryDark.sameHueLift(0.28f),
    onSecondary = Color(0xFF1A1A1A),
)

private val CreamPrimaryLight = Color(0xFF3D6B5A)
private val CreamLight = AccentInk(
    primary = CreamPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = CreamPrimaryLight.sameHueLift(0.42f),
    onSecondary = Color(0xFFFFFFFF),
)

private val CreamPrimaryDark = Color(0xFF7D9A86)
private val CreamDark = AccentInk(
    primary = CreamPrimaryDark,
    onPrimary = Color(0xFF1A1A1A),
    secondary = CreamPrimaryDark.sameHueLift(0.28f),
    onSecondary = Color(0xFF1A1A1A),
)
