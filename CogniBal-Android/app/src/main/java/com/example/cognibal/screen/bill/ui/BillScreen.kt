package com.example.cognibal.screen.bill.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.bill.model.accountColorValue
import com.example.cognibal.screen.bill.model.accountIconImage
import com.example.cognibal.screen.bill.tool.formatMinorAmount
import com.example.cognibal.screen.bill.viewmodel.AccountItemUi
import com.example.cognibal.screen.bill.viewmodel.BillHomeUiEvent
import com.example.cognibal.screen.bill.viewmodel.BillHomeUiState
import com.example.cognibal.screen.bill.viewmodel.BillHomeViewModel
import com.example.cognibal.screen.bill.viewmodel.BillRowUi
import com.example.cognibal.screen.bill.viewmodel.BudgetProgressUi
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDateTime

/**
 * 账单页入口。账户只出现在顶栏左侧：点开可以切换，也可以新建或编辑。
 */
@Composable
internal fun BillRoute(
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onViewAll: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillHomeViewModel = viewModel(factory = BillHomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BillScreen(
        uiState = uiState,
        onSelect = { viewModel.onEvent(BillHomeUiEvent.Select(it)) },
        onCreateAccount = onCreateAccount,
        onEditAccount = onEditAccount,
        onViewAll = {
            val accountId = uiState.currentId ?: return@BillScreen
            onViewAll(accountId)
        },
        modifier = modifier,
    )
}

/**
 * 记账首页。顶栏只有左侧账户，贴在状态栏下面。页面背景仍铺到屏幕边缘。
 */
@Composable
internal fun BillScreen(
    uiState: BillHomeUiState,
    onSelect: (Long) -> Unit,
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RequestPageBackground()
    Column(modifier = modifier.fillMaxSize()) {
        AccountAppBar(
            uiState = uiState,
            onSelect = onSelect,
            onCreateAccount = onCreateAccount,
            onEditAccount = onEditAccount,
            modifier = Modifier.windowInsetsPadding(
                screenContentWindowInsets().only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            BillHomeList(
                uiState = uiState,
                onViewAll = onViewAll,
            )
        }
    }
}

@Composable
private fun AccountAppBar(
    uiState: BillHomeUiState,
    onSelect: (Long) -> Unit,
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = uiState.accounts.firstOrNull { it.id == uiState.currentId }
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
    ) {
        if (current == null) {
            if (!uiState.loading) {
                AccountMenuAction(
                    label = stringResource(R.string.bill_account_create),
                    onClick = onCreateAccount,
                )
            }
        } else {
            AccountSwitcher(
                account = current,
                expanded = menuOpen,
                onExpand = { menuOpen = true },
                onDismiss = { menuOpen = false },
                accounts = uiState.accounts,
                currencySymbol = uiState.currencySymbol,
                onSelect = { accountId ->
                    menuOpen = false
                    onSelect(accountId)
                },
                onCreate = {
                    menuOpen = false
                    onCreateAccount()
                },
                onEdit = {
                    menuOpen = false
                    onEditAccount(current.id)
                },
            )
        }
    }
}

@Composable
private fun AccountSwitcher(
    account: AccountItemUi,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    accounts: List<AccountItemUi>,
    currencySymbol: String,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(account.colorKey)
    val description = stringResource(R.string.account_switch_cd, account.name)
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .widthIn(max = 240.dp)
                .background(colors.tagBackground, RoundedCornerShape(16.dp))
                .semantics { contentDescription = description }
                .interactiveClick(onClick = onExpand)
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        tint.copy(alpha = if (colors.isDark) 0.22f else 0.16f),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = accountIconImage(account.iconKey),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = account.name,
                color = if (account.enabled) colors.textPrimary else colors.textTertiary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f, fill = false),
            )
            if (!account.enabled) {
                Text(
                    text = stringResource(R.string.account_status_disabled),
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(20.dp),
            )
        }
        AccountMenu(
            expanded = expanded,
            onDismiss = onDismiss,
            accounts = accounts,
            currencySymbol = currencySymbol,
            onSelect = onSelect,
            onCreate = onCreate,
            onEdit = onEdit,
        )
    }
}

