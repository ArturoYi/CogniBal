package com.cyr.cognobal.screen.feature.bill

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.AccountType
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.data.repository.BudgetLevel
import com.cyr.cognobal.screen.feature.bill.state.AccountBarState
import com.cyr.cognobal.screen.feature.bill.state.AccountItemUi
import com.cyr.cognobal.screen.feature.bill.state.BillHomeUiEvent
import com.cyr.cognobal.screen.feature.bill.state.BillHomeUiState
import com.cyr.cognobal.screen.feature.bill.state.BillRowUi
import com.cyr.cognobal.screen.feature.bill.state.BudgetProgressUi
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.expense.AddExpenseViewModel
import com.cyr.cognobal.screen.feature.splash.LedgerStartup
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.background.RequestPageBackground
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.privacy.LocalAmountVisibility
import com.cyr.cognobal.ui.privacy.maskIfHidden
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BILL_HOME_REVEAL_TIMEOUT_MILLIS = 2_000L

@Composable
internal fun BillRoute(
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onViewAll: (Long) -> Unit,
    onOpenBill: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: BillHomeViewModel = viewModel(factory = BillHomeViewModel.Factory),
) {
    viewModel<AddExpenseViewModel>(
        key = AddExpenseViewModel.key(BillType.Expense.code),
        factory = AddExpenseViewModel.factory(BillType.Expense.code),
    )
    val accountBar by homeViewModel.accountBar.collectAsStateWithLifecycle()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    BillScreen(
        uiState = uiState,
        headerState = headerWhileHomeLoads(uiState, accountBar),
        onSelect = { homeViewModel.onEvent(BillHomeUiEvent.Select(it)) },
        onCreateAccount = onCreateAccount,
        onEditAccount = onEditAccount,
        onViewAll = {
            val accountId = accountBar.currentId ?: uiState.currentId ?: return@BillScreen
            onViewAll(accountId)
        },
        onOpenBill = onOpenBill,
        onHideBudget = { homeViewModel.onEvent(BillHomeUiEvent.HideBudget) },
        onOpenBudget = onOpenBudget,
        onOpenCalendar = onOpenCalendar,
        modifier = modifier,
    )
}

internal data class BillHomeWarmup(
    val contentReady: Boolean,
) {
    companion object {
        val Shown = BillHomeWarmup(contentReady = true)
    }
}

@Composable
internal fun rememberBillHomeWarmup(): BillHomeWarmup {
    val context = LocalContext.current
    var accountsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        LedgerStartup.ensureDefaultAccount(context)
        accountsReady = true
    }
    if (!accountsReady) return BillHomeWarmup(contentReady = false)
    val homeViewModel: BillHomeViewModel = viewModel(factory = BillHomeViewModel.Factory)
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(BILL_HOME_REVEAL_TIMEOUT_MILLIS)
        timedOut = true
    }
    return BillHomeWarmup(contentReady = !uiState.loading || timedOut)
}

private fun headerWhileHomeLoads(
    home: BillHomeUiState,
    bar: AccountBarState,
): BillHomeUiState {
    if (bar.loading) return home
    return home.copy(
        loading = false,
        currencySymbol = bar.currencySymbol,
        accounts = bar.accounts,
        currentId = bar.currentId,
    )
}

