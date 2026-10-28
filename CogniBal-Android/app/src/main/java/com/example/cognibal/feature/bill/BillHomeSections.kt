package com.example.cognibal.screen.feature.bill

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
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.bill.model.accountIconImage
import com.example.cognibal.screen.feature.bill.state.AccountItemUi
import com.example.cognibal.screen.feature.bill.state.BillHomeUiState
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.bill.state.BudgetProgressUi
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.expense.model.categoryIcon
import com.example.cognibal.screen.feature.expense.model.onCategory
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.CogniBalCardElevation
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.ui.theme.budgetNearColor
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
                        onOpenAccounts = onOpenAccounts,
                )
    }
}

/**
 * 结余卡。底色是账户自己的颜色，浅色和深色只改明暗。
 * 左上是当前月份的结余，收入和支出在底下一行。
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
        onOpenAccounts: () -> Unit,
) {
    val tint = accountColorValue(account.colorKey)
    val incomeText = cardAmount(currencySymbol, income, figuresReady)
    val expenseText = cardAmount(currencySymbol, expense, figuresReady)
    val balanceText = cardAmount(currencySymbol, balance, figuresReady, overflow = balanceOverflow)
    AccountHeroSurface(color = tint) { paint ->
        val muted = paint.on.copy(alpha = 0.78f)
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                        text = stringResource(R.string.bill_card_month_balance, currentMonthLabel()),
                        color = muted,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                )
                AccountSwitchButton(
                        account = account,
                        onColor = paint.on,
                        onOpenAccounts = onOpenAccounts,
                )
            }
            Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        modifier = Modifier.weight(1f),
                )
                Icon(
                        imageVector = accountIconImage(account.iconKey),
                        contentDescription = null,
                        tint = paint.on.copy(alpha = 0.28f),
                        modifier = Modifier.padding(start = 8.dp).size(32.dp),
                )
            }
            Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HeroAmount(
                        label = stringResource(R.string.bill_figure_income),
                        value = incomeText,
                        color = muted,
                )
                HeroAmount(
                        label = stringResource(R.string.bill_figure_expense),
                        value = expenseText,
                        color = muted,
                )
            }
        }
    }
}

@Composable
private fun HeroAmount(
        label: String,
        value: String,
        color: Color,
) {
    Text(
            text = "$label $value",
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall.merge(TextStyle(fontFeatureSettings = "tnum")),
            modifier = Modifier.clearAndSetSemantics { contentDescription = "$label $value" },
    )
}

@Composable
private fun AccountSwitchButton(
        account: AccountItemUi,
        onColor: Color,
        onOpenAccounts: () -> Unit,
) {
    val description = stringResource(R.string.account_switch_cd, account.name)
    PressFade(
            onClick = onOpenAccounts,
            modifier =
                    Modifier.collapseTouchHeight()
                            .semantics { contentDescription = description },
            contentAlignment = Alignment.CenterEnd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                    text = account.name,
                    color = onColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.widthIn(max = 132.dp),
            )
            Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = onColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 点击热区仍按 48.dp，但不再把这一行撑高，避免结余卡顶部空出一截。 */
private fun Modifier.collapseTouchHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val textHeight = 28.dp.roundToPx()
    if (placeable.height <= textHeight) {
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        layout(placeable.width, textHeight) {
            placeable.place(0, (textHeight - placeable.height) / 2)
        }
    }
}

@Composable
private fun currentMonthLabel(): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "zh") "yyyy年M月" else "MMM yyyy"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return YearMonth.now().format(formatter)
}

@Composable
private fun cardAmount(
        symbol: String,
        amount: Long,
        ready: Boolean,
        overflow: Boolean = false,
): String {
    val overflowText = stringResource(R.string.account_balance_overflow)
    if (!ready) return "—"
    if (overflow) return overflowText
    return symbol + formatMinorAmount(amount)
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
                supporting = currentMonthLabel(),
                action = stringResource(R.string.bill_budget_manage),
                onAction = onManage,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp),
        )
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            when {
                uiState.loading && budget == null -> PendingLine()
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
    val spentText = currencySymbol + formatMinorAmount(budget.spent)
    val limitText = currencySymbol + formatMinorAmount(budget.limit)
    val spentDescription = stringResource(R.string.bill_figure_expense) + " " + spentText
    val limitDescription = stringResource(R.string.bill_budget_global) + " " + limitText
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
                progress = { budget.ratio },
                modifier =
                        Modifier.fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
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
                    color = colors.textSecondary,
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
                    color = colors.textSecondary,
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
                    PendingLine(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            pending -> PendingLine(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
            showEmptyAccount || uiState.recent.isEmpty() ->
                    Text(
                            text = stringResource(R.string.bill_recent_week_empty),
                            color = MaterialTheme.appColors.textTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    )
            else ->
                    WeekBillList(
                            bills = uiState.recent,
                            currencySymbol = uiState.currencySymbol,
                    )
        }
    }
}

@Composable
private fun WeekBillList(
        bills: List<BillRowUi>,
        currencySymbol: String,
) {
    val days = remember(bills) { bills.groupBy { it.time.toLocalDate() }.entries.toList() }
    val colors = MaterialTheme.appColors
    Column(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { dayIndex, (day, rows) ->
            Text(
                    text = weekDayText(day),
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
                WeekBillRow(bill = bill, currencySymbol = currencySymbol)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun WeekBillRow(
        bill: BillRowUi,
        currencySymbol: String,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        BillMark(bill = bill)
        Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                    text = currencySymbol + formatMinorAmount(bill.signedAmount),
                    color = amountColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
            )
            Text(
                    text = clockText(bill.time),
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun BillMark(bill: BillRowUi) {
    val colors = MaterialTheme.appColors
    val tint =
            when {
                bill.type == BillType.Transfer -> colors.textSecondary
                bill.categoryColorKey != null -> accountColorValue(bill.categoryColorKey)
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
private fun weekDayText(day: LocalDate): String {
    val todayLabel = stringResource(R.string.bill_recent_today)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "zh") "M月d日 EEE" else "EEE, MMM d"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    val formatted = day.format(formatter)
    return if (day == LocalDate.now()) "$todayLabel · $formatted" else formatted
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
                        modifier = Modifier.heightIn(min = 32.dp).padding(horizontal = 2.dp),
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
    Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.cardBackground,
            shadowElevation = CogniBalCardElevation,
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
    val tint = accountColorValue(colorKey)
    val shape = RoundedCornerShape(8.dp)
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
    Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.cardBackground,
            shadowElevation = CogniBalCardElevation,
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
                    text = currencySymbol + formatMinorAmount(bill.signedAmount),
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
    val pattern = if (locale.language == "zh") "M月d日 HH:mm" else "MMM d, HH:mm"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return time.format(formatter)
}
