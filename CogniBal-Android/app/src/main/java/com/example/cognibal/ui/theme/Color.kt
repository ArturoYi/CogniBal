package com.example.cognibal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// 黑白灰骨架。页面、卡片、正文、辅助字、描边分成多档中性灰。
// 不用纯黑对纯白。柔和蓝和墨色只差墨色深浅，结构相同。
// 唯一的颜色是超支红。图表用两档灰区分，不另加蓝或金。
// =========================================================================

private val LightBackground = Color(0xFFF4F4F5)
private val LightCard = Color(0xFFFFFFFF)
private val LightTextPrimary = Color(0xFF1A1A1A)
private val LightTextSecondary = Color(0xFF3F3F46)
private val LightTextTertiary = Color(0xFF52525B)
private val LightDivider = Color(0xFFD4D4D8)
private val LightOnAccent = Color(0xFFFAFAFA)
private val LightAlert = Color(0xFFB91C1C)

private val DarkBackground = Color(0xFF141414)
private val DarkCard = Color(0xFF1F1F1F)
private val DarkTextPrimary = Color(0xFFF5F5F5)
private val DarkTextSecondary = Color(0xFFC8C8CC)
private val DarkTextTertiary = Color(0xFFA1A1AA)
private val DarkDivider = Color(0xFF71717A)
private val DarkOnAccent = Color(0xFF141414)
private val DarkAlert = Color(0xFFF87171)

private val Ink = Color(0xFF1A1A1A)
private val InkContainer = Color(0xFFF4F4F5)
private val InkOnContainer = Color(0xFF1A1A1A)
private val InkDark = Color(0xFFF5F5F5)
private val InkDarkContainer = Color(0xFF2A2A2A)
private val InkDarkOnContainer = Color(0xFFF5F5F5)

private val SoftInk = Color(0xFF3F3F46)
private val SoftInkDark = Color(0xFFE7E7EA)

// 枚举名仍是 WarmCream，已写入偏好的用户不用迁移。
val WarmCreamLight_Background = LightBackground
val WarmCreamLight_CardBackground = LightCard
val WarmCreamLight_Primary = Ink
val WarmCreamLight_OnPrimary = LightOnAccent
val WarmCreamLight_Secondary = InkContainer
val WarmCreamLight_OnSecondary = InkOnContainer
val WarmCreamLight_Expense = LightAlert
val WarmCreamLight_TextPrimary = LightTextPrimary
val WarmCreamLight_TextSecondary = LightTextSecondary
val WarmCreamLight_TextTertiary = LightTextTertiary
val WarmCreamLight_Divider = LightDivider
val WarmCreamLight_NavBar = LightCard
val WarmCreamLight_Hero = Ink
val WarmCreamLight_OnHero = LightOnAccent

val WarmCreamDark_Background = DarkBackground
val WarmCreamDark_CardBackground = DarkCard
val WarmCreamDark_Primary = InkDark
val WarmCreamDark_OnPrimary = DarkOnAccent
val WarmCreamDark_Secondary = InkDarkContainer
val WarmCreamDark_OnSecondary = InkDarkOnContainer
val WarmCreamDark_Expense = DarkAlert
val WarmCreamDark_TextPrimary = DarkTextPrimary
val WarmCreamDark_TextSecondary = DarkTextSecondary
val WarmCreamDark_TextTertiary = DarkTextTertiary
val WarmCreamDark_Divider = DarkDivider
val WarmCreamDark_NavBar = DarkCard
val WarmCreamDark_Hero = Color(0xFF2C2C2C)
val WarmCreamDark_OnHero = DarkTextPrimary

val MonochromeLight_Background = LightBackground
val MonochromeLight_CardBackground = LightCard
val MonochromeLight_Primary = SoftInk
val MonochromeLight_OnPrimary = LightOnAccent
val MonochromeLight_Secondary = InkContainer
val MonochromeLight_OnSecondary = InkOnContainer
val MonochromeLight_Expense = LightAlert
val MonochromeLight_TextPrimary = LightTextPrimary
val MonochromeLight_TextSecondary = LightTextSecondary
val MonochromeLight_TextTertiary = LightTextTertiary
val MonochromeLight_Divider = LightDivider
val MonochromeLight_NavBar = LightCard
val MonochromeLight_Hero = SoftInk
val MonochromeLight_OnHero = LightOnAccent

