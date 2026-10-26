package com.example.cognibal.screen.feature.bill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.bill.model.accountIconImage
import com.example.cognibal.screen.feature.bill.model.labelRes
import com.example.cognibal.screen.feature.bill.state.AccountItemUi
import com.example.cognibal.screen.feature.bill.state.BillHomeUiState
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.bill.state.BudgetProgressUi
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.expense.model.categoryIcon
import com.example.cognibal.screen.feature.expense.model.onCategory
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.ui.theme.budgetNearColor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HomeContentMaxWidth = 840.dp
private val AccountCardShape = RoundedCornerShape(22.dp)
private val AccountPlateShape = RoundedCornerShape(14.dp)
private val LedgerCardShape = RoundedCornerShape(16.dp)

/**
 * 记账首页。账户卡、预算、最近 7 天从上往下排。
 *
 * 顶栏已经去掉，状态栏留白加在列表自己的 contentPadding 里。
 * 卡片可以滚到状态栏后面，外层的遮罩负责把系统图标衬出来。
 */
@Composable
internal fun BillHomeList(
    uiState: BillHomeUiState,
    headerState: BillHomeUiState,
    onOpenAccounts: () -> Unit,
    onCreateAccount: () -> Unit,
    onViewAll: () -> Unit,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val base = billPageListPadding(extraBottom = 16.dp)
    val topInset = screenContentWindowInsets()
        .only(WindowInsetsSides.Top)
        .asPaddingValues()
        .calculateTopPadding()
    val contentPadding = PaddingValues(
        start = base.calculateLeftPadding(layoutDirection),
        top = topInset + 16.dp,
        end = base.calculateRightPadding(layoutDirection),
        bottom = base.calculateBottomPadding(),
    )
    // 切换账户时名字先换。金额和近 7 天要等这笔账户的数字到了再填，避免新卡挂着上一户的数。
    val figuresReady = !uiState.loading &&
        headerState.currentId != null &&
        uiState.currentId == headerState.currentId
    LazyColumn(
        modifier = modifier
            .widthIn(max = HomeContentMaxWidth)
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
        item(key = "budget") {
            BudgetBlock(
                uiState = uiState,
                onOpenBudget = onOpenBudget,
            )
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
        else -> AccountCard(
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
 * 当前账户。卡面颜色和圆环更明显，金额区盖一层磨砂，把数字从背景里托出来。
 * 结余占左侧，支出和收入在右侧上下排，结余能放下更长的金额。
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
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(account.colorKey)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val incomeText = cardAmount(currencySymbol, income, figuresReady)
    val expenseText = cardAmount(currencySymbol, expense, figuresReady)
    val balanceText = cardAmount(currencySymbol, balance, figuresReady, overflow = balanceOverflow)
    val incomeColor = if (figuresReady && income > 0L) colors.income else colors.textPrimary
    val expenseColor = if (figuresReady && expense > 0L) colors.expense else colors.textPrimary
    val balanceColor = when {
        !figuresReady || balanceOverflow -> colors.textPrimary
        balance < 0L -> colors.expense
        else -> colors.textPrimary
    }
    val frost = if (colors.isDark) Color(0xFF101418).copy(alpha = 0.64f) else Color.White.copy(alpha = 0.76f)
    val frostSheen = Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.62f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = AccountCardShape,
                clip = false,
                ambientColor = tint.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(AccountCardShape),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawAccountBackdrop(tint = tint, isDark = colors.isDark, rtl = rtl)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            AccountIdentity(
                account = account,
                onOpenAccounts = onOpenAccounts,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = AccountPlateShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (colors.isDark) 0.28f else 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.06f),
                    )
                    .clip(AccountPlateShape),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(frost),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(frostSheen, Color.Transparent),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CardFigure(
                        label = stringResource(R.string.bill_month_balance),
                        value = balanceText,
                        valueColor = balanceColor,
                        ready = figuresReady,
                        amountSize = 22.sp,
                        modifier = Modifier
                            .weight(1.4f)
                            .padding(end = 12.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CardFigureLine(
                            label = stringResource(R.string.bill_month_expense),
                            value = expenseText,
                            valueColor = expenseColor,
                            ready = figuresReady,
                        )
                        CardFigureLine(
                            label = stringResource(R.string.bill_month_income),
                            value = incomeText,
                            valueColor = incomeColor,
                            ready = figuresReady,
                        )
                    }
                }
            }
        }
    }
}

