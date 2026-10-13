package com.example.cognibal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// 方案一：暖奶油青绿（Warm Cream）调色板
// 页面底色为温暖温和的暖奶油米，搭配低饱和奶油青绿，长时间阅读不累眼
// =========================================================================

// --- 方案一 浅色（Light）原色常量 ---
val WarmCreamLight_Background = Color(0xFFF8F7F2)       // 背景底色（暖奶油米，非死白）
val WarmCreamLight_CardBackground = Color(0xFFFFFFFF)   // 卡片底色（柔和白，在暖奶油米底上有微浮层）
val WarmCreamLight_Primary = Color(0xFF78A898)          // 主色（奶油青绿）：主按钮、高亮、选中态、结余正数
val WarmCreamLight_OnPrimary = Color(0xFFFFFFFF)        // 主色上的文字图标
val WarmCreamLight_Secondary = Color(0xFFD7E6E1)        // 辅助浅青绿：标签背景、进度条底色
val WarmCreamLight_OnSecondary = Color(0xFF264A3F)      // 辅助浅青绿上的前景色
val WarmCreamLight_Expense = Color(0xFFD96C6C)          // 强调红：超支金额、负债、亏损
val WarmCreamLight_TextPrimary = Color(0xFF262626)      // 正文黑：账单标题、主要数字，非纯黑
val WarmCreamLight_TextSecondary = Color(0xFF666666)    // 次要文字：备注、分类小字、辅助说明
val WarmCreamLight_TextTertiary = Color(0xFFA3A3A3)     // 占位 / 弱化文字：提示文字、禁用态
val WarmCreamLight_Divider = Color(0xFFE5E3DD)          // 分割线：浅分割，和奶油底色自然融合
val WarmCreamLight_NavBar = Color(0xFFFFFFFF)           // 导航底栏底色

// --- 方案一 暗色（Dark）适配原色常量 ---
// 延续温暖温和调性，采用深暖炭灰（带极微温润橄榄调），避免冷黑与高刺眼对比
val WarmCreamDark_Background = Color(0xFF191B1A)        // 页面底色（深暖炭灰，温润护眼）
val WarmCreamDark_CardBackground = Color(0xFF232725)    // 卡片底色（微抬高的深暖灰容器）
val WarmCreamDark_Primary = Color(0xFF8BBFB0)           // 主色（暗色提亮奶油青绿，保证识别度）
val WarmCreamDark_OnPrimary = Color(0xFF0E2C22)         // 主色上的文字
val WarmCreamDark_Secondary = Color(0xFF283D36)         // 辅助深青绿淡块（暗色下的标签/色块容器）
val WarmCreamDark_OnSecondary = Color(0xFFD7E6E1)       // 辅助深青绿上的浅色文字
val WarmCreamDark_Expense = Color(0xFFE27B7B)           // 强调红（暗色提亮，醒目且柔和）
val WarmCreamDark_TextPrimary = Color(0xFFE8E7E2)       // 正文文字（暖米白，与奶油调呼应，非冷白）
val WarmCreamDark_TextSecondary = Color(0xFF9E9D97)     // 次要文字（温润中灰）
val WarmCreamDark_TextTertiary = Color(0xFF686864)      // 占位 / 弱化文字（暗灰）
val WarmCreamDark_Divider = Color(0xFF2E312F)           // 暗调分割线
val WarmCreamDark_NavBar = Color(0xFF1F2321)            // 导航底栏底色

// =========================================================================
// 方案二：黑白色（Monochrome）调色板
// 极简墨灰纸色，避开纯黑与纯白。对比足够阅读，长时间看不刺眼
// =========================================================================

// --- 方案二 浅色（Light）原色常量 ---
val MonochromeLight_Background = Color(0xFFF0EEEA)       // 页面背景（暖灰纸色，避开冷白）
val MonochromeLight_CardBackground = Color(0xFFF8F7F4)   // 卡片背景（柔和米白，非纯白）
val MonochromeLight_Primary = Color(0xFF3E3E40)          // 主色（柔炭灰）：主按钮、核心焦点、正数金额
val MonochromeLight_OnPrimary = Color(0xFFF7F6F3)        // 主色上的文字与图标（暖米白）
val MonochromeLight_Secondary = Color(0xFFE6E4DE)        // 辅助暖灰：标签背景、次要区块
val MonochromeLight_OnSecondary = Color(0xFF3A3A3C)      // 辅助色上的文字
val MonochromeLight_Expense = Color(0xFFC45C58)          // 强调红：压低饱和，支出仍可辨
val MonochromeLight_TextPrimary = Color(0xFF2C2C2E)      // 一级文字（墨灰，非纯黑）
val MonochromeLight_TextSecondary = Color(0xFF6A6A6C)    // 二级文字（分类、日期）
val MonochromeLight_TextTertiary = Color(0xFF989896)     // 三级弱化文字（备注、占位提示）
val MonochromeLight_Divider = Color(0xFFDDDBD5)          // 细分割线
val MonochromeLight_NavBar = Color(0xFFF6F5F2)           // 导航底栏底色