val MonochromeDark_Background = DarkBackground
val MonochromeDark_CardBackground = DarkCard
val MonochromeDark_Primary = SoftInkDark
val MonochromeDark_OnPrimary = DarkOnAccent
val MonochromeDark_Secondary = InkDarkContainer
val MonochromeDark_OnSecondary = InkDarkOnContainer
val MonochromeDark_Expense = DarkAlert
val MonochromeDark_TextPrimary = DarkTextPrimary
val MonochromeDark_TextSecondary = DarkTextSecondary
val MonochromeDark_TextTertiary = DarkTextTertiary
val MonochromeDark_Divider = DarkDivider
val MonochromeDark_NavBar = DarkCard
val MonochromeDark_Hero = SoftInkDark
val MonochromeDark_OnHero = DarkOnAccent

// =========================================================================
// 语义色。income 跟墨色同一支。图表是同一支灰的两档明度。
// hero 只在主题预览里当结余卡。
// =========================================================================

@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
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
    /** 收入柱。和 [chartExpense] 是同一色相的两档明度。 */
    val chartIncome: Color,
    /** 支出柱。比收入更深的同一支颜色。 */
    val chartExpense: Color,
    val isDark: Boolean,
)

val WarmCreamLightAppColors = AppColors(
    primary = WarmCreamLight_Primary,
    onPrimary = WarmCreamLight_OnPrimary,
    primaryContainer = WarmCreamLight_Secondary,
    onPrimaryContainer = WarmCreamLight_OnSecondary,
    background = WarmCreamLight_Background,
    onBackground = WarmCreamLight_TextPrimary,
    cardBackground = WarmCreamLight_CardBackground,
    onCard = WarmCreamLight_TextPrimary,
    tagBackground = WarmCreamLight_Secondary,
    onTag = WarmCreamLight_OnSecondary,
    income = WarmCreamLight_Primary,
    expense = WarmCreamLight_Expense,
    textPrimary = WarmCreamLight_TextPrimary,
    textSecondary = WarmCreamLight_TextSecondary,
    textTertiary = WarmCreamLight_TextTertiary,
    divider = WarmCreamLight_Divider,
    navBarBackground = WarmCreamLight_NavBar,
    hero = WarmCreamLight_Hero,
    onHero = WarmCreamLight_OnHero,
    chartIncome = Color(0xFF71717A),
    chartExpense = Ink,
    isDark = false,
)

val WarmCreamDarkAppColors = AppColors(
    primary = WarmCreamDark_Primary,
    onPrimary = WarmCreamDark_OnPrimary,
    primaryContainer = WarmCreamDark_Secondary,
    onPrimaryContainer = WarmCreamDark_OnSecondary,
    background = WarmCreamDark_Background,
    onBackground = WarmCreamDark_TextPrimary,
    cardBackground = WarmCreamDark_CardBackground,
    onCard = WarmCreamDark_TextPrimary,
    tagBackground = WarmCreamDark_Secondary,
    onTag = WarmCreamDark_OnSecondary,
    income = WarmCreamDark_Primary,
    expense = WarmCreamDark_Expense,
    textPrimary = WarmCreamDark_TextPrimary,
    textSecondary = WarmCreamDark_TextSecondary,
    textTertiary = WarmCreamDark_TextTertiary,
    divider = WarmCreamDark_Divider,
    navBarBackground = WarmCreamDark_NavBar,
    hero = WarmCreamDark_Hero,
    onHero = WarmCreamDark_OnHero,
    chartIncome = Color(0xFFD4D4D8),
    chartExpense = Color(0xFF8A8A93),
    isDark = true,
)

