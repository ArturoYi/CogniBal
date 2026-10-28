package com.example.cognibal.screen.feature.settings.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.feature.bill.BillEntryRow
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.settings.overview.state.OverviewCategoryOption
import com.example.cognibal.screen.feature.settings.overview.state.OverviewDayAmount
import com.example.cognibal.screen.feature.settings.overview.state.OverviewKind
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiEvent
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiState
import com.example.cognibal.screen.feature.settings.overview.state.matches
import com.example.cognibal.ui.loading.LoadingIndicatorAppearDelayMillis
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.delay
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.InsightSegments
import com.example.cognibal.screen.feature.settings.component.MoneySummary
import com.example.cognibal.screen.feature.settings.component.MonthStepper
import com.example.cognibal.screen.feature.settings.component.formatMonthDay
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
import com.example.cognibal.screen.feature.settings.component.moneyText
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.picker.CogniBalWheelDateTimePicker
import com.example.cognibal.ui.picker.WheelDateTime
import com.example.cognibal.ui.picker.WheelDateTimeMode
import com.example.cognibal.ui.picker.WheelDateTimeRules

/** 总览入口。日历和当天账单来自 ViewModel。 */
@Composable
internal fun OverviewRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OverviewScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun OverviewScreen(
    uiState: OverviewUiState,
    onEvent: (OverviewUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val today = LocalDate.now()
    val amounts = uiState.days.associateBy { day -> day.day }
    var pickingMonth by rememberSaveable { mutableStateOf(false) }
    var filtering by rememberSaveable { mutableStateOf(false) }
    val monthLabel = formatYearMonth(uiState.month)
    SettingsSubpage(
        title = stringResource(R.string.shortcut_overview),
        onBack = onBack,
        modifier = modifier,
    ) {
        InsightCard(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            itemSpacing = 2.dp,
        ) {
            MonthStepper(
                label = monthLabel,
                canGoNext = uiState.canGoNext,
                onPrevious = { onEvent(OverviewUiEvent.PreviousMonth) },
                onNext = { onEvent(OverviewUiEvent.NextMonth) },
                onLabelClick = { pickingMonth = true },
                labelDescription = stringResource(R.string.overview_pick_month_cd, monthLabel),
                compact = true,
            )
            MoneySummary(
                income = uiState.income,
                expense = uiState.expense,
                balance = uiState.balance,
                balanceOverflow = uiState.balanceOverflow,
                currencySymbol = uiState.currencySymbol,
                compact = true,
            )
        }
        InsightCard {
            MonthCalendar(
                month = uiState.month,
                selectedDay = uiState.selectedDay,
                today = today,
                amounts = amounts,
                currencySymbol = uiState.currencySymbol,
                onSelect = { day -> onEvent(OverviewUiEvent.SelectDay(day)) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatMonthDay(uiState.selectedDay),
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (uiState.selectedDay == today) {
                    Text(
                        text = stringResource(R.string.overview_today),
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                DayFilterButton(
                    active = uiState.filterActive,
                    onClick = { filtering = true },
                )
            }
            if (!uiState.loading && uiState.bills.isEmpty()) {
                Text(
                    text = stringResource(
                        if (uiState.dayHasBills) {
                            R.string.overview_empty_filter
                        } else {
                            R.string.overview_empty_day
                        },
                    ),
                    color = colors.textTertiary,
                    fontSize = 14.sp,
                )
            } else {
                uiState.bills.forEach { bill ->
                    BillEntryRow(bill = bill, currencySymbol = uiState.currencySymbol)
                }
            }
        }
    }
    if (pickingMonth) {
        YearMonthDialog(
            month = uiState.month,
            onConfirm = { month ->
                pickingMonth = false
                onEvent(OverviewUiEvent.SelectMonth(month))
            },
            onDismiss = { pickingMonth = false },
        )
    }
    if (filtering) {
        DayFilterSheet(
            kind = uiState.kind,
            categories = uiState.categories,
            selectedIds = uiState.selectedCategoryIds,
            loading = uiState.loading,
            onApply = { nextKind, ids ->
                onEvent(OverviewUiEvent.ApplyFilter(nextKind, ids))
            },
            onClear = { onEvent(OverviewUiEvent.ClearFilter) },
            onDismiss = { filtering = false },
        )
    }
}

@Composable
private fun DayFilterButton(
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) colors.primary.copy(alpha = if (colors.isDark) 0.22f else 0.12f) else Color.Transparent)
            .interactiveClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FilterList,
            contentDescription = stringResource(R.string.overview_filter),
            tint = if (active) colors.primary else colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun YearMonthDialog(
    month: YearMonth,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(month) {
        mutableStateOf(WheelDateTime.of(month.year, month.monthValue, 1))
    }
    val now = YearMonth.now()
    val colors = MaterialTheme.appColors
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.cardBackground) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.overview_pick_month),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    CogniBalWheelDateTimePicker(
                        value = value,
                        onValueChange = { value = it },
                        mode = WheelDateTimeMode.YearMonth,
                        minTime = WheelDateTime.of(WheelDateTimeRules.DefaultMinYear, 1, 1),
                        maxTime = WheelDateTime.of(now.year, now.monthValue, 1),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                    }
                    TextButton(
                        onClick = { onConfirm(YearMonth.of(value.year, value.month)) },
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 筛选从底部弹出。改动先留在这里，点完成才交给外面。
 * 划掉或点外面关掉时，草稿丢掉，已经生效的筛选不动。
 * 清除会清掉生效的筛选再收起。当天流水还在整理时，被点的那个按钮转圈。
 *
 * 全部、支出、收入本身就是选项，不再加小标题。
 * 分类固定两行横滑。支出在前，收入在后，中间留一道分隔。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayFilterSheet(
    kind: OverviewKind,
    categories: List<OverviewCategoryOption>,
    selectedIds: Set<Long>,
    loading: Boolean,
    onApply: (OverviewKind, Set<Long>) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val expenseLabel = stringResource(R.string.bill_fab_expense)
    val incomeLabel = stringResource(R.string.bill_fab_income)
    var draftKind by remember { mutableStateOf(kind) }
    var draftIds by remember { mutableStateOf(selectedIds) }
    val pendingState = remember { mutableStateOf<FilterCommit?>(null) }
    var pending by pendingState
    val allowHide = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            target != SheetValue.Hidden || allowHide.value || pendingState.value == null
        },
    )
    val expenseCategories = categories.filter { category -> category.expense }
    val incomeCategories = categories.filter { category -> !category.expense }
    val showExpense = draftKind != OverviewKind.Income && expenseCategories.isNotEmpty()
    val showIncome = draftKind != OverviewKind.Expense && incomeCategories.isNotEmpty()
    val settled = when (val commit = pending) {
        null -> false
        FilterCommit.Clear -> kind == OverviewKind.All && selectedIds.isEmpty()
        is FilterCommit.Apply -> kind == commit.kind &&
            selectedIds == committedCategoryIds(categories, commit.kind, commit.ids)
    }
    val busy = pending != null
    LaunchedEffect(settled) {
        if (!settled) return@LaunchedEffect
        allowHide.value = true
        if (sheetState.isVisible) sheetState.hide()
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = {
            if (pendingState.value == null || allowHide.value) onDismiss()
        },
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = !busy,
            shouldDismissOnClickOutside = !busy,
        ),
        contentWindowInsets = {
            WindowInsets.navigationBars.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.overview_filter),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            InsightSegments(
                labels = listOf(
                    stringResource(R.string.overview_filter_all),
                    expenseLabel,
                    incomeLabel,
                ),
                selectedIndex = when (draftKind) {
                    OverviewKind.All -> 0
                    OverviewKind.Expense -> 1
                    OverviewKind.Income -> 2
                },
                onSelect = { index ->
                    if (!busy) {
                        draftKind = when (index) {
                            1 -> OverviewKind.Expense
                            2 -> OverviewKind.Income
                            else -> OverviewKind.All
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            when {
                loading && categories.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary,
                    )
                }
                !showExpense && !showIncome -> Text(
                    text = stringResource(R.string.bill_entry_no_category),
                    color = colors.textTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                else -> key(draftKind) {
                    CategoryStrip(
                        expense = if (showExpense) expenseCategories else emptyList(),
                        income = if (showIncome) incomeCategories else emptyList(),
                        expenseDescription = expenseLabel,
                        incomeDescription = incomeLabel,
                        selectedIds = draftIds,
                        enabled = !busy,
                        onToggle = { id -> draftIds = draftIds.toggle(id) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterSheetButton(
                    label = stringResource(R.string.overview_filter_clear),
                    emphasized = false,
                    enabled = !busy,
                    loading = pending is FilterCommit.Clear,
                    onClick = {
                        pending = FilterCommit.Clear
                        onClear()
                    },
                )
                FilterSheetButton(
                    label = stringResource(R.string.done),
                    emphasized = true,
                    enabled = !busy,
                    loading = pending is FilterCommit.Apply,
                    onClick = {
                        val ids = committedCategoryIds(categories, draftKind, draftIds)
                        pending = FilterCommit.Apply(draftKind, ids)
                        onApply(draftKind, ids)
                    },
                )
            }
        }
    }
}

/**
 * 支出、收入排在同一条横滑列表里。三个以上固定两行，一两个仍排成一行。
 * 两类都在时，中间一道细线把它们分开，不再写小标题。
 */
@Composable
private fun CategoryStrip(
    expense: List<OverviewCategoryOption>,
    income: List<OverviewCategoryOption>,
    expenseDescription: String,
    incomeDescription: String,
    selectedIds: Set<Long>,
    enabled: Boolean,
    onToggle: (Long) -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (expense.isNotEmpty()) {
            CategoryBlock(
                categories = expense,
                description = expenseDescription,
                selectedIds = selectedIds,
                enabled = enabled,
                onToggle = onToggle,
            )
        }
        if (expense.isNotEmpty() && income.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterVertically)
                    .width(1.dp)
                    .height(28.dp)
                    .background(colors.divider),
            )
        }
        if (income.isNotEmpty()) {
            CategoryBlock(
                categories = income,
                description = incomeDescription,
                selectedIds = selectedIds,
                enabled = enabled,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun CategoryBlock(
    categories: List<OverviewCategoryOption>,
    description: String,
    selectedIds: Set<Long>,
    enabled: Boolean,
    onToggle: (Long) -> Unit,
) {
    val rows = remember(categories) { categoryRows(categories) }
    Column(
        modifier = Modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    CategoryFilterChip(
                        name = category.name,
                        selected = category.id in selectedIds,
                        enabled = enabled,
                        onClick = { onToggle(category.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Text(
        text = name,
        color = if (selected) colors.onPrimary else colors.textPrimary,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 168.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) colors.primary else colors.tagBackground)
            .semantics { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun FilterSheetButton(
    label: String,
    emphasized: Boolean,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val showSpinner = rememberDelayedLoading(loading)
    val loadingDescription = stringResource(R.string.loading_indicator_cd)
    val acceptClick = enabled && !loading
    TextButton(
        onClick = { if (acceptClick) onClick() },
        enabled = acceptClick || showSpinner,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                modifier = Modifier
                    .alpha(if (showSpinner) 0f else 1f)
                    .then(if (showSpinner) Modifier.clearAndSetSemantics {} else Modifier),
                color = if (emphasized) colors.primary else colors.textTertiary,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            )
            if (showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .semantics { contentDescription = loadingDescription },
                    strokeWidth = 2.dp,
                    color = colors.primary,
                )
            }
        }
    }
}

/** 几十毫秒的整理不闪圈，拖久了再出现，跟全页等待同一档。 */
@Composable
private fun rememberDelayedLoading(active: Boolean): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!active) {
            visible = false
            return@LaunchedEffect
        }
        delay(LoadingIndicatorAppearDelayMillis.toLong())
        visible = true
    }
    return visible
}

private sealed interface FilterCommit {
    data object Clear : FilterCommit
    data class Apply(val kind: OverviewKind, val ids: Set<Long>) : FilterCommit
}

private fun committedCategoryIds(
    categories: List<OverviewCategoryOption>,
    kind: OverviewKind,
    ids: Set<Long>,
): Set<Long> {
    if (ids.isEmpty()) return emptySet()
    val visible = categories.asSequence().filter { it.matches(kind) }.map { it.id }.toSet()
    return ids.intersect(visible)
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

/** 三个以上固定两行。一两个仍排成一行，避免叠成一列。 */
private fun categoryRows(categories: List<OverviewCategoryOption>): List<List<OverviewCategoryOption>> {
    if (categories.isEmpty()) return emptyList()
    val rowCount = if (categories.size <= 2) 1 else FilterCategoryRows
    val columns = (categories.size + rowCount - 1) / rowCount
    return List(rowCount) { row ->
        buildList {
            for (column in 0 until columns) {
                val index = row * columns + column
                if (index < categories.size) add(categories[index])
            }
        }
    }.filter { row -> row.isNotEmpty() }
}

private const val FilterCategoryRows = 2

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDay: LocalDate,
    today: LocalDate,
    amounts: Map<LocalDate, OverviewDayAmount>,
    currencySymbol: String,
    onSelect: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val weeks = remember(month, locale) { calendarWeeks(month, locale) }
    val labels = remember(locale) { weekdayLabels(locale) }
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).heightIn(min = 52.dp))
                    } else {
                        val amount = amounts[day]
                        DayCell(
                            day = day,
                            selected = day == selectedDay,
                            today = day == today,
                            income = amount?.income ?: 0L,
                            expense = amount?.expense ?: 0L,
                            currencySymbol = currencySymbol,
                            onClick = { onSelect(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    selected: Boolean,
    today: Boolean,
    income: Long,
    expense: Long,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val dateLabel = formatMonthDay(day)
    val description = if (income == 0L && expense == 0L) {
        dateLabel
    } else {
        stringResource(
            R.string.overview_day_amounts_cd,
            dateLabel,
            moneyText(currencySymbol, income),
            moneyText(currencySymbol, expense),
        )
    }
    val shape = RoundedCornerShape(10.dp)
    val background = if (selected) {
        colors.primary.copy(alpha = if (colors.isDark) 0.22f else 0.14f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .padding(1.dp)
            .then(
                if (today && !selected) {
                    Modifier.border(1.dp, colors.primary.copy(alpha = 0.7f), shape)
                } else {
                    Modifier
                },
            )
            .dayClick(description = description, selected = selected, onClick = onClick)
            .clip(shape)
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = if (selected || today) colors.primary else colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
                .height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (income > 0L) Dot(colors.chartIncome)
            if (expense > 0L) Dot(colors.chartExpense)
        }
    }
}

/**
 * 一天一格，七列排不下 48.dp 的最小点击宽度，所以这里不套 [com.example.cognibal.ui.interaction.interactiveClick]。
 * 键盘焦点仍描一圈。
 */
@Composable
private fun Modifier.dayClick(
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val outlined = if (focused) {
        border(2.dp, MaterialTheme.colorScheme.secondary, shape)
    } else {
        this
    }
    return outlined
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
        .semantics {
            contentDescription = description
            this.selected = selected
        }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, CircleShape),
    )
}

private fun calendarWeeks(month: YearMonth, locale: Locale): List<List<LocalDate?>> {
    val first = WeekFields.of(locale).firstDayOfWeek
    val start = month.atDay(1)
    val lead = (start.dayOfWeek.value - first.value + 7) % 7
    val cells = MutableList<LocalDate?>(lead) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells += month.atDay(day)
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    return cells.chunked(7)
}

private fun weekdayLabels(locale: Locale): List<String> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return List(7) { offset ->
        first.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale)
    }
}

@FormFactorPreviews
@Composable
private fun OverviewScreenPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            OverviewScreen(
                uiState = OverviewUiState(
                    loading = false,
                    month = YearMonth.of(2026, 10),
                    selectedDay = LocalDate.of(2026, 10, 13),
                    income = 18_000_00L,
                    expense = 6_420_00L,
                    balance = 11_580_00L,
                    days = listOf(
                        OverviewDayAmount(LocalDate.of(2026, 10, 3), expense = 2_800L, income = 0L),
                        OverviewDayAmount(LocalDate.of(2026, 10, 13), expense = 4_200L, income = 18_000_00L),
                        OverviewDayAmount(LocalDate.of(2026, 10, 20), expense = 1_600L, income = 0L),
                    ),
                    bills = listOf(
                        BillRowUi(
                            id = 1,
                            categoryName = "Food",
                            type = BillType.Expense,
                            time = LocalDateTime.of(2026, 10, 13, 12, 30),
                            signedAmount = -4_200L,
                        ),
                    ),
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
