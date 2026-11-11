package com.cyr.cognobal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// 温润雾白画布 (#F6F7F9) + 纯白悬浮卡片 (#FFFFFF) + 黑曜石主卡 (#1F2026) + 暖香槟金点缀。
// 暗色模式为深邃曜石底 (#101216) + 柔和抬升卡面 (#181B22)。
// 零冗余投影，依靠表面色阶与留白自然分层。
// =========================================================================

private val LightBackground = Color(0xFFF6F7F9)
private val LightCard = Color(0xFFFFFFFF)
private val LightTag = Color(0xFFF0F2F5)
private val LightTextPrimary = Color(0xFF18191E)
private val LightTextSecondary = Color(0xFF5A6070)
private val LightTextTertiary = Color(0xFF8B92A2)
private val LightDivider = Color(0xFFECEEF2)
private val LightOnAccent = Color(0xFFFFFFFF)
private val LightAlert = Color(0xFFE03131)
private val LightInkStrong = Color(0xFF1F2026)
private val LightHero = Color(0xFF1F2026)
private val LightOnHero = Color(0xFFFFFFFF)

private val DarkBackground = Color(0xFF101216)
private val DarkCard = Color(0xFF181B22)
private val DarkTag = Color(0xFF222732)
private val DarkTextPrimary = Color(0xFFF4F6FA)
private val DarkTextSecondary = Color(0xFFB4BCCB)
private val DarkTextTertiary = Color(0xFF7D8698)
private val DarkDivider = Color(0xFF252A36)
private val DarkOnAccent = Color(0xFF101216)
private val DarkAlert = Color(0xFFFF6B6B)
private val DarkInkStrong = Color(0xFFF4F6FA)
private val DarkHero = Color(0xFF1E222D)
private val DarkOnHero = Color(0xFFF8FAFC)

private val ChampagneGold = Color(0xFFB88040)
private val ChampagneGoldSoft = Color(0xFFE0B47D)
private val ChampagneGoldHero = Color(0xFFD6A66C)
private val ChampagneContainer = Color(0xFFFBF3E8)

private val ChampagneGoldDark = Color(0xFFE0B47D)
private val ChampagneGoldDarkSoft = Color(0xFFF0CFA3)
private val ChampagneDarkContainer = Color(0xFF2D251C)

private val SoftInk = Color(0xFF1F2026)
private val SoftInkDark = Color(0xFFF4F6FA)

val WarmCreamLight_Background = LightBackground
val WarmCreamLight_CardBackground = LightCard
val WarmCreamLight_Primary = ChampagneGold
val WarmCreamLight_OnPrimary = LightOnAccent
val WarmCreamLight_Secondary = ChampagneContainer
val WarmCreamLight_OnSecondary = ChampagneGold
val WarmCreamLight_Expense = LightAlert
val WarmCreamLight_TextPrimary = LightTextPrimary
val WarmCreamLight_TextSecondary = LightTextSecondary
val WarmCreamLight_TextTertiary = LightTextTertiary
val WarmCreamLight_Divider = LightDivider
val WarmCreamLight_NavBar = LightCard
val WarmCreamLight_Hero = LightHero
val WarmCreamLight_OnHero = LightOnHero

val WarmCreamDark_Background = DarkBackground
val WarmCreamDark_CardBackground = DarkCard
val WarmCreamDark_Primary = ChampagneGoldDark
val WarmCreamDark_OnPrimary = DarkOnAccent
val WarmCreamDark_Secondary = ChampagneDarkContainer
val WarmCreamDark_OnSecondary = DarkTextPrimary
val WarmCreamDark_Expense = DarkAlert
val WarmCreamDark_TextPrimary = DarkTextPrimary
val WarmCreamDark_TextSecondary = DarkTextSecondary
val WarmCreamDark_TextTertiary = DarkTextTertiary
val WarmCreamDark_Divider = DarkDivider
val WarmCreamDark_NavBar = Color(0xFF15181F)
val WarmCreamDark_Hero = DarkHero
val WarmCreamDark_OnHero = DarkOnHero

val MonochromeLight_Background = LightBackground
val MonochromeLight_CardBackground = LightCard
val MonochromeLight_Primary = SoftInk
val MonochromeLight_OnPrimary = LightOnAccent
val MonochromeLight_Secondary = LightTag
val MonochromeLight_OnSecondary = LightTextPrimary
val MonochromeLight_Expense = LightAlert
val MonochromeLight_TextPrimary = LightTextPrimary
val MonochromeLight_TextSecondary = LightTextSecondary
val MonochromeLight_TextTertiary = LightTextTertiary
val MonochromeLight_Divider = LightDivider
val MonochromeLight_NavBar = LightCard
val MonochromeLight_Hero = LightHero
val MonochromeLight_OnHero = LightOnHero

