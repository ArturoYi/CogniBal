package com.cyr.cognobal.screen.feature.settings.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
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
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.bill.PressFade
import com.cyr.cognobal.screen.feature.settings.SettingsSubpage
import com.cyr.cognobal.screen.feature.settings.component.HeaderCircleButton
import com.cyr.cognobal.screen.feature.settings.component.InsightCard
import com.cyr.cognobal.screen.feature.settings.component.InsightSegments
import com.cyr.cognobal.screen.feature.settings.component.MonthStepper
import com.cyr.cognobal.screen.feature.settings.component.YearMonthPickerDialog
import com.cyr.cognobal.screen.feature.settings.component.formatYearMonth
import com.cyr.cognobal.screen.feature.settings.component.moneyText
import com.cyr.cognobal.screen.feature.settings.statistics.state.CategoryShareUi
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsBillUi
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsKind
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsSectionUi
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsUiEvent
import com.cyr.cognobal.screen.feature.settings.statistics.state.StatisticsUiState
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.privacy.LocalAmountVisibility
import com.cyr.cognobal.ui.privacy.maskIfHidden
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.LocalAppThemeStyle
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.categorySliceColor
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

@Composable
internal fun StatisticsRoute(
    onBack: () -> Unit,
    onOpenBill: (Long) -> Unit,
    showBack: Boolean = false,
    onOpenReports: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        showBack = showBack,
        onOpenBill = onOpenBill,
        onOpenReports = onOpenReports,
        onOpenCalendar = onOpenCalendar,
        modifier = modifier,
    )
}

