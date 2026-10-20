package com.example.cognibal.screen.feature.settings.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.feature.bill.BillEntryRow
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.settings.overview.state.OverviewDayAmount
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiEvent
import com.example.cognibal.screen.feature.settings.overview.state.OverviewUiState
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
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.MoneySummary
import com.example.cognibal.screen.feature.settings.component.MonthStepper
import com.example.cognibal.screen.feature.settings.component.formatMonthDay
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
import com.example.cognibal.screen.feature.settings.component.moneyText

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
    SettingsSubpage(
        title = stringResource(R.string.shortcut_overview),
        onBack = onBack,
        modifier = modifier,
    ) {
        InsightCard {
            MonthStepper(
                label = formatYearMonth(uiState.month),
                canGoNext = uiState.canGoNext,
                onPrevious = { onEvent(OverviewUiEvent.PreviousMonth) },
                onNext = { onEvent(OverviewUiEvent.NextMonth) },
            )
            MoneySummary(
                income = uiState.income,
                expense = uiState.expense,
                balance = uiState.balance,
                balanceOverflow = uiState.balanceOverflow,
                currencySymbol = uiState.currencySymbol,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            }
            if (!uiState.loading && uiState.bills.isEmpty()) {
                Text(
                    text = stringResource(R.string.overview_empty_day),
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
}

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
            if (income > 0L) Dot(colors.income)
            if (expense > 0L) Dot(colors.expense)
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
