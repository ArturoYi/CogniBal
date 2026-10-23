package com.example.cognibal.screen.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 统计、总览、报表共用的卡片。圆角和阴影跟设置页一致。 */
@Composable
internal fun InsightCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    itemSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
    }
}

/**
 * 按月前后翻。已经是本月时，下一月按钮停用。
 *
 * [onLabelClick] 让中间的月份可点。[compact] 把这一行压到 36.dp，总览的月汇总用它。
 */
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
    val buttonSize = if (compact) 36.dp else 48.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.height(buttonSize) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            description = stringResource(R.string.insight_previous_month),
            enabled = true,
            tint = colors.primary,
            onClick = onPrevious,
            size = buttonSize,
            expandTouchTarget = !compact,
        )
        if (onLabelClick == null) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = if (compact) 15.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(buttonSize)
                    .stepperClick(
                        expandTouchTarget = !compact,
                        description = labelDescription,
                        onClick = onLabelClick,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = colors.textPrimary,
                    fontSize = if (compact) 15.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(if (compact) 18.dp else 20.dp),
                )
            }
        }
        StepperButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            description = stringResource(R.string.insight_next_month),
            enabled = canGoNext,
            tint = colors.primary.copy(alpha = if (canGoNext) 1f else 0.35f),
            onClick = onNext,
            size = buttonSize,
            expandTouchTarget = !compact,
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
    size: Dp = 48.dp,
    expandTouchTarget: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(if (size < 48.dp) 10.dp else 12.dp))
            .stepperClick(enabled = enabled, expandTouchTarget = expandTouchTarget, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(if (size < 48.dp) 20.dp else 22.dp),
        )
    }
}

/**
 * 紧凑月份条一排放不下 48.dp 的最小点击高度，所以 [expandTouchTarget] 为 false 时不再撑高。
 * 键盘焦点仍描一圈。
 */
@Composable
private fun Modifier.stepperClick(
    enabled: Boolean = true,
    expandTouchTarget: Boolean = true,
    description: String? = null,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val clicked = if (expandTouchTarget) {
        interactiveClick(enabled = enabled, onClick = onClick)
    } else {
        val shape = RoundedCornerShape(10.dp)
        val outlined = if (focused && enabled) {
            border(2.dp, MaterialTheme.colorScheme.secondary, shape)
        } else {
            this
        }
        val withPointer = if (enabled) outlined.pointerHoverIcon(PointerIcon.Hand) else outlined
        withPointer.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
    }
    return if (description == null) {
        clicked
    } else {
        clicked.semantics(mergeDescendants = true) { contentDescription = description }
    }
}

/** 支出、收入或时间范围这类互斥选项。 */
@Composable
internal fun InsightSegments(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val track = colors.tagBackground
        .copy(alpha = 0.45f)
        .compositeOver(colors.cardBackground)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(track, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val shape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (selected) colors.cardBackground else Color.Transparent)
                    .interactiveClick(role = Role.RadioButton) { onSelect(index) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.primary else colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 一个月的收入、支出和结余。[compact] 把标签和金额收成更矮的一行。 */
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
    Row(modifier = modifier.fillMaxWidth()) {
        MoneyColumn(
            label = stringResource(R.string.bill_month_income),
            amount = moneyText(currencySymbol, income),
            color = colors.income,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        MoneyColumn(
            label = stringResource(R.string.bill_month_expense),
            amount = moneyText(currencySymbol, expense),
            color = colors.expense,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        MoneyColumn(
            label = stringResource(R.string.bill_month_balance),
            amount = moneyText(currencySymbol, balance, balanceOverflow),
            color = when {
                balanceOverflow -> colors.textSecondary
                balance < 0L -> colors.expense
                else -> colors.income
            },
            compact = compact,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MoneyColumn(
    label: String,
    amount: String,
    color: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = if (compact) 2.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.appColors.textSecondary,
            fontSize = if (compact) 11.sp else 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = amount,
            color = color,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (compact) 1.dp else 6.dp),
        )
    }
}

@Composable
internal fun moneyText(symbol: String, amount: Long, overflow: Boolean = false): String {
    if (overflow) return stringResource(R.string.account_balance_overflow)
    return symbol + formatMinorAmount(amount)
}

@Composable
internal fun formatYearMonth(month: YearMonth): String {
    val locale = currentLocale()
    val pattern = if (locale.language == "zh") "yyyy年M月" else "MMMM yyyy"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return month.format(formatter)
}

@Composable
internal fun formatMonthDay(day: LocalDate): String {
    val locale = currentLocale()
    val pattern = if (locale.language == "zh") "M月d日 EEE" else "EEE, MMM d"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return day.format(formatter)
}

@Composable
internal fun formatMonthShort(month: YearMonth): String {
    val locale = currentLocale()
    val pattern = if (locale.language == "zh") "M月" else "MMM"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return month.format(formatter)
}

@Composable
private fun currentLocale(): Locale {
    return LocalConfiguration.current.locales[0] ?: Locale.getDefault()
}
