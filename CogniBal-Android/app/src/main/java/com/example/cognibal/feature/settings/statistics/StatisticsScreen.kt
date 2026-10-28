package com.example.cognibal.screen.feature.settings.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.bill.PressFade
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.InsightSegments
import com.example.cognibal.screen.feature.settings.component.MonthStepper
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
import com.example.cognibal.screen.feature.settings.component.moneyText
import com.example.cognibal.screen.feature.settings.statistics.state.CategoryShareUi
import com.example.cognibal.screen.feature.settings.statistics.state.StatisticsBillUi
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
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

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
    var openCategoryId by rememberSaveable { mutableLongStateOf(NoSelection) }
    var openKindName by rememberSaveable { mutableStateOf("") }
    var openBillId by rememberSaveable { mutableLongStateOf(NoSelection) }
    val openKind = openKindName.toStatisticsKindOrNull()
    val openShare = if (openKind == null || openCategoryId == NoSelection) {
        null
    } else {
        uiState.sections
            .firstOrNull { section -> section.kind == openKind }
            ?.categories
            ?.firstOrNull { share -> share.id == openCategoryId }
    }
    val openBills = if (openShare == null || openKind == null) {
        emptyList()
    } else {
        uiState.bills.filter { bill -> bill.categoryId == openShare.id && bill.kind == openKind }
    }
    val openBill = openBills.firstOrNull { bill -> bill.id == openBillId }
    LaunchedEffect(uiState.loading, openCategoryId, openShare, openBillId, openBill) {
        if (uiState.loading || openCategoryId == NoSelection) return@LaunchedEffect
        if (openShare == null) {
            openCategoryId = NoSelection
            openKindName = ""
            openBillId = NoSelection
        } else if (openBillId != NoSelection && openBill == null) {
            openBillId = NoSelection
        }
    }
    SettingsSubpage(
        title = stringResource(R.string.shortcut_statistics),
        onBack = onBack,
        modifier = modifier,
    ) {
        StatisticsBody(
            uiState = uiState,
            onEvent = onEvent,
            onOpenCategory = { kind, id ->
                openKindName = kind.name
                openCategoryId = id
                openBillId = NoSelection
            },
        )
    }
    if (openShare != null && openKind != null) {
        StatisticsBillSheet(
            share = openShare,
            bills = openBills,
            currencySymbol = uiState.currencySymbol,
            billId = openBill?.id,
            onOpenBill = { id -> openBillId = id },
            onBackToList = { openBillId = NoSelection },
            onDismiss = {
                openCategoryId = NoSelection
                openKindName = ""
                openBillId = NoSelection
            },
        )
    }
}

