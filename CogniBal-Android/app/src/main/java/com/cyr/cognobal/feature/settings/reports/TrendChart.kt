package com.cyr.cognobal.screen.feature.settings.reports

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.settings.component.formatMonthShort
import com.cyr.cognobal.screen.feature.settings.component.formatYearMonth
import com.cyr.cognobal.screen.feature.settings.reports.state.ReportMonthUi
import com.cyr.cognobal.ui.theme.appColors
import java.time.YearMonth

private val PlotHeight = 148.dp

/**
 * 参考设计稿重构的月份竖向胶囊轨道图：
 * 全高柔和浅灰胶囊轨道 + 收支圆润胶囊柱 + 当月空心圆环标记，彻底去除虚线网格干扰。
 */
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
            LegendItem(color = colors.chartIncome, label = stringResource(R.string.bill_figure_income))
            LegendItem(color = colors.chartExpense, label = stringResource(R.string.bill_figure_expense))
        }
        Spacer(Modifier.height(16.dp))
        val progress = reveal.value
        val now = YearMonth.now()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlotHeight)
                .semantics { contentDescription = chartDescription },
        ) {
            if (size.width <= 0f || size.height <= 0f || months.isEmpty()) return@Canvas
            val count = months.size
            val slot = size.width / count
            val trackWidth = if (count > 6) {
                (slot * 0.34f).coerceIn(5.dp.toPx(), 8.dp.toPx())
            } else {
                8.dp.toPx()
            }
            val radius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
            val trackHeight = size.height
            var maxTotal = 1L
            for (m in months) {
                val sum = (m.income + m.expense).coerceAtLeast(0L)
                if (sum > maxTotal) maxTotal = sum
            }

            for (index in months.indices) {
                val month = months[index]
                val centerX = slot * index + slot / 2f
                val left = centerX - trackWidth / 2f

                // 全高柔和胶囊轨道
                drawRoundRect(
                    color = colors.tagBackground,
                    topLeft = Offset(left, 0f),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = radius,
                )

                val total = (month.income + month.expense).coerceAtLeast(0L)
                val fraction = if (total <= 0L) {
                    0f
                } else {
                    (total.toFloat() / maxTotal.toFloat()).coerceIn(0.10f, 1f) * progress
                }
                val activeHeight = trackHeight * fraction
                val activeTop = trackHeight - activeHeight

                if (total > 0L && activeHeight > 1f) {
                    drawRoundRect(
                        color = if (month.expense > 0L) colors.chartExpense else colors.chartIncome,
                        topLeft = Offset(left, activeTop),
                        size = Size(trackWidth, activeHeight),
                        cornerRadius = radius,
                    )
                    if (month.income > 0L && month.expense > 0L) {
                        val incomeHeight = (activeHeight * (month.income.toFloat() / total.toFloat()))
                            .coerceAtLeast(trackWidth)
                        drawRoundRect(
                            color = colors.chartIncome,
                            topLeft = Offset(left, trackHeight - incomeHeight),
                            size = Size(trackWidth, incomeHeight),
                            cornerRadius = radius,
                        )
                    }
                }

                // 当前月份空心圆环标记
                if (month.month == now) {
                    val ringY = if (total > 0L) {
                        activeTop.coerceIn(8.dp.toPx(), trackHeight - 10.dp.toPx())
                    } else {
                        trackHeight - 12.dp.toPx()
                    }
                    val ringCenter = Offset(centerX, ringY)
                    drawCircle(
                        color = colors.primary,
                        radius = 6.dp.toPx(),
                        center = ringCenter,
                    )
                    drawCircle(
                        color = colors.cardBackground,
                        radius = 3.2.dp.toPx(),
                        center = ringCenter,
                    )
                }
            }
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
            style = MaterialTheme.typography.labelMedium,
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
            .padding(top = 10.dp),
    ) {
        for (month in months) {
            val tick = if (compact) month.month.monthValue.toString() else formatMonthShort(month.month)
            val spoken = formatYearMonth(month.month)
            val current = month.month == today
            Text(
                text = tick,
                color = if (current) colors.textPrimary else colors.textTertiary,
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
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