// --- 方案二 暗色（Dark）适配原色常量 ---
// 抬高的炭灰底，暖灰白文字，避免纯黑底上的纯白光晕
val MonochromeDark_Background = Color(0xFF1A1A1C)        // 页面背景（抬高炭灰，避免纯黑）
val MonochromeDark_CardBackground = Color(0xFF242426)    // 卡片背景（略抬高的炭灰容器）
val MonochromeDark_Primary = Color(0xFFD8D6D1)           // 主色（暖灰白，非纯白高光）
val MonochromeDark_OnPrimary = Color(0xFF1E1E20)         // 主色上的文字
val MonochromeDark_Secondary = Color(0xFF323234)         // 辅助暗灰（次级色块与标签容器）
val MonochromeDark_OnSecondary = Color(0xFFE4E2DD)       // 辅助块上的文字
val MonochromeDark_Expense = Color(0xFFE08A84)           // 强调红（降低亮度，仍作警示）
val MonochromeDark_TextPrimary = Color(0xFFE4E2DD)       // 一级文字（暖米灰，非近纯白）
val MonochromeDark_TextSecondary = Color(0xFFA3A19C)     // 二级文字（中性灰）
val MonochromeDark_TextTertiary = Color(0xFF6E6C68)      // 三级弱化文字
val MonochromeDark_Divider = Color(0xFF333331)           // 细分割线
val MonochromeDark_NavBar = Color(0xFF1E1E20)            // 导航底栏底色

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

// 方案一：暖奶油青绿 - 浅色 AppColors
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

// 方案一：暖奶油青绿 - 暗色 AppColors
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

// 方案二：黑白色 - 浅色 AppColors
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

// 方案二：黑白色 - 暗色 AppColors
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
// Material 3 ColorScheme 完整构建
// =========================================================================

// 方案一：暖奶油青绿 - 浅色 Material 3 ColorScheme
val WarmCreamLightColorScheme: ColorScheme = lightColorScheme(
    primary = WarmCreamLight_Primary,
    onPrimary = WarmCreamLight_OnPrimary,
    primaryContainer = WarmCreamLight_Secondary,
    onPrimaryContainer = Color(0xFF19382F),
    secondary = WarmCreamLight_Secondary,
    onSecondary = WarmCreamLight_OnSecondary,
    secondaryContainer = Color(0xFFE9F2EE),
    onSecondaryContainer = Color(0xFF1B3B31),
    background = WarmCreamLight_Background,
    onBackground = WarmCreamLight_TextPrimary,
    surface = WarmCreamLight_CardBackground,
    onSurface = WarmCreamLight_TextPrimary,
    surfaceVariant = WarmCreamLight_Secondary,
    onSurfaceVariant = WarmCreamLight_TextSecondary,
    surfaceContainerLowest = WarmCreamLight_NavBar,
    surfaceContainerLow = Color(0xFFF3F1EB),
    surfaceContainer = WarmCreamLight_CardBackground,
    surfaceContainerHigh = Color(0xFFE6E3DB),
    surfaceContainerHighest = Color(0xFFDFDCD3),
    surfaceTint = Color.Transparent,
    error = WarmCreamLight_Expense,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF5B1616),
    outline = WarmCreamLight_TextTertiary,
    outlineVariant = WarmCreamLight_Divider,
)

