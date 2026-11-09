package com.cyr.cognobal.screen.feature.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.data.repository.BudgetLevel
import com.cyr.cognobal.screen.feature.bill.model.accountColorValue
import com.cyr.cognobal.screen.feature.bill.model.accountIconImage
import com.cyr.cognobal.screen.feature.bill.model.pairedIcon
import com.cyr.cognobal.screen.feature.bill.state.AccountItemUi
import com.cyr.cognobal.screen.feature.bill.state.BillHomeUiState
import com.cyr.cognobal.screen.feature.bill.state.BillRowUi
import com.cyr.cognobal.screen.feature.bill.state.BudgetProgressUi
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.bill.tool.formatMoneyOrOverflow
import com.cyr.cognobal.screen.feature.expense.model.categoryIcon
import com.cyr.cognobal.screen.feature.expense.model.onCategory
import com.cyr.cognobal.ui.format.formatBillTime
import com.cyr.cognobal.ui.format.formatCalendarDay
import com.cyr.cognobal.ui.format.formatCalendarMonth
import com.cyr.cognobal.ui.insets.screenContentWindowInsets
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.theme.asThemeIdentity
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.budgetNearColor
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HomeContentMaxWidth = 840.dp

/**
 * 记账首页。账户卡、预算、最近 7 天从上往下排。
 *
 * 顶栏已经去掉，状态栏留白加在列表自己的 contentPadding 里。 卡片可以滚到状态栏后面，外层的遮罩负责把系统图标衬出来。
 */
@Composable
internal fun BillHomeList(
        uiState: BillHomeUiState,
        headerState: BillHomeUiState,
        onOpenAccounts: () -> Unit,
        onCreateAccount: () -> Unit,
        onViewAll: () -> Unit,
        onOpenBill: (Long) -> Unit,
        onManageBudget: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val base = billPageListPadding(extraBottom = 16.dp)
    val topInset =
            screenContentWindowInsets()
                    .only(WindowInsetsSides.Top)
                    .asPaddingValues()
                    .calculateTopPadding()
    val contentPadding =
            PaddingValues(
                    start = base.calculateLeftPadding(layoutDirection),
                    top = topInset + 16.dp,
                    end = base.calculateRightPadding(layoutDirection),
                    bottom = base.calculateBottomPadding(),
            )
    // 切换账户时名字先换。金额和近 7 天要等这笔账户的数字到了再填，避免新卡挂着上一户的数。
    val figuresReady =
            !uiState.loading &&
                    headerState.currentId != null &&
                    uiState.currentId == headerState.currentId
    LazyColumn(
            modifier =
                    modifier.widthIn(max = HomeContentMaxWidth)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .consumeWindowInsets(contentPadding),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "account") {
            AccountHero(
                    headerState = headerState,
                    uiState = uiState,
                    figuresReady = figuresReady,
                    onOpenAccounts = onOpenAccounts,
                    onCreateAccount = onCreateAccount,
            )
        }
        if (uiState.showBudget) {
            item(key = "budget") {
                BudgetBlock(
                        uiState = uiState,
                        onManage = onManageBudget,
                )
            }
        }
        item(key = "recent") {
            RecentBlock(
                    uiState = uiState,
                    headerState = headerState,
                    figuresReady = figuresReady,
                    onViewAll = onViewAll,
                    onOpenBill = onOpenBill,
            )
        }
    }
}

@Composable
private fun AccountHero(
        headerState: BillHomeUiState,
        uiState: BillHomeUiState,
        figuresReady: Boolean,
        onOpenAccounts: () -> Unit,
        onCreateAccount: () -> Unit,
) {
    val account = headerState.accounts.firstOrNull { it.id == headerState.currentId }
    when {
        headerState.loading && account == null -> AccountPlaceholder()
        account == null -> EmptyAccountCard(onCreateAccount = onCreateAccount)
        else ->
                AccountCard(
                        account = account,
                        currencySymbol = headerState.currencySymbol,
                        income = uiState.monthIncome,
                        expense = uiState.monthExpense,
                        balance = uiState.monthBalance,
                        balanceOverflow = uiState.monthOverflow,
                        figuresReady = figuresReady,
                        monthLabel = currentMonthLabel(uiState.today),
                        onOpenAccounts = onOpenAccounts,
                )
    }
}

/**
 * 结余卡。底色是坐进主题后的账户色，结余是唯一的大数字。
 * 收入和支出在下面分成两列。右下角的类型图标和账户名旁的色点，对得上实体卡。
 */
