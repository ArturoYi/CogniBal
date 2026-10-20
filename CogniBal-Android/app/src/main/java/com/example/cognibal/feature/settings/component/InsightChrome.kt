package com.example.cognibal.screen.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** 按月前后翻。已经是本月时，下一月按钮停用。 */
@Composable
internal fun MonthStepper(
    label: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            description = stringResource(R.string.insight_previous_month),
            enabled = true,
            tint = colors.primary,
            onClick = onPrevious,
        )
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StepperButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            description = stringResource(R.string.insight_next_month),
            enabled = canGoNext,
            tint = colors.primary.copy(alpha = if (canGoNext) 1f else 0.35f),
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
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .interactiveClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
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

/** 一个月的收入、支出和结余。 */
@Composable
internal fun MoneySummary(
    income: Long,
    expense: Long,
    balance: Long,
    balanceOverflow: Boolean,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(modifier = modifier.fillMaxWidth()) {
        MoneyColumn(
            label = stringResource(R.string.bill_month_income),
            amount = moneyText(currencySymbol, income),
            color = colors.income,
            modifier = Modifier.weight(1f),
        )
        MoneyColumn(
            label = stringResource(R.string.bill_month_expense),
            amount = moneyText(currencySymbol, expense),
            color = colors.expense,
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
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.appColors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = amount,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
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
