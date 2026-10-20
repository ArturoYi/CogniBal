package com.example.cognibal.screen.feature.settings.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.reports.state.ReportMonthUi
import com.example.cognibal.screen.feature.settings.reports.state.ReportSpan
import com.example.cognibal.screen.feature.settings.reports.state.ReportsUiEvent
import com.example.cognibal.screen.feature.settings.reports.state.ReportsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.YearMonth
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.InsightSegments
import com.example.cognibal.screen.feature.settings.component.MoneySummary
import com.example.cognibal.screen.feature.settings.component.formatMonthShort
import com.example.cognibal.screen.feature.settings.component.formatYearMonth

/** 报表入口。月份趋势来自 ViewModel。 */
@Composable
internal fun ReportsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun ReportsScreen(
    uiState: ReportsUiState,
    onEvent: (ReportsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubpage(
        title = stringResource(R.string.shortcut_reports),
        onBack = onBack,
        modifier = modifier,
    ) {
        InsightCard {
            InsightSegments(
                labels = listOf(
                    stringResource(R.string.reports_span_six),
                    stringResource(R.string.reports_span_twelve),
                ),
                selectedIndex = if (uiState.span == ReportSpan.SixMonths) 0 else 1,
                onSelect = { index ->
                    onEvent(
                        ReportsUiEvent.SelectSpan(
                            if (index == 0) ReportSpan.SixMonths else ReportSpan.TwelveMonths,
                        ),
                    )
                },
            )
            if (uiState.months.isNotEmpty()) {
                TrendChart(months = uiState.months)
            }
        }
        if (uiState.months.isNotEmpty()) {
            InsightCard {
                ReportTable(
                    months = uiState.months.asReversed(),
                    currencySymbol = uiState.currencySymbol,
                )
            }
        }
    }
}

@Composable
private fun TrendChart(months: List<ReportMonthUi>) {
    val colors = MaterialTheme.appColors
    val chartDescription = stringResource(R.string.reports_chart_cd)
    val scrollState = rememberScrollState()
    LaunchedEffect(months.size) {
        repeat(2) { withFrameNanos { } }
        scrollState.scrollTo(scrollState.maxValue)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = colors.income, label = stringResource(R.string.bill_month_income))
            LegendItem(color = colors.expense, label = stringResource(R.string.bill_month_expense))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .semantics { contentDescription = chartDescription },
            verticalAlignment = Alignment.Bottom,
        ) {
            val maxAmount = months.maxOf { month -> maxOf(month.income, month.expense) }.coerceAtLeast(1L)
            months.forEach { month ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.divider),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            TrendBar(amount = month.income, maxAmount = maxAmount, color = colors.income)
                            TrendBar(amount = month.expense, maxAmount = maxAmount, color = colors.expense)
                        }
                    }
                    Text(
                        text = formatMonthShort(month.month),
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBar(
    amount: Long,
    maxAmount: Long,
    color: Color,
) {
    val fraction = amount.toFloat() / maxAmount.toFloat()
    val height = if (amount <= 0L) 0.dp else (96f * fraction).dp.coerceAtLeast(3.dp)
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(height)
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(color),
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
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
private fun ReportTable(
    months: List<ReportMonthUi>,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    Column {
        months.forEachIndexed { index, month ->
            if (index > 0) {
                HorizontalDivider(
                    color = colors.divider.copy(alpha = 0.45f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Text(
                text = formatYearMonth(month.month),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MoneySummary(
                income = month.income,
                expense = month.expense,
                balance = month.balance,
                balanceOverflow = month.balanceOverflow,
                currencySymbol = currencySymbol,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun ReportsScreenPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            ReportsScreen(
                uiState = ReportsUiState(
                    loading = false,
                    span = ReportSpan.SixMonths,
                    months = listOf(
                        ReportMonthUi(YearMonth.of(2026, 5), 9_200_00L, 6_100_00L, 3_100_00L, false),
                        ReportMonthUi(YearMonth.of(2026, 6), 8_400_00L, 7_800_00L, 600_00L, false),
                        ReportMonthUi(YearMonth.of(2026, 7), 12_000_00L, 5_400_00L, 6_600_00L, false),
                        ReportMonthUi(YearMonth.of(2026, 8), 7_600_00L, 9_100_00L, -1_500_00L, false),
                        ReportMonthUi(YearMonth.of(2026, 9), 10_200_00L, 4_800_00L, 5_400_00L, false),
                        ReportMonthUi(YearMonth.of(2026, 10), 18_000_00L, 6_420_00L, 11_580_00L, false),
                    ),
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
