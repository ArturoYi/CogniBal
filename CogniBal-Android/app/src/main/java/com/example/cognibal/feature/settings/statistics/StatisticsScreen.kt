package com.example.cognibal.screen.feature.settings.statistics

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
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
    val expenseLabel = stringResource(R.string.bill_month_expense)
    val incomeLabel = stringResource(R.string.bill_month_income)
    val kindLabel = if (uiState.kind == StatisticsKind.Expense) expenseLabel else incomeLabel
    val amountColor = if (uiState.kind == StatisticsKind.Expense) colors.expense else colors.income
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
                labels = listOf(expenseLabel, incomeLabel),
                selectedIndex = if (uiState.kind == StatisticsKind.Expense) 0 else 1,
                onSelect = { index ->
                    onEvent(
                        if (index == 0) {
                            StatisticsUiEvent.ShowExpense
                        } else {
                            StatisticsUiEvent.ShowIncome
                        },
                    )
                },
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = kindLabel,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
                Text(
                    text = moneyText(uiState.currencySymbol, uiState.total),
                    color = amountColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
        if (!uiState.loading && uiState.categories.isEmpty()) {
            Text(
                text = stringResource(R.string.insight_empty),
                color = colors.textTertiary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else if (uiState.categories.isNotEmpty()) {
            InsightCard {
                uiState.categories.forEach { share ->
                    CategoryShareRow(
                        share = share,
                        currencySymbol = uiState.currencySymbol,
                    )
                }
            }
        }
    }
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
            StatisticsScreen(
                uiState = StatisticsUiState(
                    loading = false,
                    month = YearMonth.of(2026, 10),
                    kind = StatisticsKind.Expense,
                    total = 27_650L,
                    categories = listOf(
                        CategoryShareUi(1, "Food", false, "restaurant", "#FFD96C6C", 12_800L, 0.46f),
                        CategoryShareUi(2, "Transit", false, "directions_bus", "#FF78A898", 8_450L, 0.31f),
                        CategoryShareUi(3, "Shopping", false, "shopping_bag", "#FFE0A15A", 6_400L, 0.23f),
                    ),
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