/** 卡面装饰。颜色铺开，右上角四圈大小不同的圆环。 */
private fun DrawScope.drawAccountBackdrop(tint: Color, isDark: Boolean, rtl: Boolean) {
    val paper = if (isDark) Color(0xFF242B32) else Color(0xFFFFFCF8)
    val calm = tint.copy(alpha = if (isDark) 0.36f else 0.26f).compositeOver(paper)
    val rich = tint.copy(alpha = if (isDark) 0.66f else 0.48f).compositeOver(paper)
    val startX = if (rtl) size.width else 0f
    val endX = if (rtl) 0f else size.width
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(calm, rich),
            start = Offset(startX, 0f),
            end = Offset(endX, size.height),
        ),
    )
    val focal = Offset(
        x = if (rtl) -size.minDimension * 0.02f else size.width + size.minDimension * 0.02f,
        y = size.height * 0.08f,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                tint.copy(alpha = if (isDark) 0.50f else 0.36f),
                tint.copy(alpha = 0f),
            ),
            center = focal,
            radius = size.minDimension * 1.15f,
        ),
        radius = size.minDimension * 1.15f,
        center = focal,
    )
    listOf(0.46f to 4.dp, 0.74f to 7.dp, 1.06f to 10.dp, 1.42f to 13.dp).forEach { (scale, stroke) ->
        drawCircle(
            brush = ringBrush(tint, focal, isDark),
            radius = size.minDimension * scale,
            center = focal,
            style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun ringBrush(tint: Color, center: Offset, isDark: Boolean): Brush {
    val highlight = if (isDark) 0.28f else 0.70f
    val body = if (isDark) 0.62f else 0.46f
    return Brush.sweepGradient(
        colors = listOf(
            tint.copy(alpha = 0.08f),
            Color.White.copy(alpha = highlight),
            tint.copy(alpha = body),
            Color.White.copy(alpha = highlight * 0.45f),
            tint.copy(alpha = 0.08f),
        ),
        center = center,
    )
}

@Composable
private fun AccountIdentity(
    account: AccountItemUi,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountGlyph(account = account)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            val colors = MaterialTheme.appColors
            Text(
                text = account.name,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(account.type.labelRes()),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!account.enabled) {
                    Text(
                        text = stringResource(R.string.account_status_disabled),
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                if (colors.isDark) Color(0xFF141A20) else Color.White,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        AccountSwitchButton(account = account, onOpenAccounts = onOpenAccounts)
    }
}

@Composable
private fun AccountSwitchButton(
    account: AccountItemUi,
    onOpenAccounts: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val description = stringResource(R.string.account_switch_cd, account.name)
    PressFade(
        onClick = onOpenAccounts,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(if (colors.isDark) Color(0xFF141A20) else Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SwitchAccount,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AccountGlyph(account: AccountItemUi) {
    val tint = accountColorValue(account.colorKey)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(
                elevation = 1.dp,
                shape = shape,
                clip = false,
                ambientColor = tint.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(shape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = accountIconImage(account.iconKey),
            contentDescription = null,
            tint = tint.onLightPlate(),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 浅色角标上，太浅的账户色改成深字，否则图标会消失。 */
private fun Color.onLightPlate(): Color {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return if (luminance > 0.72f) Color(0xFF212529) else this
}

@Composable
private fun CardFigure(
    label: String,
    value: String,
    valueColor: Color,
    ready: Boolean,
    modifier: Modifier = Modifier,
    amountSize: TextUnit = 15.sp,
) {
    val colors = MaterialTheme.appColors
    val loadingLabel = stringResource(R.string.loading_indicator_cd)
    val spoken = if (ready) value else loadingLabel
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$label $spoken"
        },
    ) {
        Text(
            text = label,
            color = colors.textTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = amountSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun CardFigureLine(
    label: String,
    value: String,
    valueColor: Color,
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val loadingLabel = stringResource(R.string.loading_indicator_cd)
    val spoken = if (ready) value else loadingLabel
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label $spoken" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(start = 6.dp),
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
    val overflowText = stringResource(R.string.account_balance_overflow)
    if (!ready) return "—"
    if (overflow) return overflowText
    return symbol + formatMinorAmount(amount)
}

@Composable
private fun AccountPlaceholder() {
    LedgerCard {
        Spacer(modifier = Modifier.height(140.dp))
    }
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
                modifier = Modifier
                    .padding(top = 16.dp)
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
                        imageVector = Icons.Rounded.Add,
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

@Composable
private fun BudgetBlock(
    uiState: BillHomeUiState,
    onOpenBudget: () -> Unit,
) {
    val banner = budgetHomeBanner(uiState.budgets)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = stringResource(R.string.bill_budget_title),
            supporting = stringResource(R.string.budget_home_scope),
            action = stringResource(R.string.bill_budget_manage),
            onAction = onOpenBudget,
        )
        LedgerCard {
            Column(modifier = Modifier.padding(16.dp)) {
                if (banner != null) {
                    BudgetAlert(
                        over = banner.over,
                        text = banner.text,
                        onClick = onOpenBudget,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                when {
                    uiState.loading && uiState.budgets.isEmpty() -> PendingLine()
                    uiState.budgets.isEmpty() -> Text(
                        text = stringResource(R.string.bill_budget_empty),
                        color = MaterialTheme.appColors.textTertiary,
                        fontSize = 14.sp,
                    )
                    else -> {
                        uiState.budgets.forEachIndexed { index, budget ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.appColors.divider,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                            BudgetLine(
                                budget = budget,
                                currencySymbol = uiState.currencySymbol,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAlert(
    over: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val tint = if (over) colors.expense else budgetNearColor(colors.isDark)
    PressFade(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f)),
    ) {
        Text(
            text = text,
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun BudgetLine(
    budget: BudgetProgressUi,
    currencySymbol: String,
) {
    val colors = MaterialTheme.appColors
    val missing = stringResource(R.string.bill_budget_missing)
    val global = stringResource(R.string.bill_budget_global)
    val title = when {
        budget.missingCategory -> missing
        budget.categoryName == null -> global
        else -> budget.categoryName
    }
    val accent = if (budget.level == BudgetLevel.Normal) {
        colors.primary
    } else {
        budgetTint(budget.level, colors.isDark, colors.expense)
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
                text = budgetPercentLabel(budget.spent, budget.limit),
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        Text(
            text = currencySymbol + formatMinorAmount(budget.spent) +
                " / " + currencySymbol + formatMinorAmount(budget.limit),
            color = if (budget.level == BudgetLevel.Normal) colors.textSecondary else accent,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(top = 4.dp),
        )
        LinearProgressIndicator(
            progress = { budget.ratio },
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = accent,
            trackColor = colors.tagBackground,
            drawStopIndicator = {},
        )
    }
}

private fun budgetPercentLabel(spent: Long, limit: Long): String {
    if (limit <= 0L || spent <= 0L) return "0%"
    val percent = try {
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = stringResource(R.string.bill_recent_title),
            action = if (headerState.currentId != null) {
                stringResource(R.string.bill_recent_all)
            } else {
                null
            },
            onAction = if (headerState.currentId != null) onViewAll else null,
        )
        LedgerCard {
            when {
                headerState.loading && headerState.currentId == null -> PendingLine(
                    modifier = Modifier.padding(16.dp),
                )
                pending -> PendingLine(modifier = Modifier.padding(16.dp))
                showEmptyAccount || uiState.recent.isEmpty() -> Text(
                    text = stringResource(R.string.bill_recent_week_empty),
                    color = MaterialTheme.appColors.textTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                )
                else -> WeekBillList(
                    bills = uiState.recent,
                    currencySymbol = uiState.currencySymbol,
                )
            }
        }
    }
}

@Composable
private fun WeekBillList(
    bills: List<BillRowUi>,
    currencySymbol: String,
) {
    val days = remember(bills) {
        bills.groupBy { it.time.toLocalDate() }.entries.toList()
    }
    val colors = MaterialTheme.appColors
    Column(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { dayIndex, (day, rows) ->
            Text(
                text = weekDayText(day),
                color = colors.textTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (dayIndex == 0) 14.dp else 16.dp,
                ),
            )
            rows.forEachIndexed { index, bill ->
                if (index > 0) {
                    HorizontalDivider(
                        color = colors.divider,
                        modifier = Modifier.padding(start = 60.dp),
                    )
                }
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
    val title = when (bill.type) {
        BillType.Transfer -> stringResource(R.string.bill_transfer)
        else -> bill.categoryName ?: stringResource(R.string.bill_category_missing)
    }
    val amountColor = when {
        bill.signedAmount < 0L -> colors.expense
        bill.type == BillType.Income -> colors.income
        else -> colors.textPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BillMark(bill = bill)
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
                text = clockText(bill.time),
                color = colors.textTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = currencySymbol + formatMinorAmount(bill.signedAmount),
            color = amountColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun BillMark(bill: BillRowUi) {
    val colors = MaterialTheme.appColors
    val tint = when {
        bill.type == BillType.Transfer -> colors.textSecondary
        bill.categoryColorKey != null -> accountColorValue(bill.categoryColorKey)
        bill.type == BillType.Income -> colors.income
        else -> colors.expense
    }
    val icon = when (bill.type) {
        BillType.Transfer -> Icons.Rounded.SwapHoriz
        else -> categoryIcon(bill.categoryIconKey.orEmpty())
    }
    Box(
        modifier = Modifier
            .size(32.dp)
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
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
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
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = action,
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
        shape = LedgerCardShape,
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.divider),
        shadowElevation = 0.dp,
    ) {
        Column(content = content)
    }
}

/**
 * 按下时整块变淡。背景放在 [modifier] 里，淡化才会连底色一起走。
 */
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
        modifier = modifier
            .size(tileSize)
            .background(tint, shape)
            .then(
                if (outlined) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), shape)
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
internal fun billTimeText(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "zh") "M月d日 HH:mm" else "MMM d, HH:mm"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return time.format(formatter)
}