val MonochromeLightAppColors = AppColors(
    primary = MonochromeLight_Primary,
    onPrimary = MonochromeLight_OnPrimary,
    primaryContainer = MonochromeLight_Secondary,
    onPrimaryContainer = MonochromeLight_OnSecondary,
    background = MonochromeLight_Background,
    onBackground = MonochromeLight_TextPrimary,
    cardBackground = MonochromeLight_CardBackground,
    onCard = MonochromeLight_TextPrimary,
    tagBackground = MonochromeLight_Secondary,
    onTag = MonochromeLight_OnSecondary,
    income = MonochromeLight_Primary,
    expense = MonochromeLight_Expense,
    textPrimary = MonochromeLight_TextPrimary,
    textSecondary = MonochromeLight_TextSecondary,
    textTertiary = MonochromeLight_TextTertiary,
    divider = MonochromeLight_Divider,
    navBarBackground = MonochromeLight_NavBar,
    hero = MonochromeLight_Hero,
    onHero = MonochromeLight_OnHero,
    chartIncome = Color(0xFF71717A),
    chartExpense = SoftInk,
    isDark = false,
)

val MonochromeDarkAppColors = AppColors(
    primary = MonochromeDark_Primary,
    onPrimary = MonochromeDark_OnPrimary,
    primaryContainer = MonochromeDark_Secondary,
    onPrimaryContainer = MonochromeDark_OnSecondary,
    background = MonochromeDark_Background,
    onBackground = MonochromeDark_TextPrimary,
    cardBackground = MonochromeDark_CardBackground,
    onCard = MonochromeDark_TextPrimary,
    tagBackground = MonochromeDark_Secondary,
    onTag = MonochromeDark_OnSecondary,
    income = MonochromeDark_Primary,
    expense = MonochromeDark_Expense,
    textPrimary = MonochromeDark_TextPrimary,
    textSecondary = MonochromeDark_TextSecondary,
    textTertiary = MonochromeDark_TextTertiary,
    divider = MonochromeDark_Divider,
    navBarBackground = MonochromeDark_NavBar,
    hero = MonochromeDark_Hero,
    onHero = MonochromeDark_OnHero,
    chartIncome = Color(0xFFD4D4D8),
    chartExpense = Color(0xFF8A8A93),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { WarmCreamLightAppColors }

// =========================================================================
// Material 3。secondary 是键盘焦点圈，跟主色同一支。
// 标签底走 secondaryContainer。
// =========================================================================

val WarmCreamLightColorScheme: ColorScheme = lightColorScheme(
    primary = WarmCreamLight_Primary,
    onPrimary = WarmCreamLight_OnPrimary,
    primaryContainer = WarmCreamLight_Secondary,
    onPrimaryContainer = WarmCreamLight_OnSecondary,
    secondary = WarmCreamLight_Primary,
    onSecondary = WarmCreamLight_OnPrimary,
    secondaryContainer = InkContainer,
    onSecondaryContainer = InkOnContainer,
    tertiary = WarmCreamLight_Primary,
    onTertiary = WarmCreamLight_OnPrimary,
    tertiaryContainer = WarmCreamLight_Secondary,
    onTertiaryContainer = WarmCreamLight_OnSecondary,
    background = WarmCreamLight_Background,
    onBackground = WarmCreamLight_TextPrimary,
    surface = WarmCreamLight_CardBackground,
    onSurface = WarmCreamLight_TextPrimary,
    surfaceVariant = WarmCreamLight_Secondary,
    onSurfaceVariant = WarmCreamLight_TextSecondary,
    surfaceContainerLowest = WarmCreamLight_NavBar,
    surfaceContainerLow = LightCard,
    surfaceContainer = WarmCreamLight_CardBackground,
    surfaceContainerHigh = Color(0xFFEBEBEE),
    surfaceContainerHighest = LightDivider,
    surfaceTint = Color.Transparent,
    error = WarmCreamLight_Expense,
    onError = LightOnAccent,
    errorContainer = Color(0xFFF8E8E8),
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
    secondaryContainer = InkDarkContainer,
    onSecondaryContainer = InkDarkOnContainer,
    tertiary = WarmCreamDark_Primary,
    onTertiary = WarmCreamDark_OnPrimary,
    tertiaryContainer = WarmCreamDark_Secondary,
    onTertiaryContainer = WarmCreamDark_OnSecondary,
    background = WarmCreamDark_Background,
    onBackground = WarmCreamDark_TextPrimary,
    surface = WarmCreamDark_CardBackground,
    onSurface = WarmCreamDark_TextPrimary,
    surfaceVariant = WarmCreamDark_Secondary,
    onSurfaceVariant = WarmCreamDark_TextSecondary,
    surfaceContainerLowest = WarmCreamDark_NavBar,
    surfaceContainerLow = DarkCard,
    surfaceContainer = WarmCreamDark_CardBackground,
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF3A3A3A),
    surfaceTint = Color.Transparent,
    error = WarmCreamDark_Expense,
    onError = DarkOnAccent,
    errorContainer = Color(0xFF3A2224),
    onErrorContainer = DarkAlert,
    outline = WarmCreamDark_TextTertiary,
    outlineVariant = WarmCreamDark_Divider,
)

