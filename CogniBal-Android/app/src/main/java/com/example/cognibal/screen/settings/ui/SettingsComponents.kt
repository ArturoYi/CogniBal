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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
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
 * 分层一：用户陪伴统计徽章。
 *
 * 极简现代的温润陪伴胶囊：左侧记录陪伴天数与记账笔数，右侧展示轻灵的活跃状态。
 */
@Composable
fun UserStatsHeader(
    daysInUse: Int,
    totalRecords: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.primary, CircleShape),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "陪伴记录",
                color = colors.textTertiary,
                fontSize = 12.5.sp,
            )
            Text(
                text = "$daysInUse",
                color = colors.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "天",
                color = colors.textTertiary,
                fontSize = 12.5.sp,
            )
            Text(
                text = "·",
                color = colors.textTertiary.copy(alpha = 0.5f),
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            Text(
                text = "共",
                color = colors.textTertiary,
                fontSize = 12.5.sp,
            )
            Text(
                text = "$totalRecords",
                color = colors.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "笔日常",
                color = colors.textTertiary,
                fontSize = 12.5.sp,
            )
        }

        Surface(
            color = colors.tagBackground.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(colors.primary, CircleShape),
                )
                Text(
                    text = "知余账本",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * 分层二：用户总盈余卡片（年轻现代微渐变版）。
 *
 * - 纯净卡片底搭配自上而下的极淡微渐变与 20.dp 圆角，层次立体高级；
 * - 尺寸精炼克制，集成快捷脱敏按钮；
 * - 底部配有贴心的财务状态轻语，富有年轻亲和力。
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
                .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                .size(28.dp)
                                .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = "总盈余",
                            color = colors.textSecondary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.tagBackground.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isVisible) "隐藏金额" else "显示金额",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            )
                        }
                    } else {
                        Text(
                            text = "••••••",
                            color = colors.textPrimary,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isVisible) "净资产结余健康 · 心中常有余" else "金额隐匿保护中 · 轻触整卡查看",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/**
 * 设置页快捷账务工具入口。
 *
 * 统计和报表：收支结构回顾与周期对账；
 * 预算：计划控制（还能花多少）；
 * 资产：账户管理（盈余落在哪些账户）。
 */
internal enum class SettingsShortcut(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
) {
    Statistics("统计", "分类透视", Icons.Rounded.PieChart, Color(0xFFE86F58)),
    Reports("报表", "周期趋势", Icons.Rounded.BarChart, Color(0xFF3DA37D)),
    Budget("预算", "限额控制", Icons.Rounded.Savings, Color(0xFF3F8BEE)),
    Assets("资产", "账户净值", Icons.Rounded.AccountBalance, Color(0xFF8F63DE)),
}

@Composable
internal fun SettingsShortcutBar(
    onShortcutClick: (SettingsShortcut) -> Unit,
    columns: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsShortcut.entries.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
    val accentBg = shortcut.accentColor.copy(alpha = if (colors.isDark) 0.22f else 0.12f)

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
                        .size(38.dp)
                        .background(accentBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        tint = shortcut.accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = shortcut.label,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = shortcut.subtitle,
                        color = colors.textTertiary,
                        fontSize = 11.5.sp,
                    )
                }
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
                        .background(accentBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        tint = shortcut.accentColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = shortcut.label,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = shortcut.subtitle,
                    color = colors.textTertiary,
                    fontSize = 10.5.sp,
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
 * 设置项：动作行组件（无边框，规整间距，轻量图标背景）
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
    val finalIconBg = iconBackgroundColor ?: colors.tagBackground.copy(alpha = 0.55f)

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
                .size(38.dp)
                .background(finalIconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = finalIconTint,
                modifier = Modifier.size(20.dp),
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
    subtitle: String,
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
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colors.tagBackground.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
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

/**
 * 暗色模式（深浅外观）分段切换选择器（无边框，微材质区分）
 */
@Composable
fun ThemeModeSelector(
    currentPreference: DarkModePreference,
    onSelectPreference: (DarkModePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "深浅外观",
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = currentPreference.subtitle,
            color = colors.textTertiary,
            fontSize = 12.sp,
        )
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
 * 主题风格选择组件（无边框，靠背景微差与轻微标识展示）
 */
@Composable
fun ThemeStyleSelector(
    currentStyle: AppThemeStyle,
    onSelectStyle: (AppThemeStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "主题色彩体系",
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "低饱和温润配色，护眼耐看",
            color = colors.textTertiary,
            fontSize = 12.sp,
        )
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
 * 外观设置弹窗
 */
@Composable
fun AppearanceSettingsDialog(
    preferences: AppPreferences,
    onDarkModeChange: (DarkModePreference) -> Unit,
    onThemeStyleChange: (AppThemeStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "外观设置",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeModeSelector(
                    currentPreference = preferences.darkMode,
                    onSelectPreference = onDarkModeChange,
                )

                SettingsDivider()

                ThemeStyleSelector(
                    currentStyle = preferences.themeStyle,
                    onSelectStyle = onThemeStyleChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "完成", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

/**
 * 备份弹窗：把账单导出为本地 CSV。
 */
@Composable
fun BackupSettingsDialog(
    onExportData: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "备份",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "导出一份本地副本，账单仍留在这台设备上",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                )
                SettingActionItem(
                    icon = Icons.Rounded.CloudUpload,
                    title = "导出账单",
                    subtitle = "保存为 CSV 表格",
                    valueText = "CSV",
                    onClick = {
                        onDismiss()
                        onExportData()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "完成", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}

/**
 * 安全设置弹窗
 */
@Composable
fun SecuritySettingsDialog(
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "安全设置",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "保护你的财务隐私与数据资产",
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                )
                Surface(
                    color = colors.tagBackground.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "总盈余默认脱敏",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已开启。进入设置页总盈余默认显示为 ****，点击即可解开隐匿。",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }

                Surface(
                    color = colors.tagBackground.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "本地私密离线存储",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "全部记账与偏好数据均保存在本地沙盒，无需担心云端泄露。",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "我知道了", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
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
                    subtitle = "当前使用：${preferences.currency.displayName}",
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
                    subtitle = "点击与完成交互时触发微触感",
                    checked = preferences.hapticFeedback,
                    onCheckedChange = onHapticFeedbackChange,
                )

                SettingsDivider()

                SettingActionItem(
                    icon = Icons.Rounded.Storage,
                    title = "清除临时缓存",
                    subtitle = "清理渲染与图表缓存",
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
