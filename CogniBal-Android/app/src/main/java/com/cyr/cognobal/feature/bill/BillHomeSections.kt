package com.cyr.cognobal.screen.feature.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.data.repository.BudgetLevel
import com.cyr.cognobal.screen.feature.bill.model.accountIconImage
import com.cyr.cognobal.screen.feature.bill.state.BillHomeUiState
import com.cyr.cognobal.screen.feature.bill.state.BillRowUi
import com.cyr.cognobal.screen.feature.bill.state.BudgetProgressUi
import com.cyr.cognobal.screen.feature.bill.state.groupBillsByDay
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.bill.tool.formatMoneyOrOverflow
import com.cyr.cognobal.screen.feature.expense.model.categoryIcon
import com.cyr.cognobal.ui.format.formatBillTime
import com.cyr.cognobal.ui.format.formatCalendarDay
import com.cyr.cognobal.ui.insets.screenContentWindowInsets
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.privacy.AmountEyeToggle
import com.cyr.cognobal.ui.privacy.LocalAmountVisibility
import com.cyr.cognobal.ui.privacy.maskIfHidden
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.budgetNearColor
import com.cyr.cognobal.ui.theme.softBadgeTone
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HomeContentMaxWidth = 760.dp

/**
 * 记账首页：
 * 1. 顶部账本下拉切换器 + 日历视图与全部账单快捷图标
 * 2. 曜石深灰主卡（本月支出 + 隐私显隐开关 + 本月收入 + 预算剩余/结余 + 7 日香槟金迷你柱图）
 * 3. 本月预算纯白卡片（预算上限、百分比、温润进度条与剩余额度）
 * 4. 最近交易纯白卡片组（按日期分组，并可进入全部账单）
 */