@Composable
internal fun BillScreen(
    uiState: BillHomeUiState,
    onSelect: (Long) -> Unit,
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onViewAll: () -> Unit,
    onOpenBill: (Long) -> Unit = {},
    onOpenBudget: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    onHideBudget: () -> Unit = {},
    modifier: Modifier = Modifier,
    headerState: BillHomeUiState = uiState,
) {
    RequestPageBackground()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var budgetSheetOpen by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        BillHomeList(
            uiState = uiState,
            headerState = headerState,
            onOpenAccounts = { sheetOpen = true },
            onCreateAccount = onCreateAccount,
            onViewAll = onViewAll,
            onOpenBill = onOpenBill,
            onOpenBudget = onOpenBudget,
            onManageBudget = { budgetSheetOpen = true },
            onOpenCalendar = onOpenCalendar,
        )
    }
    val current = headerState.accounts.firstOrNull { it.id == headerState.currentId }
    if (sheetOpen && current != null) {
        AccountSheet(
            accounts = headerState.accounts,
            currencySymbol = headerState.currencySymbol,
            onDismiss = { sheetOpen = false },
            onSelect = onSelect,
            onCreate = onCreateAccount,
            onEdit = { onEditAccount(current.id) },
        )
    }
    if (budgetSheetOpen) {
        BudgetHomeSheet(
            onDismiss = { budgetSheetOpen = false },
            onHide = onHideBudget,
            onOpenSettings = onOpenBudget,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(
    accounts: List<AccountItemUi>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val hidden = LocalAmountVisibility.current.hidden
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(after: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            after()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_account_switch),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    key(account.id) {
                        AccountSheetRow(
                            account = account,
                            currencySymbol = currencySymbol,
                            hidden = hidden,
                            onClick = {
                                close {
                                    if (!account.selected) onSelect(account.id)
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SheetPillAction(
                    icon = Icons.Outlined.Add,
                    label = stringResource(R.string.bill_account_create),
                    primary = true,
                    onClick = { close(onCreate) },
                    modifier = Modifier.weight(1f),
                )
                SheetPillAction(
                    icon = Icons.Outlined.Edit,
                    label = stringResource(R.string.account_edit_current),
                    primary = false,
                    onClick = { close(onEdit) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AccountSheetRow(
    account: AccountItemUi,
    currencySymbol: String,
    hidden: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val amountColor = if (account.enabled) colors.textPrimary else colors.textTertiary
    val container = if (account.selected) colors.primaryContainer else colors.background
    PressFade(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .semantics { selected = account.selected },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountMark(
                iconKey = account.iconKey,
                colorKey = account.colorKey,
                emphasized = account.selected,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = account.name,
                    color = if (account.enabled) colors.textPrimary else colors.textTertiary,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (account.selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!account.enabled) {
                    Text(
                        text = stringResource(R.string.account_status_disabled),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = maskIfHidden(formatMoney(currencySymbol, account.balance), hidden, currencySymbol),
                color = amountColor,
                style = MaterialTheme.typography.titleMedium.merge(
                    TextStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                ),
                maxLines = 1,
            )
            if (account.selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetPillAction(
    icon: ImageVector,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val bg = if (primary) colors.inkStrong else colors.background
    val fg = if (primary) colors.onInkStrong else colors.textPrimary
    PressFade(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    PressFade(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(MaterialTheme.shapes.large)
            .background(colors.background),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
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
            Text(
                text = label,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetHomeSheet(
    onDismiss: () -> Unit,
    onHide: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val context = LocalContext.current
    val hiddenMessage = stringResource(R.string.budget_home_hidden)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(after: () -> Unit) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            after()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_budget_title),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            SheetAction(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.budget_home_settings),
                onClick = { close(onOpenSettings) },
            )
            SheetAction(
                icon = Icons.Outlined.VisibilityOff,
                label = stringResource(R.string.budget_home_hide),
                onClick = {
                    close {
                        onHide()
                        Toast.makeText(context, hiddenMessage, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun BillScreenPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            BillScreen(
                uiState = previewHome,
                onSelect = {},
                onCreateAccount = {},
                onEditAccount = {},
                onViewAll = {},
                onOpenBudget = {},
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF101216)
@Composable
private fun BillScreenDarkPreview() {
    CogniBalTheme(darkTheme = true) {
        PageBackgroundHost {
            BillScreen(
                uiState = previewHome,
                onSelect = {},
                onCreateAccount = {},
                onEditAccount = {},
                onViewAll = {},
                onOpenBudget = {},
            )
        }
    }
}

@Preview(name = "Mint", showBackground = true)
@Composable
private fun BillScreenMintPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.Cream, darkTheme = false) {
        PageBackgroundHost {
            BillScreen(
                uiState = previewHome,
                onSelect = {},
                onCreateAccount = {},
                onEditAccount = {},
                onViewAll = {},
                onOpenBudget = {},
            )
        }
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun BillScreenEmptyPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            BillScreen(
                uiState = BillHomeUiState(loading = false),
                onSelect = {},
                onCreateAccount = {},
                onEditAccount = {},
                onViewAll = {},
                onOpenBudget = {},
            )
        }
    }
}

private val previewHome = BillHomeUiState(
    loading = false,
    currencySymbol = "¥",
    currentId = 1L,
    totalAssets = 1_285_000L,
    monthIncome = 882_000L,
    monthExpense = 678_230L,
    monthBalance = 203_770L,
    budgets = listOf(
        BudgetProgressUi(
            id = 1L,
            categoryName = null,
            missingCategory = false,
            spent = 678_230L,
            limit = 882_000L,
            ratio = 0.77f,
            level = BudgetLevel.Normal,
        ),
    ),
    recent = listOf(
        BillRowUi(
            id = 1L,
            categoryName = "咖啡",
            type = BillType.Expense,
            time = LocalDate.now().atTime(8, 30),
            signedAmount = -2_800L,
            categoryIconKey = "coffee",
            categoryColorKey = "#FF0CA678",
        ),
        BillRowUi(
            id = 2L,
            categoryName = "购物",
            type = BillType.Expense,
            time = LocalDate.now().minusDays(1).atTime(19, 16),
            signedAmount = -15_680L,
            categoryIconKey = "shopping_bag",
            categoryColorKey = "#FFFD7E14",
            remark = "超市采购",
        ),
        BillRowUi(
            id = 3L,
            categoryName = "交通",
            type = BillType.Expense,
            time = LocalDate.now().minusDays(1).atTime(8, 15),
            signedAmount = -600L,
            categoryIconKey = "directions_bus",
            categoryColorKey = "#FF228BE6",
        ),
    ),
    accounts = listOf(
        AccountItemUi(
            id = 1L,
            name = "储蓄卡",
            type = AccountType.DebitCard,
            iconKey = "bank",
            colorKey = "blue",
            balance = 1_085_000L,
            enabled = true,
            selected = true,
        ),
        AccountItemUi(
            id = 2L,
            name = "现金",
            type = AccountType.Cash,
            iconKey = "wallet",
            colorKey = "teal",
            balance = 320_000L,
            enabled = true,
            selected = false,
        ),
    ),
)
