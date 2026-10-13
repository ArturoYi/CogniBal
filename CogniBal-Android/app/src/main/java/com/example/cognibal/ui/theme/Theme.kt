package com.example.cognibal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * 主题风格枚举
 *
 * - [WarmCream] 方案一：暖奶油青绿（温润护眼、低刺眼、暖调奶油米底）
 * - [Monochrome] 方案二：黑白色（极简墨灰纸色、低刺眼对比、浅色暗色均可长时间阅读）
 */
enum class AppThemeStyle(val displayName: String) {
    WarmCream("暖奶油青绿"),
    Monochrome("黑白色"),
}

/** 提供当前应用所选主题风格的 CompositionLocal */
val LocalAppThemeStyle = staticCompositionLocalOf { AppThemeStyle.WarmCream }

/**
 * 便捷访问记账扩展色彩体系（如收入、超支、卡片色、二级文字等）
 *
 * 示例：`color = MaterialTheme.appColors.income`
 */
val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

/**
 * CogniBal 全局主题入口
 *
 * 两套主题均完整适配浅色模式（Light）与暗色模式（Dark）
 *
 * @param themeStyle 主题风格，默认方案一 [AppThemeStyle.WarmCream]
 * @param darkTheme 是否使用暗色模式，默认跟随系统 [isSystemInDarkTheme]
 * @param content 内容槽
 */
@Composable
fun CogniBalTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeStyle) {
        AppThemeStyle.WarmCream -> if (darkTheme) WarmCreamDarkColorScheme else WarmCreamLightColorScheme
        AppThemeStyle.Monochrome -> if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
    }

    val appColors = when (themeStyle) {
        AppThemeStyle.WarmCream -> if (darkTheme) WarmCreamDarkAppColors else WarmCreamLightAppColors
        AppThemeStyle.Monochrome -> if (darkTheme) MonochromeDarkAppColors else MonochromeLightAppColors
    }

    // 动态同步状态栏与导航栏图标明暗度
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            // 浅色模式下使用深色图标，暗色模式下使用浅色图标
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppThemeStyle provides themeStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

// =========================================================================
// 预览组件：直观展示两套主题的浅色与暗色模式效果
// =========================================================================

@Composable
private fun ThemeShowcaseCard(
    themeName: String,
    modeName: String,
) {
    val colors = MaterialTheme.appColors
    Surface(
        color = colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$themeName · $modeName",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color = colors.tagBackground,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = if (colors.isDark) "暗色模式" else "浅色模式",
                        color = colors.onTag,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // 业务卡片展示（卡片背景、正文、次要文字、弱化文字、金额展示）
            Surface(
                color = colors.cardBackground,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "本月财务概览",
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "统计周期：2026年10月01日 - 10月10日",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("结余正数 (收入)", color = colors.textSecondary, fontSize = 12.sp)
                            Text(
                                text = "+ ¥12,580.00",
                                color = colors.income,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("支出超标 (超支)", color = colors.textSecondary, fontSize = 12.sp)
                            Text(
                                text = "- ¥3,420.50",
                                color = colors.expense,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "注：预算剩余 24%，请合理安排支出",
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                    )
                }
            }

            // 操作主按钮
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "记一笔 (主按钮操作)", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Preview(name = "方案一：暖奶油青绿 (浅色模式)", showBackground = true)
@Composable
private fun WarmCreamLightPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        ThemeShowcaseCard(themeName = "方案一：暖奶油青绿", modeName = "浅色")
    }
}

@Preview(name = "方案一：暖奶油青绿 (暗色模式)", showBackground = true, backgroundColor = 0xFF191B1A)
@Composable
private fun WarmCreamDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = true) {
        ThemeShowcaseCard(themeName = "方案一：暖奶油青绿", modeName = "暗色")
    }
}

@Preview(name = "方案二：黑白色 (浅色模式)", showBackground = true)
@Composable
private fun MonochromeLightPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = false) {
        ThemeShowcaseCard(themeName = "方案二：黑白色", modeName = "浅色")
    }
}

@Preview(name = "方案二：黑白色 (暗色模式)", showBackground = true, backgroundColor = 0xFF1A1A1C)
@Composable
private fun MonochromeDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        ThemeShowcaseCard(themeName = "方案二：黑白色", modeName = "暗色")
    }
}
