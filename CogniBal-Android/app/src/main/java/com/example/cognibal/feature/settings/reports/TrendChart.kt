package com.example.cognibal.screen.feature.settings.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.component.formatMonthShort
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
import com.example.cognibal.screen.feature.settings.reports.state.ReportMonthUi
import com.example.cognibal.ui.theme.appColors
import java.time.YearMonth
import kotlin.math.min

private val PlotHeight = 168.dp
private val BarRadius = 3.dp
private val MinBar = 2.5.dp

/** 各月收入和支出。整段铺开，近 12 个月也不用横滑。 */
@Composable
internal fun TrendChart(
    months: List<ReportMonthUi>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val chartDescription = stringResource(R.string.reports_chart_cd)
    val reveal = remember { Animatable(1f) }
    val skipGrow = remember { booleanArrayOf(true) }
    val axis = remember(months) { months.map { month -> month.month } }
    LaunchedEffect(axis) {
        if (skipGrow[0]) {
            skipGrow[0] = false
            return@LaunchedEffect
        }
        reveal.snapTo(0f)
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        )
    }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = colors.chartIncome, label = stringResource(R.string.bill_month_income))
            LegendItem(color = colors.chartExpense, label = stringResource(R.string.bill_month_expense))
        }
        Spacer(Modifier.height(10.dp))
        val progress = reveal.value
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlotHeight)
                .semantics { contentDescription = chartDescription },
        ) {
            drawTrend(
                months = months,
                incomeColor = colors.chartIncome,
                expenseColor = colors.chartExpense,
                guideColor = colors.divider,
                reveal = progress,
            )
        }
        MonthTicks(months = months)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            color = MaterialTheme.appColors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun MonthTicks(months: List<ReportMonthUi>) {
    val colors = MaterialTheme.appColors
    val compact = months.size > 6
    val today = YearMonth.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        for (month in months) {
            val tick = if (compact) month.month.monthValue.toString() else formatMonthShort(month.month)
            val spoken = formatYearMonth(month.month)
            val current = month.month == today
            Text(
                text = tick,
                color = if (current) colors.textSecondary else colors.textTertiary,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = if (current) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = spoken },
            )
        }
    }
}

private fun DrawScope.drawTrend(
    months: List<ReportMonthUi>,
    incomeColor: Color,
    expenseColor: Color,
    guideColor: Color,
    reveal: Float,
) {
    if (size.width <= 0f || size.height <= 0f || months.isEmpty()) return
    val count = months.size
    val topPad = 4.dp.toPx()
    val baseline = size.height - 1.dp.toPx()
    val plotHeight = (baseline - topPad).coerceAtLeast(0f)
    val slot = size.width / count
    val (barWidth, gap) = barMetrics(slot)
    val radius = BarRadius.toPx()
    val minRise = MinBar.toPx().coerceAtMost(plotHeight)
    var maxAmount = 0L
    for (month in months) {
        if (month.income > maxAmount) maxAmount = month.income
        if (month.expense > maxAmount) maxAmount = month.expense
    }

    drawGuide(y = topPad, color = guideColor, dashed = true)
    drawGuide(y = topPad + plotHeight / 2f, color = guideColor, dashed = true)
    drawGuide(y = baseline, color = guideColor, dashed = false)

    val groupWidth = barWidth * 2f + gap
    for (index in months.indices) {
        val month = months[index]
        val incomeLeft = slot * index + (slot - groupWidth) / 2f
        val expenseLeft = incomeLeft + barWidth + gap
        drawTopRoundedBar(
            color = incomeColor,
            left = incomeLeft,
            top = seriesTop(month.income, maxAmount, baseline, plotHeight, minRise, reveal),
            width = barWidth,
            bottom = baseline,
            radius = radius,
        )
        drawTopRoundedBar(
            color = expenseColor,
            left = expenseLeft,
            top = seriesTop(month.expense, maxAmount, baseline, plotHeight, minRise, reveal),
            width = barWidth,
            bottom = baseline,
            radius = radius,
        )
    }
}

/** 窄屏上两组柱不能超出自己的月份格子，否则相邻月份会叠在一起。 */
private fun DrawScope.barMetrics(slot: Float): Pair<Float, Float> {
    val maxGroup = slot * 0.86f
    var barWidth = (slot * 0.2f).coerceIn(3.dp.toPx(), 12.dp.toPx())
    var gap = (barWidth * 0.38f).coerceIn(1.5.dp.toPx(), 4.dp.toPx())
    if (barWidth * 2f + gap > maxGroup && maxGroup > 0f) {
        gap = (maxGroup * 0.16f).coerceAtLeast(1.dp.toPx())
        barWidth = ((maxGroup - gap) / 2f).coerceAtLeast(1.dp.toPx())
    }
    return barWidth to gap
}

private fun seriesTop(
    amount: Long,
    maxAmount: Long,
    baseline: Float,
    plotHeight: Float,
    minRise: Float,
    reveal: Float,
): Float {
    if (amount <= 0L || maxAmount <= 0L || plotHeight <= 0f) return baseline
    val fraction = amount.toFloat() / maxAmount.toFloat()
    val rise = (plotHeight * fraction).coerceAtLeast(minRise)
    return baseline - rise * reveal.coerceIn(0f, 1f)
}

private fun DrawScope.drawGuide(y: Float, color: Color, dashed: Boolean) {
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = if (dashed) {
            PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()), 0f)
        } else {
            null
        },
    )
}

private fun DrawScope.drawTopRoundedBar(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    bottom: Float,
    radius: Float,
) {
    val height = bottom - top
    if (width <= 0f || height <= 0.5f) return
    val corner = min(radius, min(width / 2f, height))
    val curve = CornerRadius(corner, corner)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = bottom,
                topLeftCornerRadius = curve,
                topRightCornerRadius = curve,
            ),
        )
    }
    drawPath(path, color)
}