@Composable
private fun AccountMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    accounts: List<AccountItemUi>,
    currencySymbol: String,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.cardBackground,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        accounts.forEach { account ->
            val tint = accountColorValue(account.colorKey)
            val amountColor = when {
                account.balance < 0 -> colors.expense
                else -> colors.textTertiary
            }
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = account.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (account.selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = if (account.enabled) {
                                currencySymbol + formatMinorAmount(account.balance)
                            } else {
                                stringResource(
                                    R.string.account_disabled_balance,
                                    currencySymbol + formatMinorAmount(account.balance),
                                )
                            },
                            color = amountColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = accountIconImage(account.iconKey),
                        contentDescription = null,
                        tint = tint,
                    )
                },
                trailingIcon = if (account.selected) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.primary,
                        )
                    }
                } else {
                    null
                },
                onClick = { onSelect(account.id) },
                colors = MenuDefaults.itemColors(
                    textColor = if (account.enabled) colors.textPrimary else colors.textTertiary,
                    leadingIconColor = tint,
                    trailingIconColor = colors.primary,
                ),
            )
        }
        HorizontalDivider(color = colors.divider)
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bill_account_create)) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
            },
            onClick = onCreate,
            colors = MenuDefaults.itemColors(
                textColor = colors.textPrimary,
                leadingIconColor = colors.textPrimary,
            ),
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.account_edit_current)) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
            },
            onClick = onEdit,
            colors = MenuDefaults.itemColors(
                textColor = colors.textPrimary,
                leadingIconColor = colors.textPrimary,
            ),
        )
    }
}

@Composable
private fun AccountMenuAction(
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(colors.tagBackground, RoundedCornerShape(16.dp))
            .interactiveClick(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@FormFactorPreviews
@Composable
private fun BillScreenPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            BillScreen(
                uiState = previewAccounts,
                onSelect = {},
                onCreateAccount = {},
                onEditAccount = {},
                onViewAll = {},
            )
        }
    }
}

private val previewAccounts = BillHomeUiState(
    loading = false,
    currencySymbol = "¥",
    currentId = 1L,
    totalAssets = 1_285_000L,
    monthIncome = 860_000L,
    monthExpense = 246_000L,
    monthBalance = 614_000L,
    budgets = listOf(
        BudgetProgressUi(
            id = 1L,
            categoryName = null,
            missingCategory = false,
            spent = 246_000L,
            limit = 500_000L,
            ratio = 0.49f,
            over = false,
        ),
    ),
    recent = listOf(
        BillRowUi(
            id = 1L,
            categoryName = "餐饮",
            type = BillType.Expense,
            time = LocalDateTime.of(2026, 10, 17, 12, 30),
            signedAmount = -3_500L,
        ),
        BillRowUi(
            id = 2L,
            categoryName = "工资",
            type = BillType.Income,
            time = LocalDateTime.of(2026, 10, 15, 9, 0),
            signedAmount = 860_000L,
        ),
    ),
    accounts = listOf(
        AccountItemUi(
            id = 1L,
            name = "现金",
            type = AccountType.Cash,
            iconKey = "wallet",
            colorKey = "teal",
            balance = 320_000L,
            enabled = true,
            selected = true,
        ),
        AccountItemUi(
            id = 2L,
            name = "工资卡",
            type = AccountType.DebitCard,
            iconKey = "card",
            colorKey = "blue",
            balance = 1_085_000L,
            enabled = true,
            selected = false,
        ),
        AccountItemUi(
            id = 3L,
            name = "信用卡",
            type = AccountType.CreditCard,
            iconKey = "card",
            colorKey = "clay",
            balance = -120_000L,
            enabled = true,
            selected = false,
        ),
    ),
)
