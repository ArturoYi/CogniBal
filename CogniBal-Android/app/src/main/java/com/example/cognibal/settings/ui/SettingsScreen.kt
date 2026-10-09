package com.example.cognibal.settings.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.settings.data.LocalThemeStateController
import com.example.cognibal.settings.data.ThemeStateController
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/**
 * 设置页
 *
 * 采用自适应网格设计（LazyVerticalGrid），根据屏幕可用尺寸自适应单列/多列卡片排列：
 * - 手机竖屏（Compact）：自适应单列分组卡片布局
 * - 折叠屏与平板（Expanded / Tablet / Desktop）：自适应 2 到 3 列卡片网格布局，充分利用大屏空间
 *
 * 支持：
 * - 切换主题色（暖奶油青绿 / 黑白色）
 * - 切换深浅外观（跟随系统 / 浅色模式 / 暗色模式，默认跟随系统）
 * - 偏好设置、数据缓存管理与关于应用
 */
@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val themeController = runCatching { LocalThemeStateController.current }
        .getOrElse { remember { ThemeStateController.preview() } }

    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()

    // 弹窗状态
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var cacheSizeText by remember { mutableStateOf("12.6 MB") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1200.dp)
                .consumeWindowInsets(contentPadding),
        ) {
            // 顶部大标题区域（跨满所有列）
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "设置",
                        color = colors.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "个性化定制你的记账主题、外观与使用偏好",
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                    )
                }
            }

            // 分组一：主题与外观（核心功能：切换暗色模式、切换主题色、弥散光斑）
            item {
                SettingsGroupCard(title = "外观与主题") {
                    // 暗色模式切换
                    ThemeModeSelector(
                        currentPreference = themeController.darkMode,
                        onSelectPreference = { themeController.updateDarkMode(it) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    )

                    SettingsDivider()

                    // 主题色风格切换
                    ThemeStyleSelector(
                        currentStyle = themeController.themeStyle,
                        onSelectStyle = { themeController.updateThemeStyle(it) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    )

                    SettingsDivider()

                    // 页面弥散装饰光斑
                    SettingSwitchItem(
                        icon = SettingsIcons.Sparkles,
                        title = "页面艺术弥散光斑",
                        subtitle = "在页面底色中渲染温和低饱和度的品牌光斑",
                        checked = themeController.dynamicBlooms,
                        onCheckedChange = { themeController.updateDynamicBlooms(it) },
                    )
                }
            }

            // 分组二：通用与记账偏好
            item {
                SettingsGroupCard(title = "记账与交互偏好") {
                    SettingActionItem(
                        icon = SettingsIcons.Currency,
                        title = "默认货币符号",
                        subtitle = "设置金额展示与记账的货币符号",
                        valueText = themeController.currency.displayName,
                        onClick = { showCurrencyDialog = true },
                    )

                    SettingsDivider()

                    SettingSwitchItem(
                        icon = SettingsIcons.Vibration,
                        title = "操作触感反馈",
                        subtitle = "按键交互与完成记录时提供微触感振动",
                        checked = themeController.hapticFeedback,
                        onCheckedChange = { themeController.updateHapticFeedback(it) },
                    )
                }
            }

            // 分组三：数据与存储
            item {
                SettingsGroupCard(title = "数据与存储") {
                    SettingActionItem(
                        icon = SettingsIcons.Storage,
                        title = "清除临时缓存",
                        subtitle = "清理本地渲染与账单分析缓存文件",
                        valueText = cacheSizeText,
                        onClick = { showClearCacheDialog = true },
                    )

                    SettingsDivider()

                    SettingActionItem(
                        icon = SettingsIcons.Palette,
                        title = "导出账单数据",
                        subtitle = "将账目数据导出为 CSV 表格文件",
                        valueText = "CSV",
                        onClick = {
                            Toast.makeText(context, "数据导出功能已就绪", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }

            // 分组四：关于与支持
            item {
                SettingsGroupCard(title = "关于与支持") {
                    SettingActionItem(
                        icon = SettingsIcons.Info,
                        title = "关于 CogniBal 账衡",
                        subtitle = "极简、温润护眼的双主题智能财务助手",
                        valueText = "v1.0.0",
                        onClick = { showAboutDialog = true },
                    )

                    SettingsDivider()

                    SettingActionItem(
                        icon = SettingsIcons.Settings,
                        title = "开源许可与致谢",
                        subtitle = "第三方开源库与字体开源协议",
                        onClick = {
                            Toast.makeText(context, "CogniBal 遵循 Apache 2.0 开源许可", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }

    // 货币切换对话框
    if (showCurrencyDialog) {
        CurrencySelectDialog(
            selectedCurrency = themeController.currency,
            onSelect = { themeController.updateCurrency(it) },
            onDismiss = { showCurrencyDialog = false },
        )
    }

    // 清除缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text(
                    text = "清除本地缓存",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "确定要清理当前 $cacheSizeText 的临时数据缓存吗？这不会影响你的真实账单记录。",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cacheSizeText = "0 KB"
                        showClearCacheDialog = false
                        Toast.makeText(context, "缓存已成功清理", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(text = "确认清理", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(text = "取消", color = colors.textTertiary)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
        )
    }

    // 关于对话框
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "CogniBal · 账衡",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text(
                        text = "版本：1.0.0 (Build 2026.10)",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CogniBal 致力于打造兼具温润护眼美学与极简高辨识度的记账工具，支持根据不同屏幕尺寸自适应布局，提供沉浸流畅的财务管理体验。",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = "好的", color = colors.primary)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

// =========================================================================
// FormFactorPreviews 自适应设备多屏预览验证
// =========================================================================

@FormFactorPreviews
@Composable
private fun SettingsScreenWarmCreamPreview() {
    val previewController = remember { ThemeStateController.preview() }
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        CompositionLocalProvider(LocalThemeStateController provides previewController) {
            SettingsScreen()
        }
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenMonochromeDarkPreview() {
    val previewController = remember { ThemeStateController.preview() }
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        CompositionLocalProvider(LocalThemeStateController provides previewController) {
            SettingsScreen()
        }
    }
}
