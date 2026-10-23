package com.example.cognibal.screen.feature.bill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.bill.state.BillHomeUiState
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.bill.state.BudgetProgressUi
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.ui.theme.budgetNearColor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HomeContentMaxWidth = 840.dp

/**
 * 记账首页的滚动内容。安全区放进 contentPadding，卡片可以滚过系统栏，又不会被挡住。
 * 顶栏已经吃掉状态栏，这里只补左右和下方。
 */
@Composable
internal fun BillHomeList(
    uiState: BillHomeUiState,
    onViewAll: () -> Unit,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentPadding = billPageListPadding(extraBottom = 16.dp)
    val banner = budgetHomeBanner(uiState.budgets)
    LazyColumn(
        modifier = modifier
            .widthIn(max = HomeContentMaxWidth)
            .fillMaxHeight()
            .consumeWindowInsets(contentPadding),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "assets") {
            TotalAssetsCard(uiState = uiState)
        }
        item(key = "month") {
            MonthOverviewCard(uiState = uiState)
        }
        if (banner != null) {
            item(key = "budget-banner") {
                BudgetStatusBanner(
                    over = banner.over,
                    text = banner.text,
                    onClick = onOpenBudget,
                )
            }
        }
        if (uiState.budgets.isNotEmpty()) {
            item(key = "budgets") {
                BudgetCard(budgets = uiState.budgets, currencySymbol = uiState.currencySymbol)
            }
        }
        item(key = "recent-title") {
            RecentHeader(
                enabled = uiState.currentId != null,
                onViewAll = onViewAll,
            )
        }
        if (uiState.recent.isEmpty() && !uiState.loading) {
            item(key = "recent-empty") {
                EmptyRecent()
            }
        } else {
            items(items = uiState.recent, key = { bill -> bill.id }) { bill ->
                BillEntryRow(bill = bill, currencySymbol = uiState.currencySymbol)
            }
        }
    }
}

