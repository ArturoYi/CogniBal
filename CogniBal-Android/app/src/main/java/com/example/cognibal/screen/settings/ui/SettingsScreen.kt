package com.example.cognibal.screen.settings.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.screen.settings.viewmodel.SettingsUiEvent
import com.example.cognibal.screen.settings.viewmodel.SettingsUiState
import com.example.cognibal.screen.settings.viewmodel.SettingsViewModel
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/**
 * 设置页入口。向 [SettingsViewModel] 收集状态，把点击收成 [SettingsUiEvent] 交回去。
 *
 * 和 [com.example.cognibal.MainActivity] 使用同一个 Activity 级 ViewModel。
 */
@Composable
internal fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        uiState = uiState,
        onEvent = { event ->
            viewModel.onEvent(event)
            if (event == SettingsUiEvent.ConfirmClearCache) {
                Toast.makeText(context, "缓存已成功清理", Toast.LENGTH_SHORT).show()
            }
        },
        onExportData = {
            Toast.makeText(context, "数据导出功能已就绪", Toast.LENGTH_SHORT).show()
        },
        onOpenSourceLicenses = {
            Toast.makeText(context, "CogniBal 遵循 Apache 2.0 开源许可", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier,
    )
}

/**
 * 设置页。
 *
 * 自适应网格：窄屏一列分组卡片，宽屏按 340.dp 起排成多列，最宽收到 1200.dp。
 * 这个函数只负责绘制 [uiState]，不读仓库，也不持有 ViewModel。
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onExportData: () -> Unit,
    onOpenSourceLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = uiState.preferences
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr) + 16.dp,
                end = contentPadding.calculateRightPadding(LayoutDirection.Ltr) + 16.dp,
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

            item {
                SettingsGroupCard(title = "外观与主题") {
                    ThemeModeSelector(
                        currentPreference = preferences.darkMode,
                        onSelectPreference = { onEvent(SettingsUiEvent.DarkModeChanged(it)) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    )

                    SettingsDivider()

                    ThemeStyleSelector(
                        currentStyle = preferences.themeStyle,
                        onSelectStyle = { onEvent(SettingsUiEvent.ThemeStyleChanged(it)) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    )

                    SettingsDivider()

                    SettingSwitchItem(
                        icon = SettingsIcons.Sparkles,
                        title = "页面艺术弥散光斑",
                        subtitle = "在页面底色中渲染温和低饱和度的品牌光斑",
                        checked = preferences.dynamicBlooms,
                        onCheckedChange = { onEvent(SettingsUiEvent.DynamicBloomsChanged(it)) },
                    )
                }
            }

            item {
                SettingsGroupCard(title = "记账与交互偏好") {
                    SettingActionItem(
                        icon = SettingsIcons.Currency,
                        title = "默认货币符号",
                        subtitle = "设置金额展示与记账的货币符号",
                        valueText = preferences.currency.displayName,
                        onClick = { onEvent(SettingsUiEvent.OpenCurrencyDialog) },
                    )

                    SettingsDivider()

                    SettingSwitchItem(
                        icon = SettingsIcons.Vibration,
                        title = "操作触感反馈",
                        subtitle = "按键交互与完成记录时提供微触感振动",
                        checked = preferences.hapticFeedback,
                        onCheckedChange = { onEvent(SettingsUiEvent.HapticFeedbackChanged(it)) },
                    )
                }
            }

            item {
                SettingsGroupCard(title = "数据与存储") {
                    SettingActionItem(
                        icon = SettingsIcons.Storage,
                        title = "清除临时缓存",
                        subtitle = "清理本地渲染与账单分析缓存文件",
                        valueText = uiState.cacheSizeText,
                        onClick = { onEvent(SettingsUiEvent.OpenClearCacheDialog) },
                    )

                    SettingsDivider()

                    SettingActionItem(
                        icon = SettingsIcons.Palette,
                        title = "导出账单数据",
                        subtitle = "将账目数据导出为 CSV 表格文件",
                        valueText = "CSV",
                        onClick = onExportData,
                    )
                }
            }

            item {
                SettingsGroupCard(title = "关于与支持") {
                    SettingActionItem(
                        icon = SettingsIcons.Info,
                        title = "关于 CogniBal 账衡",
                        subtitle = "极简、温润护眼的双主题智能财务助手",
                        valueText = "v1.0.0",
                        onClick = { onEvent(SettingsUiEvent.OpenAboutDialog) },
                    )

                    SettingsDivider()

                    SettingActionItem(
                        icon = SettingsIcons.Settings,
                        title = "开源许可与致谢",
                        subtitle = "第三方开源库与字体开源协议",
                        onClick = onOpenSourceLicenses,
                    )
                }
            }
        }
    }

    if (uiState.showCurrencyDialog) {
        CurrencySelectDialog(
            selectedCurrency = preferences.currency,
            onSelect = { onEvent(SettingsUiEvent.CurrencySelected(it)) },
            onDismiss = { onEvent(SettingsUiEvent.DismissCurrencyDialog) },
        )
    }

    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissClearCacheDialog) },
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
                    text = "确定要清理当前 ${uiState.cacheSizeText} 的临时数据缓存吗？这不会影响你的真实账单记录。",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.ConfirmClearCache) }) {
                    Text(text = "确认清理", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissClearCacheDialog) }) {
                    Text(text = "取消", color = colors.textTertiary)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
        )
    }

    if (uiState.showAboutDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissAboutDialog) },
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
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissAboutDialog) }) {
                    Text(text = "好的", color = colors.primary)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenWarmCreamPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(),
            onEvent = {},
            onExportData = {},
            onOpenSourceLicenses = {},
        )
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenMonochromeDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        SettingsScreen(
            uiState = SettingsUiState(),
            onEvent = {},
            onExportData = {},
            onOpenSourceLicenses = {},
        )
    }
}