@Composable
internal fun BillHomeList(
    uiState: BillHomeUiState,
    headerState: BillHomeUiState,
    onOpenAccounts: () -> Unit,
    onCreateAccount: () -> Unit,
    onViewAll: () -> Unit,
    onOpenBill: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    onManageBudget: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val base = billPageListPadding(extraBottom = 24.dp)
    val topInset =
        screenContentWindowInsets()
            .only(WindowInsetsSides.Top)
            .asPaddingValues()
            .calculateTopPadding()
    val contentPadding =
        PaddingValues(
            start = base.calculateLeftPadding(layoutDirection),
            top = topInset + 12.dp,
            end = base.calculateRightPadding(layoutDirection),
            bottom = base.calculateBottomPadding(),
        )
    val figuresReady =
        !uiState.loading &&
            headerState.currentId != null &&
            uiState.currentId == headerState.currentId
    val hasAccounts = headerState.accounts.isNotEmpty()
    val currentAccount = headerState.accounts.firstOrNull { it.id == headerState.currentId }

    LazyColumn(
        modifier = modifier
            .widthIn(max = HomeContentMaxWidth)
            .fillMaxWidth()
            .fillMaxHeight()
            .consumeWindowInsets(contentPadding),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "top-bar") {
            HomeTopBar(
                accountName = currentAccount?.name,
                hasAccounts = hasAccounts,
                onSwitchAccount = onOpenAccounts,
                onCreateAccount = onCreateAccount,
                onOpenCalendar = onOpenCalendar,
                onViewAll = onViewAll,
            )
        }

        if (!headerState.loading && !hasAccounts) {
            item(key = "empty-account") {
                EmptyAccountCard(onCreateAccount = onCreateAccount)
            }
        } else {
            item(key = "hero-card") {
                MonthHeroCard(
                    uiState = uiState,
                    currencySymbol = headerState.currencySymbol,
                    figuresReady = figuresReady,
                )
            }

            if (uiState.showBudget && hasAccounts) {
                item(key = "budget-section") {
                    HomeBudgetSection(
                        budget = homeBudget(uiState.budgets),
                        currencySymbol = headerState.currencySymbol,
                        figuresReady = figuresReady,
                        onOpenBudget = onOpenBudget,
                        onManageBudget = onManageBudget,
                    )
                }
            }

            item(key = "recent-section") {
                RecentTransactionsSection(
                    uiState = uiState,
                    hasAccounts = hasAccounts,
                    figuresReady = figuresReady,
                    onViewAll = onViewAll,
                    onOpenBill = onOpenBill,
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    accountName: String?,
    hasAccounts: Boolean,
    onSwitchAccount: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenCalendar: () -> Unit,
    onViewAll: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val title = accountName ?: stringResource(R.string.app_name)
    val switchDesc = if (accountName != null) {
        stringResource(R.string.account_switch_cd, accountName)
    } else {
        stringResource(R.string.bill_account_create)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(CircleShape)
                .semantics(mergeDescendants = true) { contentDescription = switchDesc }
                .interactiveClick(
                    onClick = if (hasAccounts) onSwitchAccount else onCreateAccount,
                )
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = if (hasAccounts) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopBarIconButton(
                icon = Icons.Outlined.CalendarMonth,
                description = stringResource(R.string.shortcut_overview),
                onClick = onOpenCalendar,
            )
            if (hasAccounts) {
                TopBarIconButton(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    description = stringResource(R.string.bill_history_title),
                    onClick = onViewAll,
                )
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = description }
            .interactiveClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MonthHeroCard(
    uiState: BillHomeUiState,
    currencySymbol: String,
    figuresReady: Boolean,
) {
    val visibility = LocalAmountVisibility.current
    val hidden = visibility.hidden
    val budget = if (uiState.showBudget) homeBudget(uiState.budgets) else null
    val expenseRaw = cardAmount(currencySymbol, uiState.monthExpense, figuresReady)
    val incomeRaw = cardAmount(currencySymbol, uiState.monthIncome, figuresReady)
    val expenseText = if (figuresReady) maskIfHidden(expenseRaw, hidden, currencySymbol) else expenseRaw
    val incomeText = if (figuresReady) maskIfHidden(incomeRaw, hidden, currencySymbol) else incomeRaw

    val rightLabel: String
    val rightValue: String
    if (budget != null && figuresReady) {
        val diff = budget.limit - budget.spent
        rightLabel = stringResource(R.string.bill_budget_title)
        val formatted = if (diff >= 0L) {
            stringResource(R.string.budget_remaining, formatMoney(currencySymbol, diff))
        } else {
            stringResource(R.string.budget_over_amount, formatMoney(currencySymbol, -diff))
        }
        rightValue = if (hidden) "$currencySymbol ••••" else formatted
    } else {
        rightLabel = stringResource(R.string.bill_month_balance)
        val balRaw = cardAmount(currencySymbol, uiState.monthBalance, figuresReady, uiState.monthOverflow)
        rightValue = if (figuresReady) maskIfHidden(balRaw, hidden, currencySymbol) else balRaw
    }

    AccountHeroSurface { paint ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.bill_month_expense),
                        color = paint.on.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    AmountEyeToggle(
                        hidden = hidden,
                        onToggle = visibility::toggle,
                        tint = paint.on.copy(alpha = 0.72f),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (budget == null) {
                        Text(
                            text = rightLabel,
                            color = paint.on.copy(alpha = 0.62f),
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = rightValue,
                        color = paint.on.copy(alpha = 0.90f),
                        style = MaterialTheme.typography.labelMedium.merge(
                            TextStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = expenseText,
                        color = paint.on,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 20.sp,
                            maxFontSize = 32.sp,
                            stepSize = 1.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${stringResource(R.string.bill_month_income)} $incomeText",
                        color = paint.on.copy(alpha = 0.66f),
                        style = MaterialTheme.typography.bodySmall.merge(
                            TextStyle(fontFeatureSettings = "tnum"),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HeroMiniSparkline(
                    bills = if (figuresReady) uiState.recent else emptyList(),
                    today = uiState.today,
                    accent = paint.identity,
                    modifier = Modifier
                        .width(92.dp)
                        .height(44.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroMiniSparkline(
    bills: List<BillRowUi>,
    today: LocalDate,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val values = remember(bills, today) {
        val byDay = bills.groupBy { it.time.toLocalDate() }
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            byDay[date].orEmpty()
                .filter { it.type == BillType.Expense }
                .sumOf { (-it.signedAmount).coerceAtLeast(0L) }
        }
    }
    val maxVal = remember(values) { (values.maxOrNull() ?: 0L).coerceAtLeast(1L) }

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val count = values.size
        if (count == 0 || size.width <= 0f || size.height <= 0f) return@Canvas
        val barWidth = 4.5.dp.toPx()
        val gap = ((size.width - barWidth * count) / (count - 1).coerceAtLeast(1)).coerceAtLeast(2f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        values.forEachIndexed { index, amount ->
            val x = index * (barWidth + gap)
            val isToday = index == count - 1
            val fraction = if (amount <= 0L) {
                0.18f
            } else {
                (amount.toFloat() / maxVal.toFloat()).coerceIn(0.26f, 1f)
            }
            val barHeight = size.height * fraction
            val top = size.height - barHeight
            val alpha = when {
                isToday && amount > 0L -> 1f
                amount > 0L -> 0.68f
                isToday -> 0.42f
                else -> 0.22f
            }
            drawRoundRect(
                color = accent.copy(alpha = alpha),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun HomeBudgetSection(
    budget: BudgetProgressUi?,
    currencySymbol: String,
    figuresReady: Boolean,
    onOpenBudget: () -> Unit,
    onManageBudget: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitleBar(
            title = stringResource(R.string.bill_budget_title),
            actionText = stringResource(if (budget != null) R.string.budget_edit else R.string.budget_add),
            onAction = onOpenBudget,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .interactiveClick(onClick = if (budget != null) onManageBudget else onOpenBudget),
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBackground,
            shadowElevation = 0.dp,
        ) {
            if (!figuresReady) {
                PendingLine(modifier = Modifier.padding(18.dp))
            } else if (budget == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.budget_empty),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                val barColor = when (budget.level) {
                    BudgetLevel.Over -> colors.expense
                    BudgetLevel.Near -> budgetNearColor(colors)
                    BudgetLevel.Normal -> colors.primary
                }
                val limitText = maskIfHidden(formatMoney(currencySymbol, budget.limit), hidden, currencySymbol)
                val percent = budgetPercentLabel(budget.spent, budget.limit)
                val diff = budget.limit - budget.spent
                val remainRaw = if (diff >= 0L) {
                    stringResource(R.string.budget_remaining, formatMoney(currencySymbol, diff))
                } else {
                    stringResource(R.string.budget_over_amount, formatMoney(currencySymbol, -diff))
                }
                val remainText = if (hidden) "$currencySymbol ••••" else remainRaw
                val progressCd = stringResource(R.string.bill_budget_progress_cd, percent)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = limitText,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium.merge(
                                TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = percent,
                            color = barColor,
                            style = MaterialTheme.typography.labelLarge.merge(
                                TextStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                            ),
                            maxLines = 1,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { budget.ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = progressCd },
                        color = barColor,
                        trackColor = colors.tagBackground,
                        drawStopIndicator = {},
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = remainText,
                            color = if (budget.level == BudgetLevel.Over) colors.expense else colors.textTertiary,
                            style = MaterialTheme.typography.labelMedium.merge(
                                TextStyle(fontFeatureSettings = "tnum"),
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    uiState: BillHomeUiState,
    hasAccounts: Boolean,
    figuresReady: Boolean,
    onViewAll: () -> Unit,
    onOpenBill: (Long) -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RecentSectionHeader(
            showActions = hasAccounts,
            onViewAll = onViewAll,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBackground,
            shadowElevation = 0.dp,
        ) {
            when {
                !figuresReady -> {
                    PendingLine(modifier = Modifier.padding(18.dp))
                }
                uiState.recent.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.bill_recent_week_empty),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                    )
                }
                else -> {
                    RecentGroupedBills(
                        bills = uiState.recent,
                        currencySymbol = uiState.currencySymbol,
                        today = uiState.today,
                        onOpenBill = onOpenBill,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSectionHeader(
    showActions: Boolean,
    onViewAll: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.bill_recent_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (showActions) {
            RecentViewAllButton(onClick = onViewAll)
        }
    }
}

@Composable
private fun RecentViewAllButton(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    val description = stringResource(R.string.bill_recent_view_all_cd)
    PressFade(onClick = onClick) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.bill_recent_all),
                color = colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private data class RecentSectionVisual(
    val title: String,
    val net: Long,
    val bills: List<BillRowUi>,
)

@Composable
private fun RecentGroupedBills(
    bills: List<BillRowUi>,
    currencySymbol: String,
    today: LocalDate,
    onOpenBill: (Long) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val sections = recentSections(bills = bills, today = today)
    Column(modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
        sections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) {
                Box(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp)
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(colors.divider),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.title,
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                Text(
                    text = maskIfHidden(
                        signedMoneyText(currencySymbol, section.net),
                        hidden,
                        currencySymbol,
                    ),
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.labelMedium.merge(
                        TextStyle(fontFeatureSettings = "tnum"),
                    ),
                    maxLines = 1,
                )
            }
            section.bills.forEachIndexed { index, bill ->
                key(bill.id) {
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(start = 72.dp, end = 16.dp)
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(colors.divider),
                        )
                    }
                    OpenBillRow(
                        bill = bill,
                        currencySymbol = currencySymbol,
                        onClick = { onOpenBill(bill.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun recentSections(
    bills: List<BillRowUi>,
    today: LocalDate,
): List<RecentSectionVisual> {
    val days = remember(bills) { groupBillsByDay(bills) }
    return days.map { (day, rows) ->
        RecentSectionVisual(
            title = weekDayText(day, today),
            net = rows.sumOf { it.signedAmount },
            bills = rows,
        )
    }
}

@Composable
private fun SectionTitleBar(
    title: String,
    actionText: String?,
    onAction: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionText != null) {
            PressFade(onClick = onAction) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionText,
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountCard(onCreateAccount: () -> Unit) {
    val colors = MaterialTheme.appColors
    PressFade(
        onClick = onCreateAccount,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardBackground),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.bill_account_empty_title),
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.inkStrong)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.onInkStrong,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.bill_account_create),
                    color = colors.onInkStrong,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/**
 * 全部账单页日期分组卡片：顶部日期与当日收支轻巧对齐，账单包裹在纯净白卡中。
 */
@Composable
internal fun DayBillBlock(
    dayLabel: String,
    rows: List<BillRowUi>,
    currencySymbol: String,
    onOpenBill: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val dayNet = remember(rows) { rows.sumOf { it.signedAmount } }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayLabel,
                color = colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (rows.isNotEmpty()) {
                Text(
                    text = maskIfHidden(signedMoneyText(currencySymbol, dayNet), hidden, currencySymbol),
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.labelMedium.merge(
                        TextStyle(fontFeatureSettings = "tnum"),
                    ),
                    maxLines = 1,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBackground,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                rows.forEachIndexed { index, bill ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(start = 70.dp, end = 16.dp)
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(colors.divider),
                        )
                    }
                    OpenBillRow(
                        bill = bill,
                        currencySymbol = currencySymbol,
                        onClick = { onOpenBill(bill.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenBillRow(
    bill: BillRowUi,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val title = when (bill.type) {
        BillType.Transfer -> stringResource(R.string.bill_transfer)
        else -> bill.categoryName ?: stringResource(R.string.bill_category_missing)
    }
    val amountColor = when (bill.type) {
        BillType.Income -> colors.income
        else -> colors.textPrimary
    }
    val remark = bill.remark?.trim().orEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .interactiveClick(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BillMark(bill = bill)
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .wrapContentHeight(unbounded = true)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = clockText(bill.time),
                color = colors.textTertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (remark.isNotEmpty()) {
                Text(
                    text = remark,
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = maskIfHidden(signedMoneyText(currencySymbol, bill.signedAmount), hidden, currencySymbol),
            color = amountColor,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
            ),
            maxLines = 1,
        )
    }
}

private fun signedMoneyText(symbol: String, signedAmount: Long): String {
    return when {
        signedAmount < 0L -> "- ${formatMoney(symbol, -signedAmount)}"
        signedAmount > 0L -> "+ ${formatMoney(symbol, signedAmount)}"
        else -> formatMoney(symbol, 0L)
    }
}

@Composable
private fun clockText(time: LocalDateTime): String {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT) }
    return time.format(formatter)
}

@Composable
private fun BillMark(bill: BillRowUi) {
    val icon = when (bill.type) {
        BillType.Transfer -> Icons.Outlined.SwapHoriz
        else -> categoryIcon(bill.categoryIconKey.orEmpty())
    }
    val tone = softBadgeTone(bill.categoryColorKey.orEmpty())
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tone.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone.content,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun cardAmount(
    symbol: String,
    amount: Long,
    ready: Boolean,
    overflow: Boolean = false,
): String {
    if (!ready) return "—"
    return formatMoneyOrOverflow(
        symbol,
        amount,
        overflow,
        stringResource(R.string.account_balance_overflow),
    )
}

private fun homeBudget(budgets: List<BudgetProgressUi>): BudgetProgressUi? =
    budgets.firstOrNull { budget -> budget.categoryName == null && !budget.missingCategory }

private fun budgetPercentLabel(spent: Long, limit: Long): String {
    if (limit <= 0L || spent <= 0L) return "0%"
    val tenths = try {
        Math.multiplyExact(spent, 1_000L) / limit
    } catch (_: ArithmeticException) {
        return "999+%"
    }
    if (tenths >= 9_995L) return "999+%"
    val whole = tenths / 10L
    val dec = tenths % 10L
    return if (dec == 0L) "$whole%" else "$whole.$dec%"
}

@Composable
internal fun weekDayText(day: LocalDate, today: LocalDate): String {
    val todayLabel = stringResource(R.string.bill_recent_today)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formatted = formatCalendarDay(day, locale)
    return if (day == today) "$todayLabel · $formatted" else formatted
}

@Composable
private fun PendingLine(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_indicator_cd)
    Text(
        text = "—",
        color = MaterialTheme.appColors.textTertiary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Composable
internal fun PressFade(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .graphicsLayer { alpha = if (pressed && enabled) 0.72f else 1f }
            .then(modifier)
            .interactiveClick(
                enabled = enabled,
                interactionSource = interaction,
                onClick = onClick,
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
internal fun AccountMark(
    iconKey: String,
    colorKey: String = "",
    modifier: Modifier = Modifier,
    tileSize: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    outlined: Boolean = false,
    emphasized: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    val tone = softBadgeTone(colorKey)
    val container = if (emphasized) colors.inkStrong else tone.container
    val content = if (emphasized) colors.onInkStrong else tone.content
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(tileSize)
            .background(container, shape)
            .then(
                if (outlined) {
                    Modifier.border(1.dp, colors.divider, shape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = accountIconImage(iconKey),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun billPageListPadding(extraBottom: Dp): PaddingValues {
    val safe =
        screenContentWindowInsets()
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = safe.calculateLeftPadding(layoutDirection) + 18.dp,
        top = 8.dp,
        end = safe.calculateRightPadding(layoutDirection) + 18.dp,
        bottom = safe.calculateBottomPadding() + extraBottom,
    )
}

@Composable
internal fun BudgetStatusBanner(
    over: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    val tint = if (over) colors.expense else budgetNearColor(colors)
    val clickable = if (onClick == null) Modifier else Modifier.interactiveClick(onClick = onClick)
    Surface(
        modifier = modifier.fillMaxWidth().then(clickable),
        shape = MaterialTheme.shapes.medium,
        color = if (over) tint.copy(alpha = 0.10f) else colors.primaryContainer,
    ) {
        Text(
            text = text,
            color = if (over) tint else colors.primary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
internal fun BillEntryRow(
    bill: BillRowUi,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val title = when (bill.type) {
        BillType.Transfer -> stringResource(R.string.bill_transfer)
        else -> bill.categoryName ?: stringResource(R.string.bill_category_missing)
    }
    val amountColor = when (bill.type) {
        BillType.Income -> colors.income
        else -> colors.textPrimary
    }
    val remark = bill.remark?.trim().orEmpty()
    val meta = if (remark.isNotEmpty()) "${clockText(bill.time)} · $remark" else clockText(bill.time)
    val clickable = if (onClick == null) Modifier else Modifier.interactiveClick(onClick = onClick)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(clickable)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BillMark(bill = bill)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                color = colors.textTertiary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = maskIfHidden(signedMoneyText(currencySymbol, bill.signedAmount), hidden, currencySymbol),
            color = amountColor,
            style = MaterialTheme.typography.titleMedium.merge(
                TextStyle(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
            ),
            maxLines = 1,
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.65f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
internal fun billTimeText(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return formatBillTime(time, locale)
}
