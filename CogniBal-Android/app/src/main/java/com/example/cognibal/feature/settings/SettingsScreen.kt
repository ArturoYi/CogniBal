package com.example.cognibal.screen.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.example.cognibal.R
import com.example.cognibal.navigation.AppearanceSettings
import com.example.cognibal.navigation.Assets
import com.example.cognibal.navigation.BackupSettings
import com.example.cognibal.navigation.Budget
import com.example.cognibal.navigation.Overview
import com.example.cognibal.navigation.Reports
import com.example.cognibal.navigation.SecuritySettings
import com.example.cognibal.navigation.Statistics
import com.example.cognibal.screen.feature.settings.state.SettingsUiEvent
import com.example.cognibal.screen.feature.settings.state.SettingsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.scaffold.LocalCogniBalScaffold
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors

/**
 * 设置列表。跳转由全局路由表处理，这里只把目的地交出去。
 */
@Composable
internal fun SettingsListRoute(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cacheClearedMessage = stringResource(R.string.cache_cleared)
    val openSourceMessage = stringResource(R.string.open_source_toast)
    val feedbackSubject = stringResource(R.string.feedback_email_subject)
    val feedbackUnavailable = stringResource(R.string.feedback_email_unavailable)
    val shortcutComingSoon = SettingsShortcut.entries
        .filter { shortcut -> shortcut.page() == null }
        .associateWith { shortcut ->
            stringResource(R.string.shortcut_coming_soon, stringResource(shortcut.labelRes))
        }
    val onEvent = { event: SettingsUiEvent ->
        viewModel.onEvent(event)
        when (event) {
            SettingsUiEvent.ConfirmClearCache ->
                Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()
            SettingsUiEvent.SendFeedback -> {
                if (!context.composeFeedbackEmail(feedbackSubject)) {
                    Toast.makeText(context, feedbackUnavailable, Toast.LENGTH_SHORT).show()
                }
            }
            else -> Unit
        }
    }
    SettingsScreen(
        uiState = uiState,
        onEvent = onEvent,
        onOpenShortcut = { shortcut ->
            val destination = shortcut.page()
            if (destination != null) {
                onNavigate(destination)
            } else {
                Toast.makeText(context, shortcutComingSoon.getValue(shortcut), Toast.LENGTH_SHORT).show()
            }
        },
        onOpenSourceLicenses = {
            Toast.makeText(context, openSourceMessage, Toast.LENGTH_SHORT).show()
        },
        onNavigate = onNavigate,
        modifier = modifier,
    )
}

/**
 * 设置页主界面。
 *
 * 核心视觉与结构：
 * 1. 柔和主色+中性灰打底，无刺眼高饱和，无突兀边框；
 * 2. 统一圆角 (20.dp) 与轻阴影，模块按意图分层分组，富有律动与呼吸感；
 * 3. 窄屏自上而下流畅展开：使用统计 -> 精炼总盈余 -> 总览与统计并排，下面是其余快捷入口 -> 备份、安全、外观 -> 通用与支持，反馈在最底；
 * 4. 宽屏（折叠屏展开/平板/桌面）自适应双栏并排：左侧财务看板（盈余与分析工具），右侧系统偏好设置，比例舒适饱满。
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onOpenShortcut: (SettingsShortcut) -> Unit,
    onOpenSourceLicenses: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    RequestPageBackground()
    val preferences = uiState.preferences
    val colors = MaterialTheme.appColors
    val versionName = appVersionName()
    SettingsHome(
        uiState = uiState,
        onEvent = onEvent,
        onOpenShortcut = onOpenShortcut,
        onNavigate = onNavigate,
        modifier = modifier,
    )

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
                    text = stringResource(R.string.clear_cache_title),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.clear_cache_message, uiState.cacheSizeText),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.ConfirmClearCache) }) {
                    Text(
                        text = stringResource(R.string.clear_cache_confirm),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissClearCacheDialog) }) {
                    Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
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
                    text = stringResource(R.string.about_title),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_description),
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
                            text = stringResource(R.string.open_source),
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissAboutDialog) }) {
                    Text(
                        text = stringResource(R.string.done),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun SettingsHome(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onOpenShortcut: (SettingsShortcut) -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = uiState.preferences
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()
    val layoutDirection = LocalLayoutDirection.current
    val versionName = appVersionName()
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val layout = settingsLayoutSpec(maxWidth, maxHeight)
    Box(
        modifier = Modifier.fillMaxSize(),
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
                    vertical = if (layout.compactHeight) 10.dp else 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (layout.compactHeight) 12.dp else 16.dp),
        ) {
            if (layout.isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        UserStatsHeader(
                            daysInUse = uiState.daysInUse,
                            totalRecords = uiState.totalRecordsCount,
                        )
                        SurplusCard(
                            surplus = uiState.totalSurplus,
                            surplusOverflow = uiState.surplusOverflow,
                            isVisible = uiState.isSurplusVisible,
                            currencySymbol = preferences.currency.symbol,
                            onToggleVisibility = { onEvent(SettingsUiEvent.ToggleSurplusVisibility) },
                        )
                        SettingsShortcutBar(
                            onShortcutClick = onOpenShortcut,
                            columns = 2,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SettingsPageGroup(
                            entries = preferenceAndSecurityEntries,
                            onNavigate = onNavigate,
                        )
                        SettingsCardGroup(
                            entries = systemAndSupportEntries,
                            onEvent = onEvent,
                        )
                        Text(
                            text = stringResource(R.string.app_version_footer, versionName),
                            color = colors.textTertiary.copy(alpha = 0.6f),
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            } else {
                UserStatsHeader(
                    daysInUse = uiState.daysInUse,
                    totalRecords = uiState.totalRecordsCount,
                )
                SurplusCard(
                    surplus = uiState.totalSurplus,
                    surplusOverflow = uiState.surplusOverflow,
                    isVisible = uiState.isSurplusVisible,
                    currencySymbol = preferences.currency.symbol,
                    onToggleVisibility = { onEvent(SettingsUiEvent.ToggleSurplusVisibility) },
                )
                SettingsShortcutBar(
                    onShortcutClick = onOpenShortcut,
                    columns = 4,
                )
                SettingsPageGroup(
                    entries = preferenceAndSecurityEntries,
                    onNavigate = onNavigate,
                )
                SettingsCardGroup(
                    entries = systemAndSupportEntries,
                    onEvent = onEvent,
                )
                Text(
                    text = stringResource(R.string.app_version_footer, versionName),
                    color = colors.textTertiary.copy(alpha = 0.6f),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }
    }
}

internal data class SettingsLayoutSpec(
    val isWide: Boolean,
    val compactHeight: Boolean,
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,
)

/**
 * 按这一页实际拿到的宽高决定栏数。
 *
 * 列表-详情并排时，列表窗格往往比整窗窄，不能再用窗口宽度。
 * 预览根节点有时没有上界，那时退回窗口尺寸。
 */
