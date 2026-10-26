package com.example.cognibal.screen.feature.settings.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.expense.model.categoryIcon
import com.example.cognibal.screen.feature.settings.statistics.state.CategoryShareUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsKind
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsSectionUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsUiEvent
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.InsightSegments
import com.example.cognibal.screen.feature.settings.component.MonthStepper
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
import com.example.cognibal.screen.feature.settings.component.moneyText

/** 统计入口。分类占比来自 ViewModel。 */
@Composable
internal fun StatisticsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun StatisticsScreen(
    uiState: StatisticsUiState,
    onEvent: (StatisticsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val allLabel = stringResource(R.string.overview_filter_all)
    val expenseLabel = stringResource(R.string.bill_month_expense)
    val incomeLabel = stringResource(R.string.bill_month_income)
    val balanceLabel = stringResource(R.string.bill_month_balance)
    val centerLabel = when (uiState.kind) {
        StatisticsKind.All -> balanceLabel
        StatisticsKind.Expense -> expenseLabel
        StatisticsKind.Income -> incomeLabel
    }
    val centerColor = when {
        uiState.kind == StatisticsKind.Expense -> colors.expense
        uiState.kind == StatisticsKind.Income -> colors.income
        uiState.totalOverflow -> colors.textSecondary
        uiState.total < 0L -> colors.expense
        else -> colors.income
    }
    val centerAmount = moneyText(uiState.currencySymbol, uiState.total, uiState.totalOverflow)
    SettingsSubpage(
        title = stringResource(R.string.shortcut_statistics),
        onBack = onBack,
        modifier = modifier,
    ) {
        InsightCard {
            MonthStepper(
                label = formatYearMonth(uiState.month),
                canGoNext = uiState.canGoNext,
                onPrevious = { onEvent(StatisticsUiEvent.PreviousMonth) },
                onNext = { onEvent(StatisticsUiEvent.NextMonth) },
            )
            InsightSegments(
                labels = listOf(allLabel, expenseLabel, incomeLabel),
                selectedIndex = uiState.kind.segmentIndex(),
                onSelect = { index ->
                    onEvent(StatisticsUiEvent.SelectKind(index.toStatisticsKind()))
                },
            )
            val chart = remember(uiState.slices, centerLabel, centerAmount, centerColor) {
                CategoryDonutChart(
                    slices = uiState.slices,
                    centerLabel = centerLabel,
                    centerAmount = centerAmount,
                    centerColor = centerColor,
                )
            }
            CategoryDonut(chart)
        }
        if (!uiState.loading && uiState.sections.isEmpty()) {
            Text(
                text = stringResource(R.string.insight_empty),
                color = colors.textTertiary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        uiState.sections.forEach { section ->
            StatisticsSection(
                section = section,
                showTitle = uiState.kind == StatisticsKind.All,
                currencySymbol = uiState.currencySymbol,
            )
        }
    }
}

@Composable
private fun StatisticsSection(
    section: StatisticsSectionUi,
    showTitle: Boolean,
    currencySymbol: String,
) {
    if (section.kind == StatisticsKind.All) return
    val titleRes = when (section.kind) {
        StatisticsKind.Expense -> R.string.statistics_section_expense
        StatisticsKind.Income -> R.string.statistics_section_income
        StatisticsKind.All -> return
    }
    val title = stringResource(titleRes)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showTitle) {
            Text(
                text = title,
                color = MaterialTheme.appColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        InsightCard {
            section.categories.forEach { share ->
                CategoryShareRow(
                    share = share,
                    currencySymbol = currencySymbol,
                )
            }
        }
    }
}

private fun StatisticsKind.segmentIndex(): Int = when (this) {
    StatisticsKind.All -> 0
    StatisticsKind.Expense -> 1
    StatisticsKind.Income -> 2
}

private fun Int.toStatisticsKind(): StatisticsKind = when (this) {
    0 -> StatisticsKind.All
    1 -> StatisticsKind.Expense
    else -> StatisticsKind.Income
}

@Composable
private fun CategoryShareRow(
    share: CategoryShareUi,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(share.colorKey)
    val title = if (share.missing || share.name == null) {
        stringResource(R.string.bill_category_missing)
    } else {
        share.name
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    tint.copy(alpha = if (colors.isDark) 0.22f else 0.16f),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(share.iconKey),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = moneyText(currencySymbol, share.amount),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = percentText(share.ratio),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            LinearProgressIndicator(
                progress = { share.ratio },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = tint,
                trackColor = colors.tagBackground,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun percentText(ratio: Float): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val format = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }
    }
    return format.format(ratio.toDouble())
}

@FormFactorPreviews
@Composable
private fun StatisticsScreenPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            val categories = previewExpenseShares()
            StatisticsScreen(
                uiState = StatisticsUiState(
                    loading = false,
                    month = YearMonth.of(2026, 10),
                    kind = StatisticsKind.Expense,
                    total = categories.sumOf { share -> share.amount },
                    slices = chartSlices(categories),
                    sections = listOf(StatisticsSectionUi(StatisticsKind.Expense, categories)),
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun StatisticsEmptyPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            StatisticsScreen(
                uiState = StatisticsUiState(
                    loading = false,
                    month = YearMonth.of(2026, 10),
                    kind = StatisticsKind.Expense,
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun StatisticsAllPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            val expense = previewExpenseShares().take(3)
            val income = listOf(
                CategoryShareUi(11, "Salary", false, "payments", "#FF0CA678", 32_000L, 0.8f),
                CategoryShareUi(12, "Refund", false, "replay", "#FFFD7E14", 8_000L, 0.2f),
            )
            StatisticsScreen(
                uiState = StatisticsUiState(
                    loading = false,
                    month = YearMonth.of(2026, 10),
                    kind = StatisticsKind.All,
                    total = 40_000L - expense.sumOf { share -> share.amount },
                    slices = comparisonSlices(expense = expense.sumOf { share -> share.amount }, income = 40_000L),
                    sections = listOf(
                        StatisticsSectionUi(StatisticsKind.Expense, expense),
                        StatisticsSectionUi(StatisticsKind.Income, income),
                    ),
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

/** 八个分类，用来看图上合并成七块之后，列表仍然逐条显示。 */
private fun previewExpenseShares(): List<CategoryShareUi> = listOf(
    CategoryShareUi(1, "Food", false, "restaurant", "#FFF03E3E", 12_800L, 0.30f),
    CategoryShareUi(2, "Transit", false, "directions_bus", "#FF0CA678", 8_450L, 0.20f),
    CategoryShareUi(3, "Shopping", false, "shopping_bag", "#FFFD7E14", 6_400L, 0.15f),
    CategoryShareUi(4, "Rent", false, "home", "#FF228BE6", 5_200L, 0.12f),
    CategoryShareUi(5, "Fun", false, "movie", "#FFBE4BDB", 3_100L, 0.07f),
    CategoryShareUi(6, "Health", false, "medication", "#FF37B24D", 2_400L, 0.06f),
    CategoryShareUi(7, "Pets", false, "pets", "#FFF59F00", 1_800L, 0.05f),
    CategoryShareUi(8, "Gifts", false, "redeem", "#FFE64980", 1_500L, 0.04f),
)
