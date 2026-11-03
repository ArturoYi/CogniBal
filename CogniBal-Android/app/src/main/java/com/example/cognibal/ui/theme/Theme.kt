package com.example.cognibal.ui.theme

import android.app.Activity
import androidx.annotation.StringRes
import com.example.cognibal.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalRippleConfiguration
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
 * 主题风格。两套都是黑白灰，浅色和夜间只换明暗。
 *
 * - [WarmCream] 深墨。枚举名沿用旧偏好键，避免已保存的选择被重置
 * - [Monochrome] 浅一档的墨。卡片和字号相同
 */
enum class AppThemeStyle(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
) {
    /** 黑白。枚举名沿用旧偏好键，避免已保存的选择被重置。 */
    WarmCream(R.string.theme_scheme_mono, R.string.theme_scheme_mono_desc),
    Monochrome(R.string.theme_scheme_mono, R.string.theme_scheme_mono_desc),
    Vitality(R.string.theme_scheme_vitality, R.string.theme_scheme_vitality_desc),
    Neon(R.string.theme_scheme_neon, R.string.theme_scheme_neon_desc),
    Lime(R.string.theme_scheme_lime, R.string.theme_scheme_lime_desc),
    Cream(R.string.theme_scheme_cream, R.string.theme_scheme_cream_desc),
}

/** 设置里展示的主色。旧的墨色和黑白算同一种。 */
internal val SelectableAccentStyles = listOf(
    AppThemeStyle.WarmCream,
    AppThemeStyle.Vitality,
    AppThemeStyle.Neon,
    AppThemeStyle.Lime,
    AppThemeStyle.Cream,
)

internal fun AppThemeStyle.sameAccent(other: AppThemeStyle): Boolean {
    fun AppThemeStyle.fold() = if (this == AppThemeStyle.Monochrome) AppThemeStyle.WarmCream else this
    return fold() == other.fold()
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
 * 两套主题均完整适配浅色模式（Light）与暗色模式（Dark）。
 *
 * @param themeStyle 主题风格，默认深墨 [AppThemeStyle.WarmCream]
 * @param darkTheme 是否使用暗色模式，默认跟随系统 [isSystemInDarkTheme]
 * @param content 内容槽
 */
@Composable
fun CogniBalTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.WarmCream,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val neutralColors = if (darkTheme) WarmCreamDarkAppColors else WarmCreamLightAppColors
    val neutralScheme = if (darkTheme) WarmCreamDarkColorScheme else WarmCreamLightColorScheme
    val appColors = neutralColors.withAccent(themeStyle)
    val colorScheme = neutralScheme.withAccent(
        style = themeStyle,
        isDark = darkTheme,
        onSurface = neutralColors.textPrimary,
    )

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
            typography = CogniBalTypography,
            shapes = CogniBalShapes,
        ) {
            // 水波纹按点击节点的矩形铺开，圆角和圆形控件上会画出形状外面。
            // 这里直接关掉，不再为裁剪它多加一层离屏绘制。
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                content()
            }
        }
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
                shape = MaterialTheme.shapes.extraSmall,
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
                color = colors.hero,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "本月结余",
                        color = colors.onHero.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "¥12,580.00",
                        color = colors.onHero,
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            text = "收入 ¥18,900.00",
                            color = colors.onHero.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "支出 ¥6,340.00",
                            color = colors.onHero.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }

            val cardShape = MaterialTheme.shapes.extraLarge
            Surface(
                color = colors.cardBackground,
                shape = cardShape,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().cardFace(cardShape),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "最近交易",
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "午餐 · 星巴克",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "预算还剩 24%",
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.labelMedium,
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
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "记一笔 (主按钮操作)", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Preview(name = "深墨 (浅色)", showBackground = true)
@Composable
private fun WarmCreamLightPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        ThemeShowcaseCard(themeName = "深墨", modeName = "浅色")
    }
}

@Preview(name = "深墨 (暗色)", showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun WarmCreamDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = true) {
        ThemeShowcaseCard(themeName = "深墨", modeName = "暗色")
    }
}

@Preview(name = "墨色 (浅色)", showBackground = true)
@Composable
private fun MonochromeLightPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = false) {
        ThemeShowcaseCard(themeName = "墨色", modeName = "浅色")
    }
}

@Preview(name = "墨色 (暗色)", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun MonochromeDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        ThemeShowcaseCard(themeName = "墨色", modeName = "暗色")
    }
}