@Composable
internal fun settingsLayoutSpec(maxWidth: Dp, maxHeight: Dp): SettingsLayoutSpec {
    val windowSizeClass = LocalCogniBalScaffold.current?.windowAdaptiveInfo?.windowSizeClass
        ?: currentWindowAdaptiveInfoV2().windowSizeClass
    val widthBounded = maxWidth.value.isFinite()
    val heightBounded = maxHeight.value.isFinite()
    val isWide = if (widthBounded) {
        maxWidth >= MediumWidth
    } else {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    }
    val expandedWidth = if (widthBounded) {
        maxWidth >= ExpandedWidth
    } else {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    }
    val compactHeight = if (heightBounded) {
        maxHeight < MediumHeight
    } else {
        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    }
    return SettingsLayoutSpec(
        isWide = isWide,
        compactHeight = compactHeight,
        maxContentWidth = if (isWide) 1080.dp else Dp.Unspecified,
        horizontalPadding = when {
            expandedWidth -> 24.dp
            isWide -> 20.dp
            else -> 16.dp
        },
    )
}

private val MediumWidth = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp
private val ExpandedWidth = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp
private val MediumHeight = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp

private data class SettingsEntry(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    val event: SettingsUiEvent,
)

private data class SettingsPageEntry(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    val key: NavKey,
)

private val preferenceAndSecurityEntries = listOf(
    SettingsPageEntry(
        icon = Icons.Rounded.Backup,
        titleRes = R.string.settings_backup,
        key = BackupSettings,
    ),
    SettingsPageEntry(
        icon = Icons.Rounded.Security,
        titleRes = R.string.settings_security,
        key = SecuritySettings,
    ),
    SettingsPageEntry(
        icon = Icons.Rounded.Palette,
        titleRes = R.string.settings_appearance,
        key = AppearanceSettings,
    ),
)

private fun SettingsShortcut.page(): NavKey? = when (this) {
    SettingsShortcut.Overview -> Overview
    SettingsShortcut.Statistics -> Statistics
    SettingsShortcut.Reports -> Reports
    SettingsShortcut.Budget -> Budget
    SettingsShortcut.Assets -> Assets
}

private val systemAndSupportEntries = listOf(
    SettingsEntry(
        icon = Icons.Rounded.Tune,
        titleRes = R.string.settings_more,
        event = SettingsUiEvent.OpenMoreDialog,
    ),
    SettingsEntry(
        icon = Icons.Rounded.Info,
        titleRes = R.string.settings_about,
        event = SettingsUiEvent.OpenAboutDialog,
    ),
    SettingsEntry(
        icon = Icons.Rounded.Mail,
        titleRes = R.string.settings_feedback,
        event = SettingsUiEvent.SendFeedback,
    ),
)

@Composable
private fun SettingsPageGroup(
    entries: List<SettingsPageEntry>,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionCard(modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) SettingsDivider()
            SettingActionItem(
                icon = entry.icon,
                title = stringResource(entry.titleRes),
                onClick = dropUnlessResumed { onNavigate(entry.key) },
            )
        }
    }
}

@Composable
private fun SettingsCardGroup(
    entries: List<SettingsEntry>,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionCard(modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) SettingsDivider()
            SettingActionItem(
                icon = entry.icon,
                title = stringResource(entry.titleRes),
                onClick = { onEvent(entry.event) },
            )
        }
    }
}

private const val FEEDBACK_EMAIL = "lzm.cyr@gmail.com"

/** 打开系统邮件的写信界面，收件人已填好。没有可处理的邮件应用时返回 false。 */
private fun Context.composeFeedbackEmail(subject: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$FEEDBACK_EMAIL?subject=${Uri.encode(subject)}")
    }
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
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
        PageBackgroundHost {
            SettingsScreen(
                uiState = SettingsUiState(isSurplusVisible = false),
                onEvent = {},
                onOpenShortcut = {},
                onOpenSourceLicenses = {},
                onNavigate = {},
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenMonochromeDarkPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Monochrome, darkTheme = true) {
        PageBackgroundHost {
            SettingsScreen(
                uiState = SettingsUiState(
                    isSurplusVisible = true,
                    totalSurplus = 1_285_000L,
                ),
                onEvent = {},
                onOpenShortcut = {},
                onOpenSourceLicenses = {},
                onNavigate = {},
            )
        }
    }
}
