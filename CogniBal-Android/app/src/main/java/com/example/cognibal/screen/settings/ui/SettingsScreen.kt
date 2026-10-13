package com.example.cognibal.screen.settings.ui

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.example.cognibal.screen.settings.viewmodel.SettingsUiEvent
import com.example.cognibal.screen.settings.viewmodel.SettingsUiState
import com.example.cognibal.screen.settings.viewmodel.SettingsViewModel
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/**
 * 设置页入口。向 [SettingsViewModel] 收集状态，把操作转成 [SettingsUiEvent] 交回去。
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
        onOpenShortcut = { shortcut ->
            Toast.makeText(context, "${shortcut.label}即将开放", Toast.LENGTH_SHORT).show()
        },
        onOpenSourceLicenses = {
            Toast.makeText(context, "CogniBal 遵循 Apache 2.0 开源许可", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier,
    )
}

/**
 * 设置页主界面。
 *
 * 核心视觉与结构：
 * 1. 柔和主色+中性灰打底，无刺眼高饱和，无突兀边框；
 * 2. 统一圆角 (20.dp) 与轻阴影，模块按意图分层分组，富有律动与呼吸感；
 * 3. 窄屏自上而下流畅展开：使用统计 -> 精炼总盈余 -> 偏好与安全 -> 通用与支持 -> 底部横向工具栏（统计、报表、预算、资产）；
 * 4. 宽屏（折叠屏展开/平板/桌面）自适应双栏并排：左侧财务看板（盈余与分析工具），右侧系统偏好设置，比例舒适饱满。
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onExportData: () -> Unit,
    onOpenShortcut: (SettingsShortcut) -> Unit,
    onOpenSourceLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = uiState.preferences
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()
    val layoutDirection = LocalLayoutDirection.current
    val versionName = appVersionName()
    val layout = settingsLayoutSpec()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = layout.maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = contentPadding.calculateLeftPadding(layoutDirection),
                    end = contentPadding.calculateRightPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
                .consumeWindowInsets(contentPadding)
                .padding(
                    horizontal = layout.horizontalPadding,
                    vertical = if (layout.compactHeight) 8.dp else 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (layout.compactHeight) 8.dp else 12.dp),
        ) {
            if (layout.isWide) {
                // 宽屏模式（折叠屏展开、平板、桌面）：左右双栏平衡排布
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // 左栏：财务概览与分析工具
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        UserStatsHeader(
                            daysInUse = uiState.daysInUse,
                            totalRecords = uiState.totalRecordsCount,
                        )
                        SurplusCard(
                            surplus = uiState.totalSurplus,
                            isVisible = uiState.isSurplusVisible,
                            currencySymbol = preferences.currency.symbol,
                            onToggleVisibility = { onEvent(SettingsUiEvent.ToggleSurplusVisibility) },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        SettingsSectionTitle("常用工具与分析")
                        SettingsShortcutBar(
                            onShortcutClick = onOpenShortcut,
                            columns = 2,
                        )
                    }

                    // 右栏：系统设置与信息
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SettingsSectionTitle("偏好与安全")
                        SettingsCardGroup(
                            entries = preferenceAndSecurityEntries,
                            onEvent = onEvent,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        SettingsSectionTitle("通用与支持")
                        SettingsCardGroup(
                            entries = systemAndSupportEntries,
                            onEvent = onEvent,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CogniBal $versionName · 理智平衡生活每一刻",
                            color = colors.textTertiary.copy(alpha = 0.6f),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                        )
                    }
                }
            } else {
                // 窄屏模式（手机竖屏）：清晰自上而下的纵向节奏
                UserStatsHeader(
                    daysInUse = uiState.daysInUse,
                    totalRecords = uiState.totalRecordsCount,
                )
                SurplusCard(
                    surplus = uiState.totalSurplus,
                    isVisible = uiState.isSurplusVisible,
                    currencySymbol = preferences.currency.symbol,
                    onToggleVisibility = { onEvent(SettingsUiEvent.ToggleSurplusVisibility) },
                )
                Spacer(modifier = Modifier.height(2.dp))
                SettingsSectionTitle("常用工具与分析")
                SettingsShortcutBar(
                    onShortcutClick = onOpenShortcut,
                    columns = 4,
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionTitle("偏好与安全")
                SettingsCardGroup(
                    entries = preferenceAndSecurityEntries,
                    onEvent = onEvent,
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionTitle("通用与支持")
                SettingsCardGroup(
                    entries = systemAndSupportEntries,
                    onEvent = onEvent,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CogniBal $versionName · 理智平衡生活每一刻",
                    color = colors.textTertiary.copy(alpha = 0.6f),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
        }
    }

    // --- 子功能交互弹窗（后续可平滑演进至独立二级页面）---

    if (uiState.showAppearanceDialog) {
        AppearanceSettingsDialog(
            preferences = preferences,
            onDarkModeChange = { onEvent(SettingsUiEvent.DarkModeChanged(it)) },
            onThemeStyleChange = { onEvent(SettingsUiEvent.ThemeStyleChanged(it)) },
            onDismiss = { onEvent(SettingsUiEvent.DismissAppearanceDialog) },
        )
    }

    if (uiState.showBackupDialog) {
        BackupSettingsDialog(
            onExportData = onExportData,
            onDismiss = { onEvent(SettingsUiEvent.DismissBackupDialog) },
        )
    }

    if (uiState.showSecurityDialog) {
        SecuritySettingsDialog(
            onDismiss = { onEvent(SettingsUiEvent.DismissSecurityDialog) },
        )
    }

    if (uiState.showMoreDialog) {
        MoreSettingsDialog(
            preferences = preferences,
            cacheSizeText = uiState.cacheSizeText,
            onOpenCurrencySelect = { onEvent(SettingsUiEvent.OpenCurrencyDialog) },
            onHapticFeedbackChange = { onEvent(SettingsUiEvent.HapticFeedbackChanged(it)) },
            onOpenClearCache = { onEvent(SettingsUiEvent.OpenClearCacheDialog) },
            onDismiss = { onEvent(SettingsUiEvent.DismissMoreDialog) },
        )
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
                    lineHeight = 20.sp,
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
            shape = RoundedCornerShape(20.dp),
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
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onOpenSourceLicenses,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            text = "开源许可与致谢",
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissAboutDialog) }) {
                    Text(text = "完成", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

private data class SettingsLayoutSpec(
    val isWide: Boolean,
    val compactHeight: Boolean,
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,
)

/**
 * 跟全局脚手架或窗口信息保持一致的断点计算。
 * 预览独立运行时回退到计算当前窗口。
 */
