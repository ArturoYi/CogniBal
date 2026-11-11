package com.cyr.cognobal.screen.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.bill.tool.formatMoneyOrOverflow
import com.cyr.cognobal.ui.format.formatCalendarDay
import com.cyr.cognobal.ui.format.ledgerDateFormatter
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.picker.CogniBalWheelDateTimePicker
import com.cyr.cognobal.ui.picker.WheelDateTime
import com.cyr.cognobal.ui.picker.WheelDateTimeMode
import com.cyr.cognobal.ui.picker.WheelDateTimeRules
import com.cyr.cognobal.ui.privacy.LocalAmountVisibility
import com.cyr.cognobal.ui.privacy.maskIfHidden
import com.cyr.cognobal.ui.theme.appColors
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
internal fun InsightCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    itemSpacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
    }
}

@Composable
internal fun MonthStepper(
    label: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
    labelDescription: String? = null,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            description = stringResource(R.string.insight_previous_month),
            enabled = true,
            tint = colors.textSecondary,
            onClick = onPrevious,
        )
        if (onLabelClick == null) {
            Text(
                text = label,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .then(
                        if (labelDescription != null) {
                            Modifier.semantics(mergeDescendants = true) {
                                contentDescription = labelDescription
                            }
                        } else {
                            Modifier
                        },
                    )
                    .interactiveClick(onClick = onLabelClick),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(18.dp),
                )
            }
        }
        StepperButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            description = stringResource(R.string.insight_next_month),
            enabled = canGoNext,
            tint = colors.textSecondary.copy(alpha = if (canGoNext) 1f else 0.28f),
            onClick = onNext,
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .interactiveClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 柔和内凹胶囊轨道 + 纯白悬浮滑块（参考统计页分段切换器）。
 */
@Composable
internal fun InsightSegments(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(14.dp)
    val thumbShape = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.tagBackground, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp)
                    .clip(thumbShape)
                    .background(if (selected) colors.cardBackground else Color.Transparent)
                    .semantics { this.selected = selected }
                    .interactiveClick(role = Role.RadioButton) { onSelect(index) }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun YearMonthPickerDialog(
    month: YearMonth,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(month) {
        mutableStateOf(WheelDateTime.of(month.year, month.monthValue, 1))
    }
    val now = YearMonth.now()
    val colors = MaterialTheme.appColors
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.cardBackground) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.overview_pick_month),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    CogniBalWheelDateTimePicker(
                        value = value,
                        onValueChange = { value = it },
                        mode = WheelDateTimeMode.YearMonth,
                        minTime = WheelDateTime.of(WheelDateTimeRules.DefaultMinYear, 1, 1),
                        maxTime = WheelDateTime.of(now.year, now.monthValue, 1),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                    }
                    TextButton(
                        onClick = { onConfirm(YearMonth.of(value.year, value.month)) },
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MoneySummary(
    income: Long,
    expense: Long,
    balance: Long,
    balanceOverflow: Boolean,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MoneyColumn(
            label = stringResource(R.string.bill_figure_income),
            amount = maskIfHidden(moneyText(currencySymbol, income), hidden, currencySymbol),
            color = colors.income,
            modifier = Modifier.weight(1f),
        )
        MoneyColumn(
            label = stringResource(R.string.bill_figure_expense),
            amount = maskIfHidden(moneyText(currencySymbol, expense), hidden, currencySymbol),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        MoneyColumn(
            label = stringResource(R.string.bill_figure_balance),
            amount = maskIfHidden(moneyText(currencySymbol, balance, balanceOverflow), hidden, currencySymbol),
            color = if (balanceOverflow) colors.textSecondary else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MoneyColumn(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            color = MaterialTheme.appColors.textTertiary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = amount,
            color = color,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun HeaderCircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .interactiveClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (active) colors.primaryContainer else colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) colors.primary else colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun moneyText(symbol: String, amount: Long, overflow: Boolean = false): String =
    formatMoneyOrOverflow(
        symbol,
        amount,
        overflow,
        stringResource(R.string.account_balance_overflow),
    )

@Composable
internal fun formatYearMonth(month: YearMonth): String {
    val locale = currentLocale()
    val formatter = remember(locale) { locale.ledgerDateFormatter("yyyy年M月", "MMMM yyyy") }
    return month.format(formatter)
}

@Composable
internal fun formatMonthDay(day: LocalDate): String = formatCalendarDay(day, currentLocale())

@Composable
internal fun formatMonthShort(month: YearMonth): String {
    val locale = currentLocale()
    val formatter = remember(locale) { locale.ledgerDateFormatter("M月", "MMM") }
    return month.format(formatter)
}

@Composable
private fun currentLocale(): Locale {
    return LocalConfiguration.current.locales[0] ?: Locale.getDefault()
}
