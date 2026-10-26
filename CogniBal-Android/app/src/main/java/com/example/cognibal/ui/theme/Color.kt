package com.example.cognibal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// 中性色两套主题共用，来自 Open Color gray。
// 浅色：页面 gray 1，卡片和底栏 gray 0。暗色：页面 gray 9，卡片和底栏 gray 8。
// 切换深浅色时整组一起换，切换青绿 / 黑白时中性色不动。
// =========================================================================

private val LightBackground = OpenColor.Gray1
private val LightCard = OpenColor.Gray0
private val LightTextPrimary = OpenColor.Gray9
private val LightTextSecondary = OpenColor.Gray7
private val LightTextTertiary = OpenColor.Gray6
private val LightDivider = OpenColor.Gray3
private val LightOnAccent = OpenColor.Gray0

private val DarkBackground = OpenColor.Gray9
private val DarkCard = OpenColor.Gray8
private val DarkTextPrimary = OpenColor.Gray1
private val DarkTextSecondary = OpenColor.Gray5
private val DarkTextTertiary = OpenColor.Gray6
private val DarkDivider = OpenColor.Gray7
private val DarkOnAccent = OpenColor.Gray9

// =========================================================================
// 方案一：青绿。浅色按钮和收入用 teal 9，白字才过对比度；暗色改用 teal 4。
// 方案二：黑白。浅色主色 gray 8，暗色主色 gray 2。支出两套都用 red。
// =========================================================================

val WarmCreamLight_Background = LightBackground
val WarmCreamLight_CardBackground = LightCard
val WarmCreamLight_Primary = OpenColor.Teal9
val WarmCreamLight_OnPrimary = LightOnAccent
val WarmCreamLight_Secondary = OpenColor.Teal0
val WarmCreamLight_OnSecondary = OpenColor.Teal9
val WarmCreamLight_Expense = OpenColor.Red9
val WarmCreamLight_TextPrimary = LightTextPrimary
val WarmCreamLight_TextSecondary = LightTextSecondary
val WarmCreamLight_TextTertiary = LightTextTertiary
val WarmCreamLight_Divider = LightDivider
val WarmCreamLight_NavBar = LightCard

val WarmCreamDark_Background = DarkBackground
val WarmCreamDark_CardBackground = DarkCard
val WarmCreamDark_Primary = OpenColor.Teal4
val WarmCreamDark_OnPrimary = DarkOnAccent
val WarmCreamDark_Secondary = OpenColor.Teal9
val WarmCreamDark_OnSecondary = OpenColor.Teal0
val WarmCreamDark_Expense = OpenColor.Red4
val WarmCreamDark_TextPrimary = DarkTextPrimary
val WarmCreamDark_TextSecondary = DarkTextSecondary
val WarmCreamDark_TextTertiary = DarkTextTertiary
val WarmCreamDark_Divider = DarkDivider
val WarmCreamDark_NavBar = DarkCard

val MonochromeLight_Background = LightBackground
val MonochromeLight_CardBackground = LightCard
val MonochromeLight_Primary = OpenColor.Gray8
val MonochromeLight_OnPrimary = LightOnAccent
val MonochromeLight_Secondary = OpenColor.Gray2
val MonochromeLight_OnSecondary = OpenColor.Gray9
val MonochromeLight_Expense = OpenColor.Red9
val MonochromeLight_TextPrimary = LightTextPrimary
val MonochromeLight_TextSecondary = LightTextSecondary
val MonochromeLight_TextTertiary = LightTextTertiary
val MonochromeLight_Divider = LightDivider
val MonochromeLight_NavBar = LightCard

val MonochromeDark_Background = DarkBackground
val MonochromeDark_CardBackground = DarkCard
val MonochromeDark_Primary = OpenColor.Gray2
val MonochromeDark_OnPrimary = DarkOnAccent
val MonochromeDark_Secondary = OpenColor.Gray7
val MonochromeDark_OnSecondary = OpenColor.Gray1
val MonochromeDark_Expense = OpenColor.Red4
val MonochromeDark_TextPrimary = DarkTextPrimary
val MonochromeDark_TextSecondary = DarkTextSecondary
val MonochromeDark_TextTertiary = DarkTextTertiary
val MonochromeDark_Divider = DarkDivider
val MonochromeDark_NavBar = DarkCard

