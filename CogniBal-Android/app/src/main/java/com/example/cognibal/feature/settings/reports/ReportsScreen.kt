package com.example.cognibal.screen.feature.settings.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.InsightSegments
import com.example.cognibal.screen.feature.settings.component.MoneySummary
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
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
private fun ReportTable(
    months: List<ReportMonthUi>,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    Column {
        months.forEachIndexed { index, month ->
            if (index > 0) {
                Spacer(Modifier.height(16.dp))
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
