package com.example.cognibal.screen.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.model.CurrencyPreference
import com.example.cognibal.screen.feature.settings.model.DarkModePreference
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalCardElevation
import com.example.cognibal.ui.theme.LocalAppThemeStyle
import com.example.cognibal.ui.theme.LocalThemeReveal
import com.example.cognibal.ui.theme.MonochromeDarkAppColors
import com.example.cognibal.ui.theme.MonochromeLightAppColors
import com.example.cognibal.ui.theme.WarmCreamDarkAppColors
import com.example.cognibal.ui.theme.WarmCreamLightAppColors
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.ui.theme.revealOrRun

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
    val days = pluralStringResource(R.plurals.day_count, daysInUse, daysInUse)
    val entries = pluralStringResource(R.plurals.entry_count, totalRecords, totalRecords)

    Text(
        text = stringResource(R.string.user_stats, days, entries),
        color = colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )
}

/**
 * 设置页快捷账务工具入口。
 *
 * 总览、统计、报表、预算、资产打开对应页面。
 */
internal enum class SettingsShortcut(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Overview(R.string.shortcut_overview, Icons.Outlined.CalendarMonth),
    Statistics(R.string.shortcut_statistics, Icons.Outlined.PieChart),
    Reports(R.string.shortcut_reports, Icons.Outlined.BarChart),
    Budget(R.string.shortcut_budget, Icons.Outlined.Savings),
    Assets(R.string.shortcut_assets, Icons.Outlined.AccountBalance),
}

/**
 * 总览、统计、报表、预算、资产排成一行，收在同一张卡片里，横跨这一页的内容宽度。
 *
 * 不再给每个入口单独做卡片，手机上才能留出字距。每格至少 48.dp；更窄时改为横向滚动。
 * 文案固定 12.sp、单行居中，放不下再末尾省略。读屏拿到完整文案。
 */
@Composable
internal fun SettingsShortcutBar(
    onShortcutClick: (SettingsShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.cardBackground,
        shadowElevation = CogniBalCardElevation,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            val shortcuts = SettingsShortcut.entries
            val fits = maxWidth / shortcuts.size >= ShortcutMinWidth
            if (fits) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    shortcuts.forEach { shortcut ->
                        SettingsShortcutItem(
                            shortcut = shortcut,
                            onClick = dropUnlessResumed { onShortcutClick(shortcut) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                ) {
                    shortcuts.forEach { shortcut ->
                        SettingsShortcutItem(
                            shortcut = shortcut,
                            onClick = dropUnlessResumed { onShortcutClick(shortcut) },
                            modifier = Modifier.width(ShortcutMinWidth),
                        )
                    }
                }
            }
        }
    }
}

private val ShortcutMinWidth = 48.dp

@Composable
private fun SettingsShortcutItem(
    shortcut: SettingsShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val iconBg = colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f)
    val label = stringResource(shortcut.labelRes)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .interactiveClick(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
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
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = CogniBalCardElevation,
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

        val mirrorChevron = LocalLayoutDirection.current == LayoutDirection.Rtl
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary.copy(alpha = 0.7f),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { scaleX = if (mirrorChevron) -1f else 1f },
        )
    }
}