// 方案一：暖奶油青绿 - 暗色 Material 3 ColorScheme
val WarmCreamDarkColorScheme: ColorScheme = darkColorScheme(
    primary = WarmCreamDark_Primary,
    onPrimary = WarmCreamDark_OnPrimary,
    primaryContainer = WarmCreamDark_Secondary,
    onPrimaryContainer = Color(0xFFC8E6DC),
    secondary = WarmCreamDark_Primary,
    onSecondary = WarmCreamDark_OnPrimary,
    secondaryContainer = WarmCreamDark_Secondary,
    onSecondaryContainer = WarmCreamDark_OnSecondary,
    background = WarmCreamDark_Background,
    onBackground = WarmCreamDark_TextPrimary,
    surface = WarmCreamDark_CardBackground,
    onSurface = WarmCreamDark_TextPrimary,
    surfaceVariant = WarmCreamDark_Secondary,
    onSurfaceVariant = WarmCreamDark_TextSecondary,
    surfaceContainerLowest = Color(0xFF131514),
    surfaceContainerLow = Color(0xFF1B1D1C),
    surfaceContainer = WarmCreamDark_CardBackground,
    surfaceContainerHigh = Color(0xFF2C302E),
    surfaceContainerHighest = Color(0xFF353937),
    surfaceTint = Color.Transparent,
    error = WarmCreamDark_Expense,
    onError = Color(0xFF4A1010),
    errorContainer = Color(0xFF602323),
    onErrorContainer = Color(0xFFFCD4D4),
    outline = WarmCreamDark_TextTertiary,
    outlineVariant = WarmCreamDark_Divider,
)

// 方案二：黑白色 - 浅色 Material 3 ColorScheme
val MonochromeLightColorScheme: ColorScheme = lightColorScheme(
    primary = MonochromeLight_Primary,
    onPrimary = MonochromeLight_OnPrimary,
    primaryContainer = MonochromeLight_Secondary,
    onPrimaryContainer = MonochromeLight_OnSecondary,
    secondary = MonochromeLight_Secondary,
    onSecondary = MonochromeLight_OnSecondary,
    secondaryContainer = Color(0xFFEAE8E2),
    onSecondaryContainer = Color(0xFF2C2C2E),
    background = MonochromeLight_Background,
    onBackground = MonochromeLight_TextPrimary,
    surface = MonochromeLight_CardBackground,
    onSurface = MonochromeLight_TextPrimary,
    surfaceVariant = MonochromeLight_Secondary,
    onSurfaceVariant = MonochromeLight_TextSecondary,
    surfaceContainerLowest = MonochromeLight_NavBar,
    surfaceContainerLow = Color(0xFFF4F3F0),
    surfaceContainer = MonochromeLight_CardBackground,
    surfaceContainerHigh = Color(0xFFE8E6E0),
    surfaceContainerHighest = Color(0xFFDEDBD4),
    surfaceTint = Color.Transparent,
    error = MonochromeLight_Expense,
    onError = Color(0xFFF7F6F3),
    errorContainer = Color(0xFFF6E6E4),
    onErrorContainer = Color(0xFF5C2422),
    outline = MonochromeLight_TextTertiary,
    outlineVariant = MonochromeLight_Divider,
)

// 方案二：黑白色 - 暗色 Material 3 ColorScheme
val MonochromeDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MonochromeDark_Primary,
    onPrimary = MonochromeDark_OnPrimary,
    primaryContainer = MonochromeDark_Secondary,
    onPrimaryContainer = MonochromeDark_OnSecondary,
    secondary = MonochromeDark_Primary,
    onSecondary = MonochromeDark_OnPrimary,
    secondaryContainer = MonochromeDark_Secondary,
    onSecondaryContainer = MonochromeDark_OnSecondary,
    background = MonochromeDark_Background,
    onBackground = MonochromeDark_TextPrimary,
    surface = MonochromeDark_CardBackground,
    onSurface = MonochromeDark_TextPrimary,
    surfaceVariant = MonochromeDark_Secondary,
    onSurfaceVariant = MonochromeDark_TextSecondary,
    surfaceContainerLowest = Color(0xFF141416),
    surfaceContainerLow = Color(0xFF1E1E20),
    surfaceContainer = MonochromeDark_CardBackground,
    surfaceContainerHigh = Color(0xFF2E2E30),
    surfaceContainerHighest = Color(0xFF38383A),
    surfaceTint = Color.Transparent,
    error = MonochromeDark_Expense,
    onError = Color(0xFF3A1614),
    errorContainer = Color(0xFF5A3030),
    onErrorContainer = Color(0xFFF3D6D4),
    outline = MonochromeDark_TextTertiary,
    outlineVariant = MonochromeDark_Divider,
)