val MonochromeLightColorScheme: ColorScheme = lightColorScheme(
    primary = MonochromeLight_Primary,
    onPrimary = MonochromeLight_OnPrimary,
    primaryContainer = MonochromeLight_Secondary,
    onPrimaryContainer = MonochromeLight_OnSecondary,
    secondary = MonochromeLight_Primary,
    onSecondary = MonochromeLight_OnPrimary,
    secondaryContainer = InkContainer,
    onSecondaryContainer = InkOnContainer,
    tertiary = MonochromeLight_Primary,
    onTertiary = MonochromeLight_OnPrimary,
    tertiaryContainer = MonochromeLight_Secondary,
    onTertiaryContainer = MonochromeLight_OnSecondary,
    background = MonochromeLight_Background,
    onBackground = MonochromeLight_TextPrimary,
    surface = MonochromeLight_CardBackground,
    onSurface = MonochromeLight_TextPrimary,
    surfaceVariant = MonochromeLight_Secondary,
    onSurfaceVariant = MonochromeLight_TextSecondary,
    surfaceContainerLowest = MonochromeLight_NavBar,
    surfaceContainerLow = LightCard,
    surfaceContainer = MonochromeLight_CardBackground,
    surfaceContainerHigh = Color(0xFFEBEBEE),
    surfaceContainerHighest = LightDivider,
    surfaceTint = Color.Transparent,
    error = MonochromeLight_Expense,
    onError = LightOnAccent,
    errorContainer = Color(0xFFF8E8E8),
    onErrorContainer = LightAlert,
    outline = MonochromeLight_TextTertiary,
    outlineVariant = MonochromeLight_Divider,
)

val MonochromeDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MonochromeDark_Primary,
    onPrimary = MonochromeDark_OnPrimary,
    primaryContainer = MonochromeDark_Secondary,
    onPrimaryContainer = MonochromeDark_OnSecondary,
    secondary = MonochromeDark_Primary,
    onSecondary = MonochromeDark_OnPrimary,
    secondaryContainer = InkDarkContainer,
    onSecondaryContainer = InkDarkOnContainer,
    tertiary = MonochromeDark_Primary,
    onTertiary = MonochromeDark_OnPrimary,
    tertiaryContainer = MonochromeDark_Secondary,
    onTertiaryContainer = MonochromeDark_OnSecondary,
    background = MonochromeDark_Background,
    onBackground = MonochromeDark_TextPrimary,
    surface = MonochromeDark_CardBackground,
    onSurface = MonochromeDark_TextPrimary,
    surfaceVariant = MonochromeDark_Secondary,
    onSurfaceVariant = MonochromeDark_TextSecondary,
    surfaceContainerLowest = MonochromeDark_NavBar,
    surfaceContainerLow = DarkCard,
    surfaceContainer = MonochromeDark_CardBackground,
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF3A3A3A),
    surfaceTint = Color.Transparent,
    error = MonochromeDark_Expense,
    onError = DarkOnAccent,
    errorContainer = Color(0xFF3A2224),
    onErrorContainer = DarkAlert,
    outline = MonochromeDark_TextTertiary,
    outlineVariant = MonochromeDark_Divider,
)
