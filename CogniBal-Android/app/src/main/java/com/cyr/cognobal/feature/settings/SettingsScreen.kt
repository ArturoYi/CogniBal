package com.cyr.cognobal.screen.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.cyr.cognobal.R
import com.cyr.cognobal.navigation.AppearanceSettings
import com.cyr.cognobal.navigation.Assets
import com.cyr.cognobal.navigation.BackupSettings
import com.cyr.cognobal.navigation.Budget
import com.cyr.cognobal.navigation.LegalPage
import com.cyr.cognobal.navigation.MoreSettings
import com.cyr.cognobal.navigation.Overview
import com.cyr.cognobal.navigation.Reports
import com.cyr.cognobal.navigation.SecuritySettings
import com.cyr.cognobal.navigation.Statistics
import com.cyr.cognobal.screen.feature.legal.LegalDocumentKind
import com.cyr.cognobal.screen.feature.settings.component.HeaderCircleButton
import com.cyr.cognobal.screen.feature.settings.state.SettingsUiEvent
import com.cyr.cognobal.screen.feature.settings.state.SettingsUiState
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.background.RequestPageBackground
import com.cyr.cognobal.ui.insets.listContentPadding
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.scaffold.LocalCogniBalScaffold
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors

@Composable
internal fun SettingsListRoute(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedbackSubject = stringResource(R.string.feedback_email_subject)
    val feedbackUnavailable = stringResource(R.string.feedback_email_unavailable)
    val shortcutComingSoon = SettingsShortcut.entries
        .filter { shortcut -> shortcut.page() == null }
        .associateWith { shortcut ->
            stringResource(R.string.shortcut_coming_soon, stringResource(shortcut.labelRes))
        }
    val onEvent = { event: SettingsUiEvent ->
        viewModel.onEvent(event)
        if (event == SettingsUiEvent.SendFeedback && !context.composeFeedbackEmail(feedbackSubject)) {
            Toast.makeText(context, feedbackUnavailable, Toast.LENGTH_SHORT).show()
        }
    }
    val openLicense = dropUnlessResumed {
        viewModel.onEvent(SettingsUiEvent.DismissAboutDialog)
        onNavigate(LegalPage(LegalDocumentKind.License))
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
        onOpenSourceLicenses = openLicense,
        onNavigate = onNavigate,
        modifier = modifier,
    )
}

