package com.example.cognibal.screen.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.screen.settings.model.AppPreferences
import com.example.cognibal.screen.settings.model.CurrencyPreference
import com.example.cognibal.screen.settings.model.DarkModePreference
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.MonochromeLight_Expense
import com.example.cognibal.ui.theme.MonochromeLight_Primary
import com.example.cognibal.ui.theme.MonochromeLight_Secondary
import com.example.cognibal.ui.theme.WarmCreamLight_Expense
import com.example.cognibal.ui.theme.WarmCreamLight_Primary
import com.example.cognibal.ui.theme.WarmCreamLight_Secondary
import com.example.cognibal.ui.theme.appColors

/**
 * 分层一：用户陪伴统计文案。
 */
@Composable
fun UserStatsHeader(
    daysInUse: Int,
    totalRecords: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Text(
        text = "知余已经陪伴你 ${daysInUse}天，共记账 ${totalRecords}笔。",
        color = colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )
}

/**
 * 分层二：用户总盈余卡片。
 */
@Composable
fun SurplusCard(
    surplus: Double,
    isVisible: Boolean,
    currencySymbol: String,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .interactiveClick(onClick = onToggleVisibility),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.primaryContainer.copy(alpha = if (colors.isDark) 0.16f else 0.28f),
                            colors.cardBackground,
                        ),
                    ),
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        Text(
                            text = "总盈余",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isVisible) "隐藏金额" else "显示金额",
                            tint = colors.primary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = isVisible,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                    label = "surplusAmount",
                ) { visible ->
                    if (visible) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = currencySymbol,
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp, end = 4.dp),
                            )
                            Text(
                                text = "%,.2f".format(surplus),
                                color = colors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            )
                        }
                    } else {
                        Text(
                            text = "••••••",
                            color = colors.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设置页快捷账务工具入口。
 */
internal enum class SettingsShortcut(
    val label: String,
    val icon: ImageVector,
) {
    Statistics("统计", Icons.Rounded.PieChart),
    Reports("报表", Icons.Rounded.BarChart),
    Budget("预算", Icons.Rounded.Savings),
    Assets("资产", Icons.Rounded.AccountBalance),
}

@Composable
internal fun SettingsShortcutBar(
    onShortcutClick: (SettingsShortcut) -> Unit,
    columns: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsShortcut.entries.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { shortcut ->
                    SettingsShortcutItem(
                        shortcut = shortcut,
                        isGrid = columns <= 2,
                        onClick = { onShortcutClick(shortcut) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsShortcutItem(
    shortcut: SettingsShortcut,
    isGrid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(16.dp)
    val iconBg = colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f)

    Surface(
        modifier = modifier
            .clip(shape)
            .interactiveClick(onClick = onClick),
        shape = shape,
        color = colors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        if (isGrid) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = shortcut.label,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = shortcut.label,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 分层三：垂直功能卡片容器
 *
 * 统一 20.dp 圆角、纯色卡片底与细腻微阴影，彻底去除边框
 */
@Composable
fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            content = content,
        )
    }
}

/**
 * 设置项：动作行组件（无边框，规整间距，轻量主题色图标背景）
 */
@Composable
fun SettingActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    valueText: String = "",
    iconTint: Color? = null,
    iconBackgroundColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val finalIconTint = iconTint ?: colors.primary
    val finalIconBg = iconBackgroundColor ?: colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .interactiveClick(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(finalIconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = finalIconTint,
                modifier = Modifier.size(19.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        if (valueText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = colors.tagBackground.copy(alpha = 0.45f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = valueText,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 设置项：开关行组件
 */
@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.textTertiary,
                uncheckedTrackColor = colors.tagBackground,
            ),
        )
    }
}

/**
 * 细微分割线组件（轻柔中性灰微弱划分，无突兀边框感）
 */
@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    HorizontalDivider(
        modifier = modifier.padding(vertical = 2.dp),
        thickness = 0.5.dp,
        color = colors.divider.copy(alpha = 0.45f),
    )
}

@Composable
private fun SettingsItemIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .size(36.dp)
            .background(
                colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

/**
 * 主题：跟随系统、浅色、暗色。
 */
@Composable
fun ThemeModeSelector(
    currentPreference: DarkModePreference,
    onSelectPreference: (DarkModePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsItemIcon(icon = Icons.Rounded.Contrast)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "主题",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentPreference.subtitle,
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.tagBackground.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DarkModePreference.entries.forEach { option ->
                val isSelected = option == currentPreference
                val icon = when (option) {
                    DarkModePreference.FOLLOW_SYSTEM -> Icons.Rounded.Contrast
                    DarkModePreference.LIGHT -> Icons.Rounded.LightMode
                    DarkModePreference.DARK -> Icons.Rounded.DarkMode
                }

                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) colors.cardBackground else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "modeContainer",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else colors.textSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "modeContent",
                )

                Surface(
                    onClick = { onSelectPreference(option) },
                    shape = RoundedCornerShape(9.dp),
                    color = containerColor,
                    modifier = Modifier.weight(1f),
                    shadowElevation = if (isSelected) 0.5.dp else 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = option.title,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option.title,
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 主题色：暖奶油青绿、黑白色。
 */
@Composable
fun ThemeStyleSelector(
    currentStyle: AppThemeStyle,
    onSelectStyle: (AppThemeStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsItemIcon(icon = Icons.Rounded.Palette)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "主题色",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "低饱和温润配色，护眼耐看",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppThemeStyle.entries.forEach { style ->
                val isSelected = style == currentStyle
                ThemeStyleCard(
                    style = style,
                    isSelected = isSelected,
                    onClick = { onSelectStyle(style) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private data class ThemeCardPalette(
    val primary: Color,
    val secondary: Color,
    val expense: Color,
    val desc: String,
)

@Composable
private fun ThemeStyleCard(
    style: AppThemeStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val palette = when (style) {
        AppThemeStyle.WarmCream -> ThemeCardPalette(
            primary = WarmCreamLight_Primary,
            secondary = WarmCreamLight_Secondary,
            expense = WarmCreamLight_Expense,
            desc = "温润护眼 · 暖奶油底",
        )
        AppThemeStyle.Monochrome -> ThemeCardPalette(
            primary = MonochromeLight_Primary,
            secondary = MonochromeLight_Secondary,
            expense = MonochromeLight_Expense,
            desc = "极简克制 · 柔和墨灰",
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) colors.tagBackground.copy(alpha = 0.55f) else colors.tagBackground.copy(alpha = 0.2f),
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = style.displayName,
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "已选择",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.primary),
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.secondary),
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.expense),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = palette.desc,
                color = colors.textTertiary,
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * 更多功能弹窗（货币符号、触感反馈、清理缓存）
 */
@Composable
fun MoreSettingsDialog(
    preferences: AppPreferences,
    cacheSizeText: String,
    onOpenCurrencySelect: () -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onOpenClearCache: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "更多功能",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingActionItem(
                    icon = Icons.Rounded.Payments,
                    title = "默认货币符号",
                    valueText = preferences.currency.symbol,
                    onClick = {
                        onDismiss()
                        onOpenCurrencySelect()
                    },
                )

                SettingsDivider()

                SettingSwitchItem(
                    icon = Icons.Rounded.Vibration,
                    title = "操作触感反馈",
                    checked = preferences.hapticFeedback,
                    onCheckedChange = onHapticFeedbackChange,
                )

                SettingsDivider()

                SettingActionItem(
                    icon = Icons.Rounded.Storage,
                    title = "清除临时缓存",
                    valueText = cacheSizeText,
                    onClick = {
                        onDismiss()
                        onOpenClearCache()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "关闭", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

/**
 * 货币符号单选选择弹窗
 */
@Composable
fun CurrencySelectDialog(
    selectedCurrency: CurrencyPreference,
    onSelect: (CurrencyPreference) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择默认记账货币",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CurrencyPreference.entries.forEach { option ->
                    val isSelected = option == selectedCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.textTertiary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.displayName,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}