@Composable
private fun StatisticsBody(
    uiState: StatisticsUiState,
    onEvent: (StatisticsUiEvent) -> Unit,
    onOpenCategory: (StatisticsKind, Long) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sideBySide = maxWidth >= SideBySideWidth && uiState.sections.isNotEmpty()
        if (sideBySide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ChartCard(
                    uiState = uiState,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1.2f),
                )
                StatisticsSections(
                    uiState = uiState,
                    onOpenCategory = onOpenCategory,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartCard(uiState = uiState, onEvent = onEvent)
                if (!uiState.loading && uiState.sections.isEmpty()) {
                    EmptyHint()
                }
                if (uiState.sections.isNotEmpty()) {
                    StatisticsSections(
                        uiState = uiState,
                        onOpenCategory = onOpenCategory,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    uiState: StatisticsUiState,
    onEvent: (StatisticsUiEvent) -> Unit,
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
    val centerColor = if (uiState.totalOverflow) colors.textSecondary else colors.textPrimary
    val centerAmount = moneyText(uiState.currencySymbol, uiState.total, uiState.totalOverflow)
    val chart = remember(uiState.slices, centerLabel, centerAmount, centerColor) {
        CategoryDonutChart(
            slices = uiState.slices,
            centerLabel = centerLabel,
            centerAmount = centerAmount,
            centerColor = centerColor,
        )
    }
    val billCount = if (!uiState.loading && uiState.bills.isNotEmpty()) uiState.bills.size else null
    InsightCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        itemSpacing = 8.dp,
    ) {
        MonthStepper(
            label = formatYearMonth(uiState.month),
            canGoNext = uiState.canGoNext,
            onPrevious = { onEvent(StatisticsUiEvent.PreviousMonth) },
            onNext = { onEvent(StatisticsUiEvent.NextMonth) },
            compact = true,
        )
        InsightSegments(
            labels = listOf(allLabel, expenseLabel, incomeLabel),
            selectedIndex = uiState.kind.segmentIndex(),
            onSelect = { index ->
                onEvent(StatisticsUiEvent.SelectKind(index.toStatisticsKind()))
            },
        )
        ChartSummary(chart = chart, billCount = billCount)
    }
}

@Composable
private fun ChartSummary(
    chart: CategoryDonutChart,
    billCount: Int?,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val diameter = if (maxWidth < CHART_DIAMETER) maxWidth else CHART_DIAMETER
            CategoryDonut(
                chart = chart,
                modifier = Modifier.size(diameter),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chart.centerLabel,
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = chart.centerAmount,
                color = chart.centerColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 16.sp,
                    maxFontSize = 22.sp,
                    stepSize = 1.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
            if (billCount != null) {
                Text(
                    text = pluralStringResource(R.plurals.entry_count, billCount, billCount),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StatisticsSections(
    uiState: StatisticsUiState,
    onOpenCategory: (StatisticsKind, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.sections.forEach { section ->
            StatisticsSection(
                section = section,
                showTitle = uiState.kind == StatisticsKind.All,
                currencySymbol = uiState.currencySymbol,
                onOpen = { id -> onOpenCategory(section.kind, id) },
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Text(
        text = stringResource(R.string.insight_empty),
        color = MaterialTheme.appColors.textTertiary,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun StatisticsSection(
    section: StatisticsSectionUi,
    showTitle: Boolean,
    currencySymbol: String,
    onOpen: (Long) -> Unit,
) {
    if (section.kind == StatisticsKind.All) return
    val titleRes = when (section.kind) {
        StatisticsKind.Expense -> R.string.statistics_section_expense
        StatisticsKind.Income -> R.string.statistics_section_income
        StatisticsKind.All -> return
    }
    val title = stringResource(titleRes)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        InsightCard(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            itemSpacing = 0.dp,
        ) {
            section.categories.forEach { share ->
                CategoryShareRow(
                    share = share,
                    currencySymbol = currencySymbol,
                    onClick = { onOpen(share.id) },
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

private fun String.toStatisticsKindOrNull(): StatisticsKind? =
    StatisticsKind.entries.firstOrNull { kind -> kind.name == this }

/** 还没点开分类或账单。不用可空 Long，旋转后才能写回状态。 */
private const val NoSelection = -1L

@Composable
internal fun CategoryShareUi.resolvedName(): String {
    val current = name
    if (missing || current == null) return stringResource(R.string.bill_category_missing)
    return current
}

@Composable
private fun CategoryShareRow(
    share: CategoryShareUi,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(share.colorKey)
    val title = share.resolvedName()
    PressFade(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatisticsCategoryMark(
                iconKey = share.iconKey,
                colorKey = share.colorKey,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
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
                        textAlign = TextAlign.End,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        modifier = Modifier.widthIn(min = 72.dp),
                    )
                    Text(
                        text = percentText(share.ratio),
                        color = colors.textTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .widthIn(min = 36.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { share.ratio },
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = tint,
                    trackColor = colors.tagBackground,
                    drawStopIndicator = {},
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
internal fun percentText(ratio: Float): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val format = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }
    }
    return format.format(ratio.toDouble())
}

/** 内容够宽时，图和分类左右并排，避免环两侧空出一大块。 */
private val SideBySideWidth = 520.dp

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
                    bills = previewBills(StatisticsKind.Expense, categories),
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
                    bills = previewBills(StatisticsKind.Expense, expense) +
                        previewBills(StatisticsKind.Income, income),
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

private fun previewBills(
    kind: StatisticsKind,
    shares: List<CategoryShareUi>,
): List<StatisticsBillUi> = shares.mapIndexed { index, share ->
    StatisticsBillUi(
        id = share.id,
        categoryId = share.id,
        kind = kind,
        amount = share.amount,
        time = LocalDateTime.of(2026, 10, 28 - index, 12, 30),
        remark = if (index == 0) "Lunch" else null,
        accountName = "Cash",
    )
}