/**
 * 设置页（无账号本地账本极简重构）：
 * 1. 顶部标题「设置」 + 右上角外观快捷入口
 * 2. 曜石深灰伴随卡（陪伴天数 · 累计笔数 + 香槟金备份快捷按钮，无虚假头像与 VIP）
 * 3. 纯白分组菜单列表（账本工具 / 偏好与数据 / 支持与版本，零次要废话描述）
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
    val colors = MaterialTheme.appColors
    val versionName = appVersionName()
    SettingsHome(
        uiState = uiState,
        onEvent = onEvent,
        onNavigate = onNavigate,
        versionName = versionName,
        modifier = modifier,
    )

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
            shape = RoundedCornerShape(24.dp),
        )
    }
}

@Composable
private fun SettingsHome(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigate: (NavKey) -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()
    val layoutDirection = LocalLayoutDirection.current
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.tab_settings),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    HeaderCircleButton(
                        icon = Icons.Outlined.Palette,
                        description = stringResource(R.string.settings_appearance),
                        onClick = dropUnlessResumed { onNavigate(AppearanceSettings) },
                    )
                }

                UserStatsHeader(
                    daysInUse = uiState.daysInUse,
                    totalRecords = uiState.totalRecordsCount,
                    onBackupClick = dropUnlessResumed { onNavigate(BackupSettings) },
                )

                if (layout.isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SettingsPageGroup(
                                entries = ledgerFeatureEntries,
                                onNavigate = onNavigate,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SettingsPageGroup(
                                entries = preferenceAndSecurityEntries,
                                onNavigate = onNavigate,
                            )
                            SettingsCardGroup(
                                entries = systemAndSupportEntries,
                                versionName = versionName,
                                onEvent = onEvent,
                                onNavigate = onNavigate,
                            )
                        }
                    }
                } else {
                    SettingsPageGroup(
                        entries = ledgerFeatureEntries,
                        onNavigate = onNavigate,
                    )
                    SettingsPageGroup(
                        entries = preferenceAndSecurityEntries,
                        onNavigate = onNavigate,
                    )
                    SettingsCardGroup(
                        entries = systemAndSupportEntries,
                        versionName = versionName,
                        onEvent = onEvent,
                        onNavigate = onNavigate,
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
            expandedWidth -> 28.dp
            else -> 18.dp
        },
    )
}

private val MediumWidth = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp
private val ExpandedWidth = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp
private val MediumHeight = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp

private data class SettingsEntry(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    val event: SettingsUiEvent? = null,
    val destination: NavKey? = null,
    val showsVersion: Boolean = false,
)

private data class SettingsPageEntry(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    val key: NavKey,
)

private val ledgerFeatureEntries = listOf(
    SettingsPageEntry(
        icon = Icons.Outlined.CalendarMonth,
        titleRes = R.string.shortcut_overview,
        key = Overview,
    ),
    SettingsPageEntry(
        icon = Icons.Outlined.Savings,
        titleRes = R.string.shortcut_budget,
        key = Budget,
    ),
    SettingsPageEntry(
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        titleRes = R.string.shortcut_reports,
        key = Reports,
    ),
)

private val preferenceAndSecurityEntries = listOf(
    SettingsPageEntry(
        icon = Icons.Outlined.Palette,
        titleRes = R.string.settings_appearance,
        key = AppearanceSettings,
    ),
    SettingsPageEntry(
        icon = Icons.Outlined.Backup,
        titleRes = R.string.settings_backup,
        key = BackupSettings,
    ),
    SettingsPageEntry(
        icon = Icons.Outlined.Payments,
        titleRes = R.string.settings_more,
        key = MoreSettings,
    ),
    SettingsPageEntry(
        icon = Icons.Outlined.Security,
        titleRes = R.string.settings_security,
        key = SecuritySettings,
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
        icon = Icons.Outlined.Mail,
        titleRes = R.string.settings_feedback,
        event = SettingsUiEvent.SendFeedback,
    ),
    SettingsEntry(
        icon = Icons.Outlined.Info,
        titleRes = R.string.settings_about,
        event = SettingsUiEvent.OpenAboutDialog,
        showsVersion = true,
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
    versionName: String,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionCard(modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) SettingsDivider()
            val destination = entry.destination
            SettingActionItem(
                icon = entry.icon,
                title = stringResource(entry.titleRes),
                valueText = if (entry.showsVersion) "v$versionName" else "",
                onClick = if (destination != null) {
                    dropUnlessResumed { onNavigate(destination) }
                } else {
                    { onEvent(checkNotNull(entry.event)) }
                },
            )
        }
    }
}

private const val FEEDBACK_EMAIL = "lzm.cyr@gmail.com"

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

@Composable
private fun appVersionName(): String {
    if (LocalInspectionMode.current) return "1.0.0"
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
        info.versionName.orEmpty().ifEmpty { "1.0.0" }
    }
}

@FormFactorPreviews
@Composable
private fun SettingsScreenWarmCreamPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            SettingsScreen(
                uiState = SettingsUiState(daysInUse = 128, totalRecordsCount = 342),
                onEvent = {},
                onOpenShortcut = {},
                onOpenSourceLicenses = {},
                onNavigate = {},
            )
        }
    }
}

@Preview(name = "Narrow", widthDp = 320, heightDp = 720, showBackground = true)
@Composable
private fun SettingsScreenNarrowPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            SettingsScreen(
                uiState = SettingsUiState(daysInUse = 128, totalRecordsCount = 342),
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
                uiState = SettingsUiState(daysInUse = 128, totalRecordsCount = 342),
                onEvent = {},
                onOpenShortcut = {},
                onOpenSourceLicenses = {},
                onNavigate = {},
            )
        }
    }
}