@Composable
private fun AccountCard(
        account: AccountItemUi,
        currencySymbol: String,
        income: Long,
        expense: Long,
        balance: Long,
        balanceOverflow: Boolean,
        figuresReady: Boolean,
        monthLabel: String,
        onOpenAccounts: () -> Unit,
) {
    val tint = accountColorValue(account.colorKey)
    val incomeText = cardAmount(currencySymbol, income, figuresReady)
    val expenseText = cardAmount(currencySymbol, expense, figuresReady)
    val balanceText = cardAmount(currencySymbol, balance, figuresReady, overflow = balanceOverflow)
    val switchInteraction = remember { MutableInteractionSource() }
    val cardCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    var switchAnchor by remember { mutableStateOf(SwitchAnchor.Empty) }
    AccountHeroSurface(color = tint) { paint ->
        val muted = paint.on.copy(alpha = 0.78f)
        Box(
                modifier =
                        Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                            cardCoords[0] = coordinates
                        },
        ) {
            Icon(
                    imageVector = account.type.pairedIcon().image,
                    contentDescription = null,
                    tint = paint.on.copy(alpha = 0.16f),
                    modifier =
                            Modifier.align(Alignment.BottomEnd)
                                    .padding(end = 16.dp, bottom = 16.dp)
                                    .size(32.dp),
            )
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                            text = stringResource(R.string.bill_card_month_balance, monthLabel),
                            color = muted,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                    )
                    AccountSwitchLabel(
                            account = account,
                            onColor = paint.on,
                            identity = paint.identity,
                            interaction = switchInteraction,
                            onPlaced = { coordinates ->
                                val parent = cardCoords[0] ?: return@AccountSwitchLabel
                                if (!parent.isAttached || !coordinates.isAttached) return@AccountSwitchLabel
                                val origin = parent.localPositionOf(coordinates, Offset.Zero)
                                val next =
                                        SwitchAnchor(
                                                x = origin.x.roundToInt(),
                                                y = origin.y.roundToInt(),
                                                width = coordinates.size.width,
                                                height = coordinates.size.height,
                                        )
                                if (next != switchAnchor) switchAnchor = next
                            },
                    )
                }
                Text(
                        text = balanceText,
                        color = paint.on,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style =
                                MaterialTheme.typography.displaySmall.merge(
                                        TextStyle(fontFeatureSettings = "tnum"),
                                ),
                        autoSize =
                                TextAutoSize.StepBased(
                                        minFontSize = 22.sp,
                                        maxFontSize = 34.sp,
                                        stepSize = 1.sp,
                                ),
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
                Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeroAmount(
                            label = stringResource(R.string.bill_figure_income),
                            value = incomeText,
                            color = muted,
                            modifier = Modifier.weight(1f),
                    )
                    HeroAmount(
                            label = stringResource(R.string.bill_figure_expense),
                            value = expenseText,
                            color = muted,
                            modifier = Modifier.weight(1f),
                    )
                }
            }
            AccountSwitchTarget(
                    account = account,
                    anchor = switchAnchor,
                    interaction = switchInteraction,
                    onOpenAccounts = onOpenAccounts,
            )
        }
    }
}

@Composable
private fun HeroAmount(
        label: String,
        value: String,
        color: Color,
        modifier: Modifier = Modifier,
) {
    Column(
            modifier = modifier.clearAndSetSemantics { contentDescription = "$label $value" },
    ) {
        Text(
                text = label,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
        )
        Text(
                text = value,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.merge(TextStyle(fontFeatureSettings = "tnum")),
        )
    }
}