@Composable
private fun settingsLayoutSpec(): SettingsLayoutSpec {
    val windowSizeClass = LocalCogniBalScaffold.current?.windowAdaptiveInfo?.windowSizeClass
        ?: currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    val isWide = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    val expandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
    )
    return SettingsLayoutSpec(
        isWide = isWide,
        compactHeight = !windowSizeClass.isHeightAtLeastBreakpoint(
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND,
        ),
        maxContentWidth = if (isWide) 920.dp else 560.dp,
        horizontalPadding = when {
            expandedWidth -> 28.dp
            isWide -> 20.dp
            else -> 16.dp
        },
    )
}

private data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val event: SettingsUiEvent,
)

private val preferenceAndSecurityEntries = listOf(
    SettingsEntry(
        icon = Icons.Rounded.Palette,
        title = "外观与主题",
        subtitle = "深浅外观、色彩方案",
        accentColor = Color(0xFF8E6CE6),
        event = SettingsUiEvent.OpenAppearanceDialog,
    ),
    SettingsEntry(
        icon = Icons.Rounded.Backup,
        title = "数据备份",
        subtitle = "账单导出、备份副本",
        accentColor = Color(0xFF38A169),
        event = SettingsUiEvent.OpenBackupDialog,
    ),
    SettingsEntry(
        icon = Icons.Rounded.Security,
        title = "安全与隐私",
        subtitle = "本地沙盒、脱敏保护",
        accentColor = Color(0xFFE2843B),
        event = SettingsUiEvent.OpenSecurityDialog,
    ),
)

private val systemAndSupportEntries = listOf(
    SettingsEntry(
        icon = Icons.Rounded.Tune,
        title = "更多设置",
        subtitle = "货币符号、触感反馈、清理缓存",
        accentColor = Color(0xFF3182CE),
        event = SettingsUiEvent.OpenMoreDialog,
    ),
    SettingsEntry(
        icon = Icons.Rounded.Info,
        title = "关于知余",
        subtitle = "版本 1.0.0 · 开源与致谢",
        accentColor = Color(0xFFD65D7A),
        event = SettingsUiEvent.OpenAboutDialog,
    ),
)

@Composable
private fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Text(
        text = title,
        color = colors.textTertiary,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingsCardGroup(
    entries: List<SettingsEntry>,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    SettingsSectionCard(modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) SettingsDivider()
            val accentBg = entry.accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.12f)
            SettingActionItem(
                icon = entry.icon,
                title = entry.title,
                subtitle = entry.subtitle,
                iconTint = entry.accentColor,
                iconBackgroundColor = accentBg,
                onClick = { onEvent(entry.event) },
            )
        }
    }
}

/** 底部注脚用的版本号，与安装包 versionName 一致。预览里没有包信息时回退到占位。 */
@Composable
private fun appVersionName(): String {
    if (LocalInspectionMode.current) return "1.0"
    val context = LocalContext.current
    return remember(context) {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName.orEmpty().ifEmpty { "1.0" }
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenWarmCreamPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(isSurplusVisible = false),
            onEvent = {},
            onExportData = {},
            onOpenShortcut = {},
            onOpenSourceLicenses = {},
        )
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenMonochromeDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        SettingsScreen(
            uiState = SettingsUiState(isSurplusVisible = true),
            onEvent = {},
            onExportData = {},
            onOpenShortcut = {},
            onOpenSourceLicenses = {},
        )
    }
}