@Composable
internal fun billPageListPadding(extraBottom: Dp): PaddingValues {
    val safe = screenContentWindowInsets()
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
private fun TotalAssetsCard(uiState: BillHomeUiState) {
    val colors = MaterialTheme.appColors
    HomeCard {
        Text(
            text = stringResource(R.string.bill_total_assets),
            color = colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (uiState.totalOverflow) {
                stringResource(R.string.account_balance_overflow)
            } else {
                uiState.currencySymbol + formatMinorAmount(uiState.totalAssets)
            },
            color = when {
                uiState.totalOverflow -> colors.textSecondary
                uiState.totalAssets < 0L -> colors.expense
                else -> colors.textPrimary
            },
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MonthOverviewCard(uiState: BillHomeUiState) {
    val colors = MaterialTheme.appColors
    HomeCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            MonthColumn(
                label = stringResource(R.string.bill_month_income),
                amount = uiState.monthIncome,
                overflow = false,
                color = colors.income,
                symbol = uiState.currencySymbol,
                modifier = Modifier.weight(1f),
            )
            MonthColumn(
                label = stringResource(R.string.bill_month_expense),
                amount = uiState.monthExpense,
                overflow = false,
                color = colors.expense,
                symbol = uiState.currencySymbol,
                modifier = Modifier.weight(1f),
            )
            MonthColumn(
                label = stringResource(R.string.bill_month_balance),
                amount = uiState.monthBalance,
                overflow = uiState.monthOverflow,
                color = when {
                    uiState.monthOverflow -> colors.textSecondary
                    uiState.monthBalance < 0L -> colors.expense
                    else -> colors.income
                },
                symbol = uiState.currencySymbol,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthColumn(
    label: String,
    amount: Long,
    overflow: Boolean,
    color: Color,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = MaterialTheme.appColors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (overflow) {
                stringResource(R.string.account_balance_overflow)
            } else {
                symbol + formatMinorAmount(amount)
            },
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun BudgetCard(
    budgets: List<BudgetProgressUi>,
    currencySymbol: String,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val colors = MaterialTheme.appColors
    val description = stringResource(
        if (expanded) R.string.bill_budget_collapse_cd else R.string.bill_budget_expand_cd,
    )
    HomeCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
                .interactiveClick(onClick = { expanded = !expanded }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bill_budget_title),
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.budget_home_scope),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                budgets.forEachIndexed { index, budget ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = colors.divider,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    BudgetRow(budget = budget, currencySymbol = currencySymbol)
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(
    budget: BudgetProgressUi,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    val title = when {
        budget.missingCategory -> stringResource(R.string.bill_budget_missing)
        budget.categoryName == null -> stringResource(R.string.bill_budget_global)
        else -> budget.categoryName
    }
    Column(modifier = Modifier.fillMaxWidth()) {
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
                text = currencySymbol + formatMinorAmount(budget.spent) +
                    " / " + currencySymbol + formatMinorAmount(budget.limit),
                color = if (budget.level == BudgetLevel.Normal) colors.textSecondary else budgetTint(budget.level, colors.isDark, colors.expense),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LinearProgressIndicator(
            progress = { budget.ratio },
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = if (budget.level == BudgetLevel.Normal) colors.primary else budgetTint(budget.level, colors.isDark, colors.expense),
            trackColor = colors.tagBackground,
            drawStopIndicator = {},
        )
    }
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
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier.interactiveClick(onClick = onClick)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable),
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

private data class HomeBanner(val over: Boolean, val text: String)

@Composable
private fun budgetHomeBanner(budgets: List<BudgetProgressUi>): HomeBanner? {
    val concerning = budgets.filter { it.level != BudgetLevel.Normal }
    if (concerning.isEmpty()) return null
    val over = concerning.any { it.level == BudgetLevel.Over }
    val worst = if (over) BudgetLevel.Over else BudgetLevel.Near
    val separator = stringResource(R.string.budget_name_separator)
    val global = stringResource(R.string.bill_budget_global)
    val missing = stringResource(R.string.bill_budget_missing)
    val names = concerning.filter { it.level == worst }.joinToString(separator) { budget ->
        when {
            budget.missingCategory -> missing
            budget.categoryName == null -> global
            else -> budget.categoryName
        }
    }
    return HomeBanner(
        over = over,
        text = stringResource(if (over) R.string.budget_banner_over else R.string.budget_banner_near, names),
    )
}

private fun budgetTint(level: BudgetLevel, dark: Boolean, expense: Color): Color = when (level) {
    BudgetLevel.Over -> expense
    BudgetLevel.Near -> budgetNearColor(dark)
    BudgetLevel.Normal -> expense
}

@Composable
private fun RecentHeader(
    enabled: Boolean,
    onViewAll: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.bill_recent_title),
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (enabled) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .interactiveClick(onClick = onViewAll)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bill_recent_all),
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyRecent() {
    Text(
        text = stringResource(R.string.bill_recent_empty),
        color = MaterialTheme.appColors.textTertiary,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
internal fun BillEntryRow(
    bill: BillRowUi,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val title = when (bill.type) {
        BillType.Transfer -> stringResource(R.string.bill_transfer)
        else -> bill.categoryName ?: stringResource(R.string.bill_category_missing)
    }
    val amountColor = when {
        bill.signedAmount < 0L -> colors.expense
        bill.type == BillType.Income -> colors.income
        else -> colors.textPrimary
    }
    val icon = when (bill.type) {
        BillType.Expense -> Icons.Rounded.ArrowDownward
        BillType.Income -> Icons.Rounded.ArrowUpward
        BillType.Transfer -> Icons.Rounded.SwapHoriz
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(amountColor.copy(alpha = if (colors.isDark) 0.2f else 0.12f), CircleShape),
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = billTimeText(bill.time),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = currencySymbol + formatMinorAmount(bill.signedAmount),
                color = amountColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HomeCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.appColors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            content()
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
