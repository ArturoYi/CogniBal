package com.cyr.cognobal.screen.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.bill.AccountHeroSurface
import com.cyr.cognobal.screen.feature.bill.PressFade
import com.cyr.cognobal.screen.feature.settings.model.CurrencyPreference
import com.cyr.cognobal.screen.feature.settings.model.DarkModePreference
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.LocalAppThemeStyle
import com.cyr.cognobal.ui.theme.LocalThemeReveal
import com.cyr.cognobal.ui.theme.SelectableAccentStyles
import com.cyr.cognobal.ui.theme.accentInk
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.revealOrRun
import com.cyr.cognobal.ui.theme.sameAccent

/**
 * 无账号本地账本专属的曜石深灰伴随卡：
 * 不做虚假头像与 VIP，而以大号等宽数字凝练呈现陪伴天数与累计笔数，右侧配香槟金备份胶囊按钮。
 */
@Composable
fun UserStatsHeader(
    daysInUse: Int,
    totalRecords: Int,
    onBackupClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val daysFull = pluralStringResource(R.plurals.day_count, daysInUse, daysInUse)
    val entriesFull = pluralStringResource(R.plurals.entry_count, totalRecords, totalRecords)
    val fullSemantic = stringResource(R.string.user_stats, daysFull, entriesFull)

    AccountHeroSurface(modifier = modifier) { paint ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = fullSemantic },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                HeroMetricBlock(
                    number = daysInUse,
                    formatted = daysFull,
                    valueColor = paint.on,
                    unitColor = paint.on.copy(alpha = 0.65f),
                )
                Box(
                    Modifier
                        .padding(bottom = 6.dp)
                        .width(1.dp)
                        .height(22.dp)
                        .background(paint.on.copy(alpha = 0.16f)),
                )
                HeroMetricBlock(
                    number = totalRecords,
                    formatted = entriesFull,
                    valueColor = paint.on,
                    unitColor = paint.on.copy(alpha = 0.65f),
                )
            }

            PressFade(
                onClick = onBackupClick,
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .clip(CircleShape)
                    .background(paint.identity),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Backup,
                        contentDescription = null,
                        tint = Color(0xFF18191E),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.backup_save),
                        color = Color(0xFF18191E),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricBlock(
    number: Int,
    formatted: String,
    valueColor: Color,
    unitColor: Color,
) {
    val digits = number.toString()
    val unit = formatted.replaceFirst(digits, "").trim()
    Row(verticalAlignment = Alignment.Bottom) {
        if (formatted.contains(digits) && unit.isNotEmpty()) {
            Text(
                text = digits,
                color = valueColor,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit,
                color = unitColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline(),
            )
        } else {
            Text(
                text = formatted,
                color = valueColor,
                style = MaterialTheme.typography.titleLarge.merge(
                    TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal enum class SettingsShortcut(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Overview(R.string.shortcut_overview, Icons.Outlined.CalendarMonth),
    Statistics(R.string.shortcut_statistics, Icons.Outlined.PieChart),
    Reports(R.string.shortcut_reports, Icons.AutoMirrored.Outlined.TrendingUp),
    Budget(R.string.shortcut_budget, Icons.Outlined.Savings),
    Assets(R.string.shortcut_assets, Icons.Outlined.AccountBalance),
}

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
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            content = content,
        )
    }
}

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
    val finalIconTint = iconTint ?: colors.textPrimary
    val finalIconBg = iconBackgroundColor ?: colors.tagBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(MaterialTheme.shapes.medium)
            .interactiveClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(finalIconBg, RoundedCornerShape(11.dp)),
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
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (valueText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                color = colors.textTertiary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

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

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = 58.dp, end = 10.dp)
            .fillMaxWidth()
            .height(0.8.dp)
            .background(MaterialTheme.appColors.divider),
    )
}

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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DarkModePreference.entries.forEach { option ->
            val isSelected = option == shownPreference
            val icon = when (option) {
                DarkModePreference.FOLLOW_SYSTEM -> Icons.Outlined.Contrast
                DarkModePreference.LIGHT -> Icons.Outlined.LightMode
                DarkModePreference.DARK -> Icons.Outlined.DarkMode
            }
            val containerColor = if (isSelected) colors.inkStrong else colors.background
            val contentColor = if (isSelected) colors.onInkStrong else colors.textSecondary
            val shape = RoundedCornerShape(15.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(shape)
                    .background(containerColor)
                    .semantics { selected = isSelected }
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
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(option.titleRes),
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

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

@Composable
fun ThemeStyleSelector(
    onSelectStyle: (AppThemeStyle) -> Unit,
    modifier: Modifier = Modifier,
    currentStyle: AppThemeStyle = LocalAppThemeStyle.current,
) {
    val rows = SelectableAccentStyles.chunked(3)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowStyles.forEach { style ->
                    AccentSwatchItem(
                        style = style,
                        isSelected = style.sameAccent(currentStyle),
                        onClick = { onSelectStyle(style) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowStyles.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AccentSwatchItem(
    style: AppThemeStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val themeReveal = LocalThemeReveal.current
    val ink = style.accentInk(colors.isDark)
    val label = stringResource(style.nameRes)
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(if (isSelected) colors.background else Color.Transparent)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                selected = isSelected
            }
            .themeOptionClick(shape = shape) { center ->
                themeReveal.revealOrRun(
                    centerInRoot = center,
                    colorsWillChange = !isSelected,
                ) {
                    themeReveal?.hold(style = style)
                    onClick()
                }
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .border(2.dp, ink.primary, CircleShape)
                            .padding(3.dp)
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(ink.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = ink.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

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
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyPreference.entries.forEach { option ->
                    val isSelected = option == selectedCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isSelected) colors.primaryContainer else colors.background)
                            .semantics { selected = isSelected }
                            .interactiveClick(
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(option.nameRes),
                            color = if (isSelected) colors.primary else colors.textPrimary,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
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
        shape = MaterialTheme.shapes.extraLarge,
    )
}