/** 设置项之间留白，不再画分割线。 */
@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    Spacer(modifier.height(4.dp))
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
    val systemDark = isSystemInDarkTheme()
    val themeReveal = LocalThemeReveal.current
    var pendingMode by remember { mutableStateOf<DarkModePreference?>(null) }
    SideEffect {
        if (pendingMode == currentPreference) pendingMode = null
    }
    val shownPreference = pendingMode ?: currentPreference
    val currentDark = shownPreference.showsDarkTheme(systemDark)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsItemIcon(icon = Icons.Outlined.Contrast)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.theme),
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(shownPreference.subtitleRes),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        val trackColor = colors.tagBackground
            .copy(alpha = 0.45f)
            .compositeOver(colors.cardBackground)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = trackColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DarkModePreference.entries.forEach { option ->
                val isSelected = option == shownPreference
                val icon = when (option) {
                    DarkModePreference.FOLLOW_SYSTEM -> Icons.Outlined.Contrast
                    DarkModePreference.LIGHT -> Icons.Outlined.LightMode
                    DarkModePreference.DARK -> Icons.Outlined.DarkMode
                }

                val containerColor = if (isSelected) colors.cardBackground else Color.Transparent
                val contentColor = if (isSelected) colors.primary else colors.textSecondary

                val shape = RoundedCornerShape(9.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .shadow(
                            elevation = if (isSelected) 0.5.dp else 0.dp,
                            shape = shape,
                        )
                        .clip(shape)
                        .background(containerColor)
                        .themeOptionClick(shape = shape) { center ->
                            val nextDark = option.showsDarkTheme(systemDark)
                            themeReveal.revealOrRun(
                                centerInRoot = center,
                                colorsWillChange = option != shownPreference && nextDark != currentDark,
                            ) {
                                themeReveal?.hold(dark = nextDark)
                                pendingMode = option
                                onSelectPreference(option)
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(option.titleRes),
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(option.titleRes),
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * 主题选项不画点击水波纹。
 *
 * 亮色切到跟随系统、而系统也是亮色时，主题不会变，水波纹仍会在按钮上闪一下。
 * 键盘焦点仍描一圈，方便看见当前项。
 * 圆心用这一项的中心，手指和键盘都从同一处扩开。
 */
@Composable
private fun Modifier.themeOptionClick(
    shape: Shape,
    onClick: (centerInRoot: Offset) -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val coordinates = remember { CoordinatesHolder() }
    val outlined = if (focused) {
        border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.secondary,
            shape = shape,
        )
    } else {
        this
    }
    return outlined
        .onGloballyPositioned { coordinates.value = it }
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.RadioButton,
            onClick = { onClick(coordinates.value.centerInRoot()) },
        )
}

private class CoordinatesHolder {
    var value: LayoutCoordinates? = null
}

private fun LayoutCoordinates?.centerInRoot(): Offset {
    val coordinates = this
    if (coordinates == null || !coordinates.isAttached) return Offset.Zero
    return coordinates.localToRoot(
        Offset(
            x = coordinates.size.width / 2f,
            y = coordinates.size.height / 2f,
        ),
    )
}

private fun DarkModePreference.showsDarkTheme(systemDark: Boolean): Boolean = when (this) {
    DarkModePreference.FOLLOW_SYSTEM -> systemDark
    DarkModePreference.LIGHT -> false
    DarkModePreference.DARK -> true
}

/**
 * 主题色：柔和蓝、墨色。两套都是同一种颜色贯穿。
 */
@Composable
fun ThemeStyleSelector(
    onSelectStyle: (AppThemeStyle) -> Unit,
    modifier: Modifier = Modifier,
    currentStyle: AppThemeStyle = LocalAppThemeStyle.current,
) {
    val colors = MaterialTheme.appColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsItemIcon(icon = Icons.Outlined.Palette)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.theme_color),
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.theme_color_subtitle),
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
)

@Composable
private fun ThemeStyleCard(
    style: AppThemeStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val themeReveal = LocalThemeReveal.current
    val swatches = when (style) {
        AppThemeStyle.WarmCream -> if (colors.isDark) WarmCreamDarkAppColors else WarmCreamLightAppColors
        AppThemeStyle.Monochrome -> if (colors.isDark) MonochromeDarkAppColors else MonochromeLightAppColors
    }
    val palette = ThemeCardPalette(
        primary = swatches.primary,
        secondary = swatches.tagBackground,
        expense = swatches.chartExpense,
    )

    val shape = RoundedCornerShape(14.dp)
    val containerColor = colors.tagBackground
        .copy(alpha = if (isSelected) 0.55f else 0.2f)
        .compositeOver(colors.cardBackground)
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .themeOptionClick(shape = shape) { center ->
                themeReveal.revealOrRun(
                    centerInRoot = center,
                    colorsWillChange = !isSelected,
                ) {
                    themeReveal?.hold(style = style)
                    onClick()
                }
            }
            .padding(12.dp),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(style.nameRes),
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
                            imageVector = Icons.Outlined.Check,
                            contentDescription = stringResource(R.string.selected),
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
                text = stringResource(style.descriptionRes),
                color = colors.textTertiary,
                fontSize = 11.sp,
            )
    }
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
                text = stringResource(R.string.choose_currency),
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
                            text = stringResource(option.nameRes),
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
                Text(
                    text = stringResource(R.string.cancel),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
    )
}