@Composable
internal fun StatisticsScreen(
    uiState: StatisticsUiState,
    onEvent: (StatisticsUiEvent) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean = false,
    onOpenBill: (Long) -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pickingMonth by rememberSaveable { mutableStateOf(false) }
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
        showBackOverride = showBack,
        trailing = {
            HeaderCircleButton(
                icon = Icons.Outlined.CalendarMonth,
                description = stringResource(R.string.shortcut_overview),
                onClick = onOpenCalendar,
            )
            HeaderCircleButton(
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                description = stringResource(R.string.shortcut_reports),
                onClick = onOpenReports,
            )
        },
        modifier = modifier,
    ) {
        StatisticsBody(
            uiState = uiState,
            onEvent = onEvent,
            onPickMonth = { pickingMonth = true },
            onOpenCategory = { kind, id ->
                openKindName = kind.name
                openCategoryId = id
                openBillId = NoSelection
            },
        )
    }

    if (pickingMonth) {
        YearMonthPickerDialog(
            month = uiState.month,
            onConfirm = { month ->
                pickingMonth = false
                onEvent(StatisticsUiEvent.SelectMonth(month))
            },
            onDismiss = { pickingMonth = false },
        )
    }

    if (openShare != null && openKind != null) {
        StatisticsBillSheet(
            share = openShare,
            bills = openBills,
            currencySymbol = uiState.currencySymbol,
            billId = openBill?.id,
            onOpenBill = onOpenBill,
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
    onPickMonth: () -> Unit,
    onOpenCategory: (StatisticsKind, Long) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sideBySide = maxWidth >= SideBySideWidth && uiState.sections.isNotEmpty()
        if (sideBySide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ChartCard(
                    uiState = uiState,
                    onEvent = onEvent,
                    onPickMonth = onPickMonth,
                    modifier = Modifier.weight(1.15f),
                )
                StatisticsSections(
                    uiState = uiState,
                    onOpenCategory = onOpenCategory,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartCard(
                    uiState = uiState,
                    onEvent = onEvent,
                    onPickMonth = onPickMonth,
                )
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
    onPickMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val expenseLabel = stringResource(R.string.bill_figure_expense)
    val incomeLabel = stringResource(R.string.bill_figure_income)
    val allLabel = stringResource(R.string.overview_filter_all)

    val summaryLabel = when (uiState.kind) {
        StatisticsKind.Expense -> stringResource(R.string.bill_month_expense)
        StatisticsKind.Income -> stringResource(R.string.bill_month_income)
        StatisticsKind.All -> stringResource(R.string.bill_month_balance)
    }
    val centerColor = if (uiState.totalOverflow) colors.textSecondary else colors.textPrimary
    val rawAmount = moneyText(uiState.currencySymbol, uiState.total, uiState.totalOverflow)
    val centerAmount = maskIfHidden(rawAmount, hidden, uiState.currencySymbol)
    val chart = remember(uiState.slices, summaryLabel, centerAmount, centerColor) {
        CategoryDonutChart(
            slices = uiState.slices,
            centerLabel = summaryLabel,
            centerAmount = centerAmount,
            centerColor = centerColor,
        )
    }
    val monthLabel = formatYearMonth(uiState.month)
    val billCount = if (!uiState.loading && uiState.bills.isNotEmpty()) uiState.bills.size else null
    val dailyAvg = remember(uiState.total, uiState.month, uiState.kind, uiState.totalOverflow) {
        if (uiState.totalOverflow || uiState.kind == StatisticsKind.All || uiState.total <= 0L) {
            null
        } else {
            val today = LocalDate.now()
            val currentYm = YearMonth.from(today)
            val days = if (uiState.month == currentYm) {
                today.dayOfMonth.coerceAtLeast(1)
            } else {
                uiState.month.lengthOfMonth()
            }
            uiState.total / days
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InsightSegments(
            labels = listOf(expenseLabel, incomeLabel, allLabel),
            selectedIndex = uiState.kind.segmentIndex(),
            onSelect = { index ->
                onEvent(StatisticsUiEvent.SelectKind(index.toStatisticsKind()))
            },
        )
        MonthStepper(
            label = monthLabel,
            canGoNext = uiState.canGoNext,
            onPrevious = { onEvent(StatisticsUiEvent.PreviousMonth) },
            onNext = { onEvent(StatisticsUiEvent.NextMonth) },
            onLabelClick = onPickMonth,
            labelDescription = stringResource(R.string.overview_pick_month_cd, monthLabel),
        )
        InsightCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)) {
            ChartSummary(
                chart = chart,
                currencySymbol = uiState.currencySymbol,
                dailyAvg = dailyAvg,
                billCount = billCount,
                hidden = hidden,
            )
        }
    }
}

@Composable
private fun ChartSummary(
    chart: CategoryDonutChart,
    currencySymbol: String,
    dailyAvg: Long?,
    billCount: Int?,
    hidden: Boolean,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = chart.centerLabel,
                color = colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chart.centerAmount,
                color = chart.centerColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 27.sp),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 18.sp,
                    maxFontSize = 27.sp,
                    stepSize = 1.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            val footer = buildList {
                if (dailyAvg != null) {
                    val avgStr = maskIfHidden(moneyText(currencySymbol, dailyAvg), hidden, currencySymbol)
                    add("$avgStr / ${stringResource(R.string.wheel_day)}")
                }
                if (billCount != null) {
                    add(pluralStringResource(R.plurals.entry_count, billCount, billCount))
                }
            }.joinToString(" · ")
            if (footer.isNotEmpty()) {
                Text(
                    text = footer,
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.labelMedium.merge(
                        TextStyle(fontFeatureSettings = "tnum"),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CategoryDonut(
            chart = chart,
            modifier = Modifier.size(CHART_DIAMETER),
        )
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        uiState.sections.forEach { section ->
            StatisticsSection(
                section = section,
                currencySymbol = uiState.currencySymbol,
                onOpen = { id -> onOpenCategory(section.kind, id) },
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    SurfaceEmptyCard(text = stringResource(R.string.insight_empty))
}

@Composable
private fun SurfaceEmptyCard(text: String) {
    val colors = MaterialTheme.appColors
    InsightCard {
        Text(
            text = text,
            color = colors.textTertiary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StatisticsSection(
    section: StatisticsSectionUi,
    currencySymbol: String,
    onOpen: (Long) -> Unit,
) {
    if (section.kind == StatisticsKind.All) return
    val titleRes = when (section.kind) {
        StatisticsKind.Expense -> R.string.statistics_section_expense
        StatisticsKind.Income -> R.string.statistics_section_income
        StatisticsKind.All -> return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(titleRes),
            color = MaterialTheme.appColors.textPrimary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        InsightCard(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            itemSpacing = 2.dp,
        ) {
            val total = section.categories.size
            section.categories.forEachIndexed { index, share ->
                if (index > 0) {
                    Box(
                        Modifier
                            .padding(start = 56.dp)
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(MaterialTheme.appColors.divider),
                    )
                }
                CategoryShareRow(
                    share = share,
                    index = index,
                    total = total,
                    currencySymbol = currencySymbol,
                    onClick = { onOpen(share.id) },
                )
            }
        }
    }
}

private fun StatisticsKind.segmentIndex(): Int = when (this) {
    StatisticsKind.Expense -> 0
    StatisticsKind.Income -> 1
    StatisticsKind.All -> 2
}

private fun Int.toStatisticsKind(): StatisticsKind = when (this) {
    0 -> StatisticsKind.Expense
    1 -> StatisticsKind.Income
    else -> StatisticsKind.All
}

private fun String.toStatisticsKindOrNull(): StatisticsKind? =
    StatisticsKind.entries.firstOrNull { kind -> kind.name == this }

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
    index: Int,
    total: Int,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val monochrome = LocalAppThemeStyle.current == AppThemeStyle.Monochrome
    val barColor = categorySliceColor(
        colorKey = share.colorKey,
        index = index,
        total = total,
        colors = colors,
        monochrome = monochrome,
    )
    val title = share.resolvedName()
    PressFade(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatisticsCategoryMark(
                iconKey = share.iconKey,
                colorKey = share.colorKey,
                tile = 42.dp,
                icon = 20.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = maskIfHidden(moneyText(currencySymbol, share.amount), hidden, currencySymbol),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall.merge(
                            TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = percentText(share.ratio),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.labelMedium.merge(
                            TextStyle(fontFeatureSettings = "tnum"),
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .widthIn(min = 42.dp),
                    )
                }
                LinearProgressIndicator(
                    progress = { share.ratio },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = barColor,
                    trackColor = colors.tagBackground,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
internal fun percentText(ratio: Float): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val format = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }
    }
    return format.format(ratio.toDouble())
}

private val SideBySideWidth = 560.dp

@FormFactorPreviews
@Composable
private fun StatisticsScreenPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            val categories = previewExpenseShares()
            StatisticsScreen(
                uiState = StatisticsUiState(
                    loading = false,
                    month = YearMonth.of(2026, 5),
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

private fun previewExpenseShares(): List<CategoryShareUi> = listOf(
    CategoryShareUi(1, "购物", false, "shopping_bag", "#FFFD7E14", 245_600L, 0.362f),
    CategoryShareUi(2, "餐饮", false, "restaurant", "#FFF03E3E", 163_550L, 0.241f),
    CategoryShareUi(3, "交通", false, "directions_bus", "#FF228BE6", 89_620L, 0.132f),
    CategoryShareUi(4, "居住", false, "home", "#FF7950F2", 84_500L, 0.125f),
    CategoryShareUi(5, "娱乐", false, "movie", "#FFE64980", 48_960L, 0.072f),
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
        time = LocalDateTime.of(2026, 5, 28 - index, 12, 30),
        remark = if (index == 0) "超市采购" else null,
        accountName = "储蓄卡",
    )
}
