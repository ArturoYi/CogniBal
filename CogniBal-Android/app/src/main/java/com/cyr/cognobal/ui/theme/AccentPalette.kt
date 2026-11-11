package com.cyr.cognobal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 黑曜石主卡 + 可选主题强调色体系：
 * 默认 [AppThemeStyle.WarmCream] 使用温润香槟金搭配曜石深灰主卡，复刻极简高质感记账视觉；
 * 其余主题只切换焦点强调色，页面与白卡保持通透干净。
 */
internal data class AccentInk(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val heroAccent: Color,
)

internal fun AppThemeStyle.accentInk(isDark: Boolean): AccentInk = when (this) {
    AppThemeStyle.WarmCream -> if (isDark) ChampagneDark else ChampagneLight
    AppThemeStyle.Cream -> if (isDark) MintDark else MintLight
    AppThemeStyle.Neon -> if (isDark) IndigoDark else IndigoLight
    AppThemeStyle.Vitality -> if (isDark) CoralDark else CoralLight
    AppThemeStyle.Lime -> if (isDark) LimeDark else LimeLight
    AppThemeStyle.Monochrome -> if (isDark) MonoDark else MonoLight
}

internal fun AppColors.withAccent(style: AppThemeStyle): AppColors {
    val ink = style.accentInk(isDark)
    val softTint = when {
        style == AppThemeStyle.Monochrome -> tagBackground
        isDark -> lerp(cardBackground, ink.primary, 0.20f)
        else -> lerp(cardBackground, ink.primary, 0.14f)
    }
    val incomeTone = when {
        style == AppThemeStyle.Monochrome -> textPrimary
        isDark -> Color(0xFF34D399)
        else -> Color(0xFF0F9D76)
    }
    return copy(
        primary = ink.primary,
        onPrimary = ink.onPrimary,
        primaryContainer = softTint,
        onPrimaryContainer = if (style == AppThemeStyle.Monochrome) textPrimary else ink.primary,
        income = incomeTone,
        heroAccent = ink.heroAccent,
        chartIncome = ink.secondary,
        chartExpense = ink.primary,
    )
}

internal fun ColorScheme.withAccent(style: AppThemeStyle, isDark: Boolean, onSurface: Color): ColorScheme {
    val ink = style.accentInk(isDark)
    val baseSurface = if (isDark) Color(0xFF181B22) else Color(0xFFFFFFFF)
    val softTint = lerp(baseSurface, ink.primary, if (isDark) 0.20f else 0.14f)
    return copy(
        primary = ink.primary,
        onPrimary = ink.onPrimary,
        primaryContainer = softTint,
        onPrimaryContainer = onSurface,
        secondary = ink.primary,
        onSecondary = ink.onPrimary,
        tertiary = ink.secondary,
        onTertiary = ink.onSecondary,
    )
}

private fun Color.sameHueLift(amount: Float): Color = lerp(this, Color.White, amount)

private val ChampagnePrimaryLight = Color(0xFFB88040)
private val ChampagneLight = AccentInk(
    primary = ChampagnePrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE0B47D),
    onSecondary = Color(0xFFFFFFFF),
    heroAccent = Color(0xFFD6A66C),
)

private val ChampagnePrimaryDark = Color(0xFFE0B47D)
private val ChampagneDark = AccentInk(
    primary = ChampagnePrimaryDark,
    onPrimary = Color(0xFF101216),
    secondary = ChampagnePrimaryDark.sameHueLift(0.28f),
    onSecondary = Color(0xFF101216),
    heroAccent = Color(0xFFE6BE8A),
)

private val MintPrimaryLight = Color(0xFF0D9488)
private val MintLight = AccentInk(
    primary = MintPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = MintPrimaryLight.sameHueLift(0.45f),
    onSecondary = Color(0xFFFFFFFF),
    heroAccent = Color(0xFF5EEAD4),
)

private val MintPrimaryDark = Color(0xFF34D399)
private val MintDark = AccentInk(
    primary = MintPrimaryDark,
    onPrimary = Color(0xFF101216),
    secondary = MintPrimaryDark.sameHueLift(0.30f),
    onSecondary = Color(0xFF101216),
    heroAccent = Color(0xFF6EE7B7),
)

private val IndigoPrimaryLight = Color(0xFF5B50EC)
private val IndigoLight = AccentInk(
    primary = IndigoPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = IndigoPrimaryLight.sameHueLift(0.46f),
    onSecondary = Color(0xFFFFFFFF),
    heroAccent = Color(0xFFA5B4FC),
)

private val IndigoPrimaryDark = Color(0xFF818CF8)
private val IndigoDark = AccentInk(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF101216),
    secondary = IndigoPrimaryDark.sameHueLift(0.30f),
    onSecondary = Color(0xFF101216),
    heroAccent = Color(0xFFA5B4FC),
)

private val CoralPrimaryLight = Color(0xFFEA580C)
private val CoralLight = AccentInk(
    primary = CoralPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = CoralPrimaryLight.sameHueLift(0.46f),
    onSecondary = Color(0xFFFFFFFF),
    heroAccent = Color(0xFFFDBA74),
)

private val CoralPrimaryDark = Color(0xFFFB923C)
private val CoralDark = AccentInk(
    primary = CoralPrimaryDark,
    onPrimary = Color(0xFF101216),
    secondary = CoralPrimaryDark.sameHueLift(0.30f),
    onSecondary = Color(0xFF101216),
    heroAccent = Color(0xFFFDBA74),
)

private val LimePrimaryLight = Color(0xFF65A30D)
private val LimeLight = AccentInk(
    primary = LimePrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    secondary = LimePrimaryLight.sameHueLift(0.46f),
    onSecondary = Color(0xFFFFFFFF),
    heroAccent = Color(0xFFBEF264),
)

private val LimePrimaryDark = Color(0xFFA3E635)
private val LimeDark = AccentInk(
    primary = LimePrimaryDark,
    onPrimary = Color(0xFF1A2E05),
    secondary = LimePrimaryDark.sameHueLift(0.30f),
    onSecondary = Color(0xFF101216),
    heroAccent = Color(0xFFBEF264),
)

private val MonoLight = AccentInk(
    primary = Color(0xFF1F2026),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF98A2B3),
    onSecondary = Color(0xFFFFFFFF),
    heroAccent = Color(0xFFD0D5DD),
)

private val MonoDark = AccentInk(
    primary = Color(0xFFF4F6FA),
    onPrimary = Color(0xFF101216),
    secondary = Color(0xFF98A2B3),
    onSecondary = Color(0xFF101216),
    heroAccent = Color(0xFF98A2B3),
)