/** 账户名只负责占位和按下变淡。点击落在 [AccountSwitchTarget]，行高保持文字高度。 */
@Composable
private fun AccountSwitchLabel(
        account: AccountItemUi,
        onColor: Color,
        identity: Color,
        interaction: MutableInteractionSource,
        onPlaced: (LayoutCoordinates) -> Unit,
) {
    val pressed by interaction.collectIsPressedAsState()
    Row(
            modifier =
                    Modifier.graphicsLayer { alpha = if (pressed) 0.72f else 1f }
                            .clearAndSetSemantics {}
                            .onGloballyPositioned(onPlaced),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
                modifier =
                        Modifier.size(8.dp)
                                .border(1.dp, onColor.copy(alpha = 0.40f), CircleShape)
                                .background(identity, CircleShape),
        )
        Text(
                text = account.name,
                color = onColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.widthIn(max = 120.dp),
        )
        Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = onColor.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 盖在账户名上的 48.dp 热区。挂在结余卡这一层，才不会把账户名那一行撑高。
 */
@Composable
private fun AccountSwitchTarget(
        account: AccountItemUi,
        anchor: SwitchAnchor,
        interaction: MutableInteractionSource,
        onOpenAccounts: () -> Unit,
) {
    if (!anchor.placed) return
    val density = LocalDensity.current
    val description = stringResource(R.string.account_switch_cd, account.name)
    val touch = 48.dp
    val touchPx = with(density) { touch.roundToPx() }
    val widthPx = maxOf(anchor.width, touchPx)
    val left = anchor.x + anchor.width - widthPx
    val top = (anchor.y + anchor.height / 2 - touchPx / 2).coerceAtLeast(0)
    Box(
            modifier =
                    Modifier.offset { IntOffset(left, top) }
                            .semantics { contentDescription = description }
                            .interactiveClick(interactionSource = interaction, onClick = onOpenAccounts)
                            .size(with(density) { widthPx.toDp() }, touch),
    )
}

private data class SwitchAnchor(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
) {
    val placed: Boolean
        get() = width > 0 && height > 0

    companion object {
        val Empty = SwitchAnchor(x = 0, y = 0, width = 0, height = 0)
    }
}

@Composable
private fun currentMonthLabel(today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return formatCalendarMonth(YearMonth.from(today), locale)
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

@Composable
private fun AccountPlaceholder() {
    LedgerCard { Spacer(modifier = Modifier.height(140.dp)) }
}

@Composable
private fun EmptyAccountCard(onCreateAccount: () -> Unit) {
    val colors = MaterialTheme.appColors
    LedgerCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                    text = stringResource(R.string.bill_account_empty_title),
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
            )
            Text(
                    text = stringResource(R.string.bill_account_empty_body),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp),
            )
            PressFade(
                    onClick = onCreateAccount,
                    modifier =
                            Modifier.padding(top = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.primary)
                                    .heightIn(min = 48.dp),
                    contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(18.dp),
                    )
                    Text(
                            text = stringResource(R.string.bill_account_create),
                            color = colors.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * 首页只画当前周期里启用的总预算。分类预算留在设置页。
 * 没到预警线用主色，到了预警线换接近色，花满换成支出色。
 * 进度条下面左侧是本月已支出，右侧是本月总预算。
 */
@Composable
private fun BudgetBlock(
        uiState: BillHomeUiState,
        onManage: () -> Unit,
) {
    val budget = homeBudget(uiState.budgets)
    LedgerCard {
        SectionHeader(
                title = stringResource(R.string.bill_budget_title),
                supporting = currentMonthLabel(uiState.today),
                action = stringResource(R.string.bill_budget_manage),
                onAction = onManage,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp),
        )
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            when {
                uiState.loading && budget == null -> BudgetTrackPlaceholder()
                budget == null ->
                        Text(
                                text = stringResource(R.string.bill_budget_empty),
                                color = MaterialTheme.appColors.textTertiary,
                                fontSize = 14.sp,
                        )
                else -> HomeBudgetBar(budget, uiState.currencySymbol)
            }
        }
    }
}

@Composable
private fun HomeBudgetBar(
        budget: BudgetProgressUi,
        currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    val description =
            stringResource(
                    R.string.bill_budget_progress_cd,
                    budgetPercentLabel(budget.spent, budget.limit),
            )
    val color =
            when (budget.level) {
                BudgetLevel.Over -> colors.expense
                BudgetLevel.Near -> budgetNearColor(colors.isDark)
                BudgetLevel.Normal -> colors.primary
            }
    val amountStyle =
            MaterialTheme.typography.labelLarge.merge(TextStyle(fontFeatureSettings = "tnum"))
    val spentText = formatMoney(currencySymbol, budget.spent)
    val limitText = formatMoney(currencySymbol, budget.limit)
    val spentDescription = stringResource(R.string.bill_figure_expense) + " " + spentText
    val limitDescription = stringResource(R.string.bill_budget_global) + " " + limitText
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                text = budgetPercentLabel(budget.spent, budget.limit),
                color = color,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium.merge(TextStyle(fontFeatureSettings = "tnum")),
                modifier = Modifier.padding(bottom = 8.dp).clearAndSetSemantics {},
        )
        LinearProgressIndicator(
                progress = { budget.ratio },
                modifier =
                        Modifier.fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .semantics { contentDescription = description },
                color = color,
                trackColor = colors.tagBackground,
                drawStopIndicator = {},
        )
        Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                    text = spentText,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = amountStyle,
                    modifier =
                            Modifier.weight(1f).semantics {
                                contentDescription = spentDescription
                            },
            )
            Text(
                    text = limitText,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    style = amountStyle,
                    modifier =
                            Modifier.weight(1f).semantics {
                                contentDescription = limitDescription
                            },
            )
        }
    }
}