// =========================================================================
// 语义化扩展色彩系统（AppColors）
// 提供专用于记账业务的直观语义色彩映射，如收入、支出、卡片底色等
// =========================================================================

@Immutable
data class AppColors(
    val primary: Color,             // 主色（主按钮、选中态、图表高亮）
    val onPrimary: Color,           // 主色上的前景色
    val primaryContainer: Color,    // 主色容器色
    val onPrimaryContainer: Color,  // 主色容器上的前景色
    val background: Color,          // 页面底色
    val onBackground: Color,        // 页面底色上的文字
    val cardBackground: Color,      // 卡片底色 / 模块容器
    val onCard: Color,              // 卡片上的正文字色
    val tagBackground: Color,       // 辅助浅色 / 标签背景 / 进度条底色
    val onTag: Color,               // 标签文字颜色
    val income: Color,              // 结余正数 / 收入 / 盈利
    val expense: Color,             // 强调红 / 超支 / 负数 / 负债 / 亏损
    val textPrimary: Color,         // 一级文字 / 账单标题、主要数字
    val textSecondary: Color,       // 二级文字 / 备注、分类、日期
    val textTertiary: Color,        // 三级弱化文字 / 占位提示 / 禁用态
    val divider: Color,             // 分割线
    val navBarBackground: Color,    // 导航底栏背景
    val isDark: Boolean,            // 是否为深色模式
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
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { WarmCreamLightAppColors }

// =========================================================================
// Material 3 ColorScheme
// secondary 用作键盘焦点圈，跟当前主色同一级，深浅色切换时一起换。
// 标签底色走 secondaryContainer，不跟焦点圈混用。
// =========================================================================

val WarmCreamLightColorScheme: ColorScheme = lightColorScheme(
    primary = WarmCreamLight_Primary,
    onPrimary = WarmCreamLight_OnPrimary,
    primaryContainer = WarmCreamLight_Secondary,
    onPrimaryContainer = WarmCreamLight_OnSecondary,
    secondary = WarmCreamLight_Primary,
    onSecondary = WarmCreamLight_OnPrimary,
    secondaryContainer = OpenColor.Teal1,
    onSecondaryContainer = OpenColor.Teal9,
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
    surfaceContainerHigh = OpenColor.Gray2,
    surfaceContainerHighest = OpenColor.Gray3,
    surfaceTint = Color.Transparent,
    error = WarmCreamLight_Expense,
    onError = LightOnAccent,
    errorContainer = OpenColor.Red0,
    onErrorContainer = OpenColor.Red9,
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
    secondaryContainer = WarmCreamDark_Secondary,
    onSecondaryContainer = WarmCreamDark_OnSecondary,
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
    surfaceContainerHigh = OpenColor.Gray7,
    surfaceContainerHighest = OpenColor.Gray7,
    surfaceTint = Color.Transparent,
    error = WarmCreamDark_Expense,
    onError = DarkOnAccent,
    errorContainer = OpenColor.Red9,
    onErrorContainer = OpenColor.Red1,
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
    secondaryContainer = OpenColor.Gray2,
    onSecondaryContainer = OpenColor.Gray9,
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
    surfaceContainerHigh = OpenColor.Gray2,
    surfaceContainerHighest = OpenColor.Gray3,
    surfaceTint = Color.Transparent,
    error = MonochromeLight_Expense,
    onError = LightOnAccent,
    errorContainer = OpenColor.Red0,
    onErrorContainer = OpenColor.Red9,
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
    secondaryContainer = MonochromeDark_Secondary,
    onSecondaryContainer = MonochromeDark_OnSecondary,
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
    surfaceContainerHigh = OpenColor.Gray7,
    surfaceContainerHighest = OpenColor.Gray7,
    surfaceTint = Color.Transparent,
    error = MonochromeDark_Expense,
    onError = DarkOnAccent,
    errorContainer = OpenColor.Red9,
    onErrorContainer = OpenColor.Red1,
    outline = MonochromeDark_TextTertiary,
    outlineVariant = MonochromeDark_Divider,
)
