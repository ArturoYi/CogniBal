package com.example.cognibal.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.settings.model.CurrencyPreference
import com.example.cognibal.settings.model.DarkModePreference
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.MonochromeLight_Background
import com.example.cognibal.ui.theme.MonochromeLight_Expense
import com.example.cognibal.ui.theme.MonochromeLight_Primary
import com.example.cognibal.ui.theme.MonochromeLight_Secondary
import com.example.cognibal.ui.theme.WarmCreamLight_Background
import com.example.cognibal.ui.theme.WarmCreamLight_Expense
import com.example.cognibal.ui.theme.WarmCreamLight_Primary
import com.example.cognibal.ui.theme.WarmCreamLight_Secondary
import com.example.cognibal.ui.theme.appColors

/**
 * 分组容器卡片
 *
 * 将相关设置项收纳在一个具有柔和背景、圆角及内外边距的卡片内
 */
@Composable
fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Surface(
            color = colors.cardBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = colors.divider.copy(alpha = 0.6f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                content = content,
            )
        }
    }
}

/**
 * 暗色模式（深浅外观）分段切换选择器
 *
 * 包含：跟随系统（默认）、浅色模式、暗色模式
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = currentPreference.subtitle,
            color = colors.textTertiary,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.tagBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DarkModePreference.entries.forEach { option ->
                val isSelected = option == currentPreference
                val icon = when (option) {
                    DarkModePreference.FOLLOW_SYSTEM -> SettingsIcons.AutoMode
                    DarkModePreference.LIGHT -> SettingsIcons.LightMode
                    DarkModePreference.DARK -> SettingsIcons.DarkMode
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
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor,
                    modifier = Modifier.weight(1f),
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = colors.primary.copy(alpha = 0.35f),
                        )
                    } else {
                        null
                    },
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
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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
 * 主题风格选择卡片组
 *
 * 直观展示“暖奶油青绿”和“黑白色”调色板
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
            text = "主题风格色调",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "精调色彩体系，适配浅色与暗色模式",
            color = colors.textTertiary,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))

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
            desc = "极简纯粹 · 黑白美学",
        )
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.divider,
        label = "styleBorder",
    )
    val borderWidth = if (isSelected) 1.8.dp else 1.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.tagBackground.copy(alpha = 0.35f) else colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
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
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(colors.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = SettingsIcons.Check,
                            contentDescription = "已选择",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 颜色小球展示预览
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                        .border(0.5.dp, colors.divider, CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.secondary)
                        .border(0.5.dp, colors.divider, CircleShape),
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.expense)
                        .border(0.5.dp, colors.divider, CircleShape),
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
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.tagBackground.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

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
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

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
 * 设置项：动作与导航行组件
 */
@Composable
fun SettingActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    valueText: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .interactiveClick(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.tagBackground.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

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
                )
            }
        }

        if (valueText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = SettingsIcons.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 分割线组件
 */
@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    HorizontalDivider(
        modifier = modifier.padding(vertical = 4.dp),
        thickness = 0.6.dp,
        color = colors.divider.copy(alpha = 0.7f),
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
            Column {
                CurrencyPreference.entries.forEach { option ->
                    val isSelected = option == selectedCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
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
                Text(text = "取消", color = colors.primary)
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(16.dp),
    )
}