/** 加载时的进度槽，高度和进度条一致。 */
@Composable
private fun BudgetTrackPlaceholder() {
    val description = stringResource(R.string.loading_indicator_cd)
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.appColors.tagBackground)
                            .clearAndSetSemantics { contentDescription = description },
    )
}

/** 总预算的分类名为空，且不是缺失分类。同一周期里启用的总预算最多一条。 */
private fun homeBudget(budgets: List<BudgetProgressUi>): BudgetProgressUi? =
        budgets.firstOrNull { budget -> budget.categoryName == null && !budget.missingCategory }

private fun budgetPercentLabel(spent: Long, limit: Long): String {
    if (limit <= 0L || spent <= 0L) return "0%"
    val percent =
            try {
                Math.multiplyExact(spent, 100L) / limit
            } catch (_: ArithmeticException) {
                return "999+%"
            }
    return if (percent > 999L) "999+%" else "$percent%"
}

@Composable
private fun RecentBlock(
        uiState: BillHomeUiState,
        headerState: BillHomeUiState,
        figuresReady: Boolean,
        onViewAll: () -> Unit,
        onOpenBill: (Long) -> Unit,
) {
    val pending = headerState.currentId != null && !figuresReady
    val showEmptyAccount = !headerState.loading && headerState.currentId == null
    LedgerCard {
        SectionHeader(
                title = stringResource(R.string.bill_recent_title),
                action =
                        if (headerState.currentId != null) {
                            stringResource(R.string.bill_recent_all)
                        } else {
                            null
                        },
                onAction = if (headerState.currentId != null) onViewAll else null,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp),
        )
        when {
            headerState.loading && headerState.currentId == null ->
                    PendingLine(modifier = recentMessagePadding)
            pending -> PendingLine(modifier = recentMessagePadding)
            showEmptyAccount || uiState.recent.isEmpty() ->
                    Text(
                            text = stringResource(R.string.bill_recent_week_empty),
                            color = MaterialTheme.appColors.textTertiary,
                            fontSize = 14.sp,
                            modifier = recentMessagePadding,
                    )
            else ->
                    WeekBillList(
                            bills = uiState.recent,
                            currencySymbol = uiState.currencySymbol,
                            today = uiState.today,
                            onOpenBill = onOpenBill,
                    )
        }
    }
}

