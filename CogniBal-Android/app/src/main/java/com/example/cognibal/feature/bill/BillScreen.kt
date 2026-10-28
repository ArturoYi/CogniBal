package com.example.cognibal.screen.feature.bill

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.screen.feature.bill.model.labelRes
import com.example.cognibal.screen.feature.bill.state.AccountBarState
import com.example.cognibal.screen.feature.bill.state.AccountItemUi
import com.example.cognibal.screen.feature.bill.state.BillHomeUiEvent
import com.example.cognibal.screen.feature.bill.state.BillHomeUiState
import com.example.cognibal.screen.feature.bill.state.BillRowUi
import com.example.cognibal.screen.feature.bill.state.BudgetProgressUi
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.expense.AddExpenseViewModel
import com.example.cognibal.screen.feature.splash.LedgerStartup
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 默认账户准备好之后，首页查询最多再等这么久。超时就揭开，避免开屏停住。 */
private const val BILL_HOME_REVEAL_TIMEOUT_MILLIS = 2_000L

/**
 * 账单页入口。当前账户在顶部卡片上，点右上角可以切换、新建或编辑。
 */
@Composable
internal fun BillRoute(
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onViewAll: (Long) -> Unit,
    onOpenBudget: () -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: BillHomeViewModel = viewModel(factory = BillHomeViewModel.Factory),
) {
    // 和记一笔页共用同一个 ViewModel。首页先把它订上，打开记一笔时分类和账本已经在状态里。
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
        onHideBudget = { homeViewModel.onEvent(BillHomeUiEvent.HideBudget) },
        onOpenBudget = onOpenBudget,
        modifier = modifier,
    )
}

/**
 * 开屏盖住期间，首页可以画到什么程度。
 *
 * [composeHome] 为真才把首页放进组合。默认账户还没写完就订阅，会先读到空库。
 * [contentReady] 为真表示总资产、本月收支、预算和最近 7 天都已进状态，可以揭开。
 */
internal data class BillHomeWarmup(
    val composeHome: Boolean,
    val contentReady: Boolean,
) {
    companion object {
        /** 不走开屏时直接显示首页，不再等预热。 */
        val Shown = BillHomeWarmup(composeHome = true, contentReady = true)
    }
}

/**
 * 开屏还盖着的时候，把账单首页整份数据准备好。
 *
 * 先等默认账户写完再订阅，避免读到插入前的空库，揭开后又刷成真正的账户和金额。
 * 查询一直不回来时到点也把 [BillHomeWarmup.contentReady] 设为真，开屏不能一直盖着。
 */
@Composable
internal fun rememberBillHomeWarmup(): BillHomeWarmup {
    val context = LocalContext.current
    var accountsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        LedgerStartup.ensureDefaultAccount(context)
        accountsReady = true
    }
    if (!accountsReady) return BillHomeWarmup(composeHome = false, contentReady = false)
    val homeViewModel: BillHomeViewModel = viewModel(factory = BillHomeViewModel.Factory)
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(BILL_HOME_REVEAL_TIMEOUT_MILLIS)
        timedOut = true
    }
    return BillHomeWarmup(
        composeHome = true,
        contentReady = !uiState.loading || timedOut,
    )
}

/**
 * 卡片上的账户跟账户条走，切换时名字立刻换。
 * 本月金额仍用正文。两边账户还不一致时，卡片上先显示破折号。
 */
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

/**
 * 记账首页。账户卡片、预算、最近 7 天同一列往下排。
 * 页面背景铺到屏幕边缘，列表自己躲开系统栏。
 */
@Composable
internal fun BillScreen(
    uiState: BillHomeUiState,
    onSelect: (Long) -> Unit,
    onCreateAccount: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onViewAll: () -> Unit,
    onOpenBudget: () -> Unit,
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
            onManageBudget = { budgetSheetOpen = true },
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

/**
 * 切换、新建、编辑放在同一层。点完先收起再跳，避免弹层盖住下一页。
 */
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
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_account_switch),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.bill_account_switch_hint),
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            )
            accounts.forEach { account ->
                key(account.id) {
                    AccountSheetRow(
                        account = account,
                        currencySymbol = currencySymbol,
                        onClick = {
                            close {
                                if (!account.selected) onSelect(account.id)
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SheetAction(
                icon = Icons.Outlined.Add,
                label = stringResource(R.string.bill_account_create),
                onClick = { close(onCreate) },
            )
            SheetAction(
                icon = Icons.Outlined.Edit,
                label = stringResource(R.string.account_edit_current),
                onClick = { close(onEdit) },
            )
        }
    }
}

@Composable
private fun AccountSheetRow(
    account: AccountItemUi,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val typeName = stringResource(account.type.labelRes())
    val detail = if (account.enabled) {
        typeName
    } else {
        stringResource(R.string.account_disabled_balance, typeName)
    }
    val amountColor = when {
        !account.enabled -> colors.textTertiary
        account.balance < 0L -> colors.textPrimary
        else -> colors.textPrimary
    }
    val selectedColor = if (account.selected) {
        colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.08f)
    } else {
        Color.Transparent
    }
    PressFade(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(selectedColor)
            .semantics { selected = account.selected },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountMark(
                iconKey = account.iconKey,
                colorKey = account.colorKey,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = account.name,
                    color = if (account.enabled) colors.textPrimary else colors.textTertiary,
                    fontSize = 15.sp,
                    fontWeight = if (account.selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = currencySymbol + formatMinorAmount(account.balance),
                color = amountColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                style = TextStyle(fontFeatureSettings = "tnum"),
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
            .heightIn(min = 56.dp),
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
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.tagBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/**
 * 首页预算的入口。先调整预算，再选隐藏。不直接打开预算页。
 */
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
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_budget_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
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

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF212529)
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
            level = BudgetLevel.Normal,
        ),
        BudgetProgressUi(
            id = 2L,
            categoryName = "餐饮",
            missingCategory = false,
            spent = 180_000L,
            limit = 200_000L,
            ratio = 0.9f,
            level = BudgetLevel.Near,
        ),
    ),
    recent = listOf(
        BillRowUi(
            id = 1L,
            categoryName = "餐饮",
            type = BillType.Expense,
            time = LocalDate.now().atTime(12, 30),
            signedAmount = -3_500L,
            categoryIconKey = "restaurant",
            categoryColorKey = "#FFF03E3E",
        ),
        BillRowUi(
            id = 2L,
            categoryName = "交通",
            type = BillType.Expense,
            time = LocalDate.now().atTime(8, 15),
            signedAmount = -1_200L,
            categoryIconKey = "directions_bus",
            categoryColorKey = "#FF0CA678",
        ),
        BillRowUi(
            id = 3L,
            categoryName = "工资",
            type = BillType.Income,
            time = LocalDate.now().minusDays(1).atTime(9, 0),
            signedAmount = 860_000L,
            categoryIconKey = "payments",
            categoryColorKey = "#FF0CA678",
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