val MonochromeDark_Background = DarkBackground
val MonochromeDark_CardBackground = DarkCard
val MonochromeDark_Primary = SoftInkDark
val MonochromeDark_OnPrimary = DarkOnAccent
val MonochromeDark_Secondary = DarkTag
val MonochromeDark_OnSecondary = DarkTextPrimary
val MonochromeDark_Expense = DarkAlert
val MonochromeDark_TextPrimary = DarkTextPrimary
val MonochromeDark_TextSecondary = DarkTextSecondary
val MonochromeDark_TextTertiary = DarkTextTertiary
val MonochromeDark_Divider = DarkDivider
val MonochromeDark_NavBar = Color(0xFF15181F)
val MonochromeDark_Hero = DarkHero
val MonochromeDark_OnHero = DarkOnHero

@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inkStrong: Color,
    val onInkStrong: Color,
    val background: Color,
    val onBackground: Color,
    val cardBackground: Color,
    val onCard: Color,
    val tagBackground: Color,
    val onTag: Color,
    val income: Color,
    val expense: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val navBarBackground: Color,
    val hero: Color,
    val onHero: Color,
    val heroAccent: Color,
    val chartIncome: Color,
    val chartExpense: Color,
    val isDark: Boolean,
)

val WarmCreamLightAppColors = AppColors(
    primary = WarmCreamLight_Primary,
    onPrimary = WarmCreamLight_OnPrimary,
    primaryContainer = WarmCreamLight_Secondary,
    onPrimaryContainer = WarmCreamLight_OnSecondary,
    inkStrong = LightInkStrong,
    onInkStrong = LightOnAccent,
    background = WarmCreamLight_Background,
    onBackground = WarmCreamLight_TextPrimary,
    cardBackground = WarmCreamLight_CardBackground,
    onCard = WarmCreamLight_TextPrimary,
    tagBackground = LightTag,
    onTag = WarmCreamLight_TextPrimary,
    income = Color(0xFF0F9D76),
    expense = WarmCreamLight_Expense,
    textPrimary = WarmCreamLight_TextPrimary,
    textSecondary = WarmCreamLight_TextSecondary,
    textTertiary = WarmCreamLight_TextTertiary,
    divider = WarmCreamLight_Divider,
    navBarBackground = WarmCreamLight_NavBar,
    hero = WarmCreamLight_Hero,
    onHero = WarmCreamLight_OnHero,
    heroAccent = ChampagneGoldHero,
    chartIncome = ChampagneGoldSoft,
    chartExpense = ChampagneGold,
    isDark = false,
)

val WarmCreamDarkAppColors = AppColors(
    primary = WarmCreamDark_Primary,
    onPrimary = WarmCreamDark_OnPrimary,
    primaryContainer = WarmCreamDark_Secondary,
    onPrimaryContainer = WarmCreamDark_OnSecondary,
    inkStrong = DarkInkStrong,
    onInkStrong = DarkOnAccent,
    background = WarmCreamDark_Background,
    onBackground = WarmCreamDark_TextPrimary,
    cardBackground = WarmCreamDark_CardBackground,
    onCard = WarmCreamDark_TextPrimary,
    tagBackground = DarkTag,
    onTag = WarmCreamDark_TextPrimary,
    income = Color(0xFF34D399),
    expense = WarmCreamDark_Expense,
    textPrimary = WarmCreamDark_TextPrimary,
    textSecondary = WarmCreamDark_TextSecondary,
    textTertiary = WarmCreamDark_TextTertiary,
    divider = WarmCreamDark_Divider,
    navBarBackground = WarmCreamDark_NavBar,
    hero = WarmCreamDark_Hero,
    onHero = WarmCreamDark_OnHero,
    heroAccent = ChampagneGoldDark,
    chartIncome = ChampagneGoldDarkSoft,
    chartExpense = ChampagneGoldDark,
    isDark = true,
)

val MonochromeLightAppColors = AppColors(
    primary = MonochromeLight_Primary,
    onPrimary = MonochromeLight_OnPrimary,
    primaryContainer = MonochromeLight_Secondary,
    onPrimaryContainer = MonochromeLight_OnSecondary,
    inkStrong = LightInkStrong,
    onInkStrong = LightOnAccent,
    background = MonochromeLight_Background,
    onBackground = MonochromeLight_TextPrimary,
    cardBackground = MonochromeLight_CardBackground,
    onCard = MonochromeLight_TextPrimary,
    tagBackground = LightTag,
    onTag = MonochromeLight_TextPrimary,
    income = MonochromeLight_Primary,
    expense = MonochromeLight_Expense,
    textPrimary = MonochromeLight_TextPrimary,
    textSecondary = MonochromeLight_TextSecondary,
    textTertiary = MonochromeLight_TextTertiary,
    divider = MonochromeLight_Divider,
    navBarBackground = MonochromeLight_NavBar,
    hero = MonochromeLight_Hero,
    onHero = MonochromeLight_OnHero,
    heroAccent = Color(0xFFD0D5DD),
    chartIncome = Color(0xFF98A2B3),
    chartExpense = SoftInk,
    isDark = false,
)