@Composable
private fun WeekBillList(
        bills: List<BillRowUi>,
        currencySymbol: String,
        today: LocalDate,
        onOpenBill: (Long) -> Unit,
) {
    val days = remember(bills) { bills.groupBy { it.time.toLocalDate() }.entries.toList() }
    val colors = MaterialTheme.appColors
    Column(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { dayIndex, (day, rows) ->
            Text(
                    text = weekDayText(day, today),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier =
                            Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = if (dayIndex == 0) 14.dp else 16.dp,
                            ),
            )
            rows.forEach { bill ->
                WeekBillRow(
                        bill = bill,
                        currencySymbol = currencySymbol,
                        onClick = { onOpenBill(bill.id) },
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun WeekBillRow(
        bill: BillRowUi,
        currencySymbol: String,
        onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val title =
            when (bill.type) {
                BillType.Transfer -> stringResource(R.string.bill_transfer)
                else -> bill.categoryName ?: stringResource(R.string.bill_category_missing)
            }
    val amountColor =
            when {
                bill.type == BillType.Income -> colors.income
                else -> colors.textPrimary
            }
    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .interactiveClick(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        BillMark(bill = bill)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
            )
            Text(
                    text = clockText(bill.time),
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
                text = formatMoney(currencySymbol, bill.signedAmount),
                color = amountColor,
                style = MaterialTheme.typography.titleMedium.merge(TextStyle(fontFeatureSettings = "tnum")),
                maxLines = 1,
        )
    }
}

/** 和日期标题同一套左右边距，空状态和加载行不会贴边。 */
private val recentMessagePadding = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)

@Composable
private fun BillMark(bill: BillRowUi) {
    val colors = MaterialTheme.appColors
    val tint =
            when {
                bill.type == BillType.Transfer -> colors.textSecondary
                bill.categoryColorKey != null -> accountColorValue(bill.categoryColorKey).asThemeIdentity()
                bill.type == BillType.Income -> colors.income
                else -> colors.textSecondary
            }
    val icon =
            when (bill.type) {
                BillType.Transfer -> Icons.Outlined.SwapHoriz
                else -> categoryIcon(bill.categoryIconKey.orEmpty())
            }
    Box(
            modifier =
                    Modifier.size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint.copy(alpha = if (colors.isDark) 0.22f else 0.12f)),
            contentAlignment = Alignment.Center,
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun weekDayText(day: LocalDate, today: LocalDate): String {
    val todayLabel = stringResource(R.string.bill_recent_today)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formatted = formatCalendarDay(day, locale)
    return if (day == today) "$todayLabel · $formatted" else formatted
}

@Composable
private fun clockText(time: LocalDateTime): String {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT) }
    return time.format(formatter)
}

@Composable
private fun SectionHeader(
        title: String,
        supporting: String? = null,
        action: String? = null,
        onAction: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                        text = supporting,
                        color = colors.textTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (action != null && onAction != null) {
            PressFade(onClick = onAction) {
                Row(
                        modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                            text = action,
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                    )
                    Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingLine(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_indicator_cd)
    Text(
            text = "—",
            color = MaterialTheme.appColors.textTertiary,
            fontSize = 14.sp,
            modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

@Composable
private fun LedgerCard(
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    val shape = MaterialTheme.shapes.extraLarge
    Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = colors.cardBackground,
            shadowElevation = 0.dp,
    ) { Column(content = content) }
}

/** 按下时整块变淡。背景放在 [modifier] 里，淡化才会连底色一起走。 */
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
            modifier =
                    Modifier.graphicsLayer { alpha = if (pressed && enabled) 0.72f else 1f }
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

/** 账户角标。卡片上加一圈浅边，和卡面拉开。 */
@Composable
internal fun AccountMark(
        iconKey: String,
        colorKey: String,
        modifier: Modifier = Modifier,
        tileSize: Dp = 36.dp,
        iconSize: Dp = 18.dp,
        outlined: Boolean = false,
) {
    val tint = accountColorValue(colorKey).asThemeIdentity()
    val shape = MaterialTheme.shapes.small
    Box(
            modifier =
                    modifier.size(tileSize)
                            .background(tint, shape)
                            .then(
                                    if (outlined) {
                                        Modifier.border(
                                                1.dp,
                                                Color.White.copy(alpha = 0.55f),
                                                shape
                                        )
                                    } else {
                                        Modifier
                                    },
                            ),
            contentAlignment = Alignment.Center,
    ) {
        Icon(
                imageVector = accountIconImage(iconKey),
                contentDescription = null,
                tint = tint.onCategory(),
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
            start = safe.calculateLeftPadding(layoutDirection) + 16.dp,
            top = 8.dp,
            end = safe.calculateRightPadding(layoutDirection) + 16.dp,
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
    val tint = if (over) colors.expense else budgetNearColor(colors.isDark)
    val clickable =
            if (onClick == null) {
                Modifier
            } else {
                Modifier.interactiveClick(onClick = onClick)
            }
    Surface(
            modifier = modifier.fillMaxWidth().then(clickable),
            shape = RoundedCornerShape(16.dp),
            color = tint.copy(alpha = 0.14f),
    ) {
        Text(
                text = text,
                color = tint,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
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
    val title =
            when (bill.type) {
                BillType.Transfer -> stringResource(R.string.bill_transfer)
                else -> bill.categoryName ?: stringResource(R.string.bill_category_missing)
            }
    val amountColor =
            when {
                bill.type == BillType.Income -> colors.income
                else -> colors.textPrimary
            }
    val icon =
            when (bill.type) {
                BillType.Expense -> Icons.Outlined.ArrowDownward
                BillType.Income -> Icons.Outlined.ArrowUpward
                BillType.Transfer -> Icons.Outlined.SwapHoriz
            }
    val shape = MaterialTheme.shapes.extraLarge
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier.interactiveClick(onClick = onClick)
    }
    Surface(
            modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).then(clickable),
            shape = shape,
            color = colors.cardBackground,
            shadowElevation = 0.dp,
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                    modifier =
                            Modifier.size(36.dp)
                                    .background(
                                            amountColor.copy(
                                                    alpha = if (colors.isDark) 0.2f else 0.12f
                                            ),
                                            CircleShape
                                    ),
                    contentAlignment = Alignment.Center,
            ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(18.dp),
                )
            }
            Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Text(
                        text = title,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                )
                Text(
                        text = billTimeText(bill.time),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                    text = formatMoney(currencySymbol, bill.signedAmount),
                    color = amountColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
            )
        }
    }
}

@Composable
internal fun billTimeText(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return formatBillTime(time, locale)
}