val MonochromeDarkAppColors = AppColors(
    primary = MonochromeDark_Primary,
    onPrimary = MonochromeDark_OnPrimary,
    primaryContainer = MonochromeDark_Secondary,
    onPrimaryContainer = MonochromeDark_OnSecondary,
    inkStrong = DarkInkStrong,
    onInkStrong = DarkOnAccent,
    background = MonochromeDark_Background,
    onBackground = MonochromeDark_TextPrimary,
    cardBackground = MonochromeDark_CardBackground,
    onCard = MonochromeDark_TextPrimary,
    tagBackground = DarkTag,
    onTag = MonochromeDark_TextPrimary,
    income = MonochromeDark_Primary,
    expense = MonochromeDark_Expense,
    textPrimary = MonochromeDark_TextPrimary,
    textSecondary = MonochromeDark_TextSecondary,
    textTertiary = MonochromeDark_TextTertiary,
    divider = MonochromeDark_Divider,
    navBarBackground = MonochromeDark_NavBar,
    hero = MonochromeDark_Hero,
    onHero = MonochromeDark_OnHero,
    heroAccent = Color(0xFF98A2B3),
    chartIncome = Color(0xFF98A2B3),
    chartExpense = SoftInkDark,
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { WarmCreamLightAppColors }

val WarmCreamLightColorScheme: ColorScheme = lightColorScheme(
    primary = WarmCreamLight_Primary,
    onPrimary = WarmCreamLight_OnPrimary,
    primaryContainer = WarmCreamLight_Secondary,
    onPrimaryContainer = WarmCreamLight_OnSecondary,
    secondary = WarmCreamLight_Primary,
    onSecondary = WarmCreamLight_OnPrimary,
    secondaryContainer = LightTag,
    onSecondaryContainer = WarmCreamLight_TextPrimary,
    tertiary = WarmCreamLight_Primary,
    onTertiary = WarmCreamLight_OnPrimary,
    tertiaryContainer = LightTag,
    onTertiaryContainer = WarmCreamLight_TextPrimary,
    background = WarmCreamLight_Background,
    onBackground = WarmCreamLight_TextPrimary,
    surface = WarmCreamLight_CardBackground,
    onSurface = WarmCreamLight_TextPrimary,
    surfaceVariant = LightTag,
    onSurfaceVariant = WarmCreamLight_TextSecondary,
    surfaceContainerLowest = WarmCreamLight_NavBar,
    surfaceContainerLow = LightCard,
    surfaceContainer = WarmCreamLight_CardBackground,
    surfaceContainerHigh = LightTag,
    surfaceContainerHighest = LightDivider,
    surfaceTint = Color.Transparent,
    error = WarmCreamLight_Expense,
    onError = LightOnAccent,
    errorContainer = Color(0xFFFFE3E3),
    onErrorContainer = LightAlert,
    outline = WarmCreamLight_TextTertiary,
    outlineVariant = WarmCreamLight_Divider,
)

val WarmCreamDarkColorScheme: ColorScheme = darkColorScheme(
    primary = WarmCreamDark_Primary,
    onPrimary = WarmCreamDark_OnPrimary,
    primaryContainer = WarmCreamDark_Secondary,
    onPrimaryContainer = WarmCreamDark_OnSecondary,
    secondary = WarmCreamDark_Primary,
    onSecondary = WarmCreamDark_OnPrimary,
    secondaryContainer = DarkTag,
    onSecondaryContainer = WarmCreamDark_TextPrimary,
    tertiary = WarmCreamDark_Primary,
    onTertiary = WarmCreamDark_OnPrimary,
    tertiaryContainer = DarkTag,
    onTertiaryContainer = WarmCreamDark_TextPrimary,
    background = WarmCreamDark_Background,
    onBackground = WarmCreamDark_TextPrimary,
    surface = WarmCreamDark_CardBackground,
    onSurface = WarmCreamDark_TextPrimary,
    surfaceVariant = DarkTag,
    onSurfaceVariant = WarmCreamDark_TextSecondary,
    surfaceContainerLowest = WarmCreamDark_NavBar,
    surfaceContainerLow = DarkCard,
    surfaceContainer = WarmCreamDark_CardBackground,
    surfaceContainerHigh = DarkTag,
    surfaceContainerHighest = DarkDivider,
    surfaceTint = Color.Transparent,
    error = WarmCreamDark_Expense,
    onError = DarkOnAccent,
    errorContainer = Color(0xFF3B181E),
    onErrorContainer = DarkAlert,
    outline = WarmCreamDark_TextTertiary,
    outlineVariant = WarmCreamDark_Divider,
)

val MonochromeLightColorScheme: ColorScheme = WarmCreamLightColorScheme.copy(
    primary = MonochromeLight_Primary,
    secondary = MonochromeLight_Primary,
    tertiary = MonochromeLight_Primary,
)

val MonochromeDarkColorScheme: ColorScheme = WarmCreamDarkColorScheme.copy(
    primary = MonochromeDark_Primary,
    secondary = MonochromeDark_Primary,
    tertiary = MonochromeDark_Primary,
)
