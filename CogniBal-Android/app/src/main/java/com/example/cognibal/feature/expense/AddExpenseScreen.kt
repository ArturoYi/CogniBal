package com.example.cognibal.screen.feature.expense

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.expense.model.AddBillIssue
import com.example.cognibal.screen.feature.expense.model.categoryIcon
import com.example.cognibal.screen.feature.expense.state.AccountChoice
import com.example.cognibal.screen.feature.expense.state.AddExpenseUiEvent
import com.example.cognibal.screen.feature.expense.state.AddExpenseUiState
import com.example.cognibal.screen.feature.expense.state.CategoryChoice
import com.example.cognibal.screen.feature.expense.state.CategoryCreateResult
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalCardElevation
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.launch
import com.example.cognibal.screen.feature.expense.model.onCategory
import com.example.cognibal.screen.feature.expense.model.messageRes

/**
 * 记一笔。顶栏切换支出、收入和转账。
 * 支出和收入先点分类，再填金额和备注，记入首页当前选中的账户，页面上不再写账户名。
 * 转账不选分类，改为选择转出和转入账户。
 * 金额和备注都用系统键盘，页面不自动抢焦点。返回直接离开这一页。
 */
@Composable
internal fun AddExpenseRoute(
    typeCode: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExpenseViewModel = viewModel(
        key = AddExpenseViewModel.key(typeCode),
        factory = AddExpenseViewModel.factory(typeCode),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var createError by remember { mutableStateOf<AddBillIssue?>(null) }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.onLeave() }
    }
    LaunchedEffect(viewModel) {
        viewModel.finished.collect { onBack() }
    }
    LaunchedEffect(viewModel) {
        viewModel.categoryCreated.collect { result ->
            when (result) {
                is CategoryCreateResult.Saved -> {
                    showCreate = false
                    createError = null
                }
                is CategoryCreateResult.Failed -> createError = result.issue
            }
        }
    }
    AddExpenseScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        showCreateCategory = showCreate,
        createError = createError,
        onShowCreateCategory = {
            createError = null
            showCreate = true
        },
        onDismissCreateCategory = {
            showCreate = false
            createError = null
        },
        showDate = showDate,
        showTime = showTime,
        onShowDate = { showDate = true },
        onShowTime = { showTime = true },
        onDismissDate = { showDate = false },
        onDismissTime = { showTime = false },
        modifier = modifier,
    )
}

@Composable
internal fun AddExpenseScreen(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isEdit: Boolean = false,
    onDelete: () -> Unit = {},
    showCreateCategory: Boolean = false,
    createError: AddBillIssue? = null,
    onShowCreateCategory: () -> Unit = {},
    onDismissCreateCategory: () -> Unit = {},
    showDate: Boolean = false,
    showTime: Boolean = false,
    onShowDate: () -> Unit = {},
    onShowTime: () -> Unit = {},
    onDismissDate: () -> Unit = {},
    onDismissTime: () -> Unit = {},
) {
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val transfer = uiState.type == BillType.Transfer
    val categorySelected = uiState.categoryId != null
    val showFields = transfer || categorySelected
    var pickingFrom by rememberSaveable { mutableStateOf(false) }
    var pickingTo by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(transfer) {
        if (!transfer) {
            pickingFrom = false
            pickingTo = false
        }
    }
    val horizontalInsets = screenContentWindowInsets().only(WindowInsetsSides.Horizontal)
    RequestPageBackground()
    Column(modifier.fillMaxSize()) {
        TypeBar(
            type = uiState.type,
            enabled = !uiState.saving,
            onBack = onBack,
            onType = { onEvent(AddExpenseUiEvent.TypeSelected(it)) },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(horizontalInsets)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (uiState.ready) Column(
                modifier = Modifier
                    .widthIn(max = FormMaxWidth)
                    .fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (transfer) {
                        TransferAccounts(
                            accounts = uiState.accounts,
                            fromId = uiState.accountId,
                            toId = uiState.toAccountId,
                            enabled = !uiState.saving,
                            onPickFrom = { pickingFrom = true },
                            onPickTo = { pickingTo = true },
                        )
                    } else {
                        CategorySection(
                            categories = uiState.categories,
                            selectedId = uiState.categoryId,
                            enabled = !uiState.saving,
                            onSelect = { onEvent(AddExpenseUiEvent.CategorySelected(it)) },
                            onCreate = onShowCreateCategory,
                        )
                    }
                    if (showFields) {
                        EntryFields(
                            uiState = uiState,
                            isEdit = isEdit,
                            onEvent = onEvent,
                            onDelete = onDelete,
                            onPickDate = onShowDate,
                            onPickTime = onShowTime,
                        )
                    } else {
                        Spacer(Modifier.windowInsetsPadding(navigationBottomInset()))
                    }
                }
                if (showFields) {
                    SaveBar(
                        saving = uiState.saving,
                        includeBottomInset = !imeVisible,
                        onSave = { onEvent(AddExpenseUiEvent.Save) },
                    )
                }
            }
        }
    }
    if (showCreateCategory) {
        CreateCategoryDialog(
            error = createError,
            onConfirm = { name, iconKey, colorHex ->
                onEvent(AddExpenseUiEvent.CreateCategory(name, iconKey, colorHex))
            },
            onDismiss = onDismissCreateCategory,
        )
    }
    if (showDate) {
        BillDateDialog(
            initial = uiState.billTime.toLocalDate(),
            onConfirm = { date ->
                onEvent(AddExpenseUiEvent.DatePicked(date))
                onDismissDate()
            },
            onDismiss = onDismissDate,
        )
    }
    if (showTime) {
        BillTimeSheet(
            hour = uiState.billTime.hour,
            minute = uiState.billTime.minute,
            onConfirm = { hour, minute ->
                onEvent(AddExpenseUiEvent.TimePicked(hour, minute))
                onDismissTime()
            },
            onDismiss = onDismissTime,
        )
    }
    if (transfer && pickingFrom) {
        AccountPickDialog(
            title = stringResource(R.string.bill_entry_from),
            accounts = uiState.accounts,
            selectedId = uiState.accountId,
            onSelect = { accountId ->
                onEvent(AddExpenseUiEvent.AccountSelected(accountId))
                pickingFrom = false
            },
            onDismiss = { pickingFrom = false },
        )
    }
    if (transfer && pickingTo) {
        AccountPickDialog(
            title = stringResource(R.string.bill_entry_to),
            accounts = uiState.accounts.filter { it.id != uiState.accountId },
            selectedId = uiState.toAccountId,
            onSelect = { accountId ->
                onEvent(AddExpenseUiEvent.ToAccountSelected(accountId))
                pickingTo = false
            },
            onDismiss = { pickingTo = false },
        )
    }
    val prompt = uiState.overBudgetPrompt
    if (prompt != null) {
        val colors = MaterialTheme.appColors
        val separator = stringResource(R.string.budget_name_separator)
        val globalName = stringResource(R.string.bill_budget_global)
        val names = buildList {
            if (prompt.includesGlobal) add(globalName)
            addAll(prompt.categoryNames)
        }.joinToString(separator)
        AlertDialog(
            onDismissRequest = { onEvent(AddExpenseUiEvent.DismissOverBudget) },
            title = {
                Text(
                    text = stringResource(R.string.budget_over_title),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.budget_over_message, names),
                    color = colors.textSecondary,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(AddExpenseUiEvent.ConfirmOverBudget) }) {
                    Text(
                        text = stringResource(R.string.budget_over_confirm),
                        color = colors.expense,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AddExpenseUiEvent.DismissOverBudget) }) {
                    Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                }
            },
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun TypeBar(
    type: BillType,
    enabled: Boolean,
    onBack: () -> Unit,
    onType: (BillType) -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                screenContentWindowInsets().only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .interactiveClick(enabled = enabled, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
                tint = colors.textPrimary,
            )
        }
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypeChip(
                label = stringResource(R.string.bill_fab_expense),
                selected = type == BillType.Expense,
                color = colors.primary,
                enabled = enabled,
                onClick = { onType(BillType.Expense) },
            )
            TypeChip(
                label = stringResource(R.string.bill_fab_income),
                selected = type == BillType.Income,
                color = colors.primary,
                enabled = enabled,
                onClick = { onType(BillType.Income) },
            )
            TypeChip(
                label = stringResource(R.string.bill_fab_transfer),
                selected = type == BillType.Transfer,
                color = colors.primary,
                enabled = enabled,
                onClick = { onType(BillType.Transfer) },
            )
        }
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Text(
        text = label,
        color = if (selected) color.onCategory() else colors.textSecondary,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .semantics { this.selected = selected }
            .background(
                if (selected) color else colors.tagBackground,
                RoundedCornerShape(20.dp),
            )
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TransferAccounts(
    accounts: List<AccountChoice>,
    fromId: Long?,
    toId: Long?,
    enabled: Boolean,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    val from = accounts.firstOrNull { it.id == fromId }
    val to = accounts.firstOrNull { it.id == toId }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ClockRow(
            label = stringResource(R.string.bill_entry_from),
            value = from?.name ?: stringResource(R.string.bill_entry_no_account),
            enabled = enabled && accounts.isNotEmpty(),
            onClick = onPickFrom,
        )
        ClockRow(
            label = stringResource(R.string.bill_entry_to),
            value = to?.name ?: stringResource(R.string.bill_entry_error_second_account),
            enabled = enabled && accounts.size > 1,
            onClick = onPickTo,
        )
    }
}

@Composable
private fun CategorySection(
    categories: List<CategoryChoice>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        shadowElevation = CogniBalCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.bill_entry_category),
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (categories.isEmpty()) {
                Text(
                    text = stringResource(R.string.bill_entry_no_category),
                    color = colors.textTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            CategoryGrid(
                categories = categories,
                selectedId = selectedId,
                enabled = enabled,
                onSelect = onSelect,
                onCreate = onCreate,
            )
        }
    }
}

/** 按一行能放下的列数均分宽度，避免固定格子宽度在右侧空出一整列。 */
@Composable
private fun CategoryGrid(
    categories: List<CategoryChoice>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / CategoryTileMinWidth).toInt().coerceAtLeast(1)
        val count = categories.size + 1
        val rows = (count + columns - 1) / columns
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (column in 0 until columns) {
                        val index = row * columns + column
                        when {
                            index < categories.size -> {
                                val category = categories[index]
                                CategoryTile(
                                    category = category,
                                    selected = category.id == selectedId,
                                    enabled = enabled,
                                    onClick = { onSelect(category.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            index == categories.size ->
                                NewCategoryTile(
                                    enabled = enabled,
                                    onClick = onCreate,
                                    modifier = Modifier.weight(1f),
                                )
                            else -> Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private val CategoryTileMinWidth = 72.dp

@Composable
private fun CategoryTile(
    category: CategoryChoice,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(category.colorKey)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics(mergeDescendants = true) { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (selected) tint else colors.tagBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(category.iconKey),
                contentDescription = null,
                tint = if (selected) tint.onCategory() else tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = category.name,
            color = if (selected) tint else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun NewCategoryTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.tagBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.bill_entry_new_category),
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(R.string.bill_entry_new_category),
            color = colors.primary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun EntryFields(
    uiState: AddExpenseUiState,
    isEdit: Boolean,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onDelete: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val amountBringIntoView = remember { BringIntoViewRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ClockRow(
            label = stringResource(R.string.bill_entry_time),
            value = localizedBillTime(uiState.billTime),
            enabled = !uiState.saving,
            onClick = {
                focusManager.clearFocus(force = true)
                keyboard?.hide()
                onPickTime()
            },
        )
        ClockRow(
            label = stringResource(R.string.bill_entry_date),
            value = localizedBillDate(uiState.billTime),
            enabled = !uiState.saving,
            onClick = {
                focusManager.clearFocus()
                onPickDate()
            },
        )
        AmountField(
            amountText = uiState.amountText,
            symbol = uiState.currencySymbol,
            type = uiState.type,
            enabled = !uiState.saving,
            bringIntoView = amountBringIntoView,
            onChange = { onEvent(AddExpenseUiEvent.AmountChanged(it)) },
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        )
        RemarkField(
            value = uiState.remark,
            enabled = !uiState.saving,
            onChange = { onEvent(AddExpenseUiEvent.RemarkChanged(it)) },
            onDone = { focusManager.clearFocus() },
        )
        val issue = uiState.issue
        if (issue != null) {
            Text(
                text = stringResource(issue.messageRes()),
                color = MaterialTheme.appColors.expense,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        if (isEdit) {
            TextButton(
                onClick = onDelete,
                enabled = !uiState.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.bill_entry_delete),
                    color = MaterialTheme.appColors.expense,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AmountField(
    amountText: String,
    symbol: String,
    type: BillType,
    enabled: Boolean,
    bringIntoView: BringIntoViewRequester,
    onChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val amountColor = type.amountColor()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) {
        if (focused) bringIntoView.bringIntoView()
    }
    OutlinedTextField(
        value = amountText,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(CogniBalCardElevation, RoundedCornerShape(16.dp))
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) scope.launch { bringIntoView.bringIntoView() }
            },
        enabled = enabled,
        textStyle = TextStyle(
            color = amountColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        ),
        label = { Text(stringResource(R.string.bill_entry_amount)) },
        placeholder = { Text(stringResource(R.string.bill_entry_amount_hint)) },
        prefix = {
            Text(
                text = symbol,
                color = amountColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        shape = RoundedCornerShape(16.dp),
        colors = entryFieldColors(textColor = amountColor, prefixColor = amountColor),
    )
}

@Composable
private fun RemarkField(
    value: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) {
        if (focused) bringIntoView.bringIntoView()
    }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(CogniBalCardElevation, RoundedCornerShape(16.dp))
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) scope.launch { bringIntoView.bringIntoView() }
            },
        enabled = enabled,
        label = { Text(stringResource(R.string.account_field_remark)) },
        placeholder = { Text(stringResource(R.string.account_field_remark_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(16.dp),
        colors = entryFieldColors(),
    )
}

@Composable
private fun ClockRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(CogniBalCardElevation, RoundedCornerShape(16.dp))
            .background(colors.cardBackground, RoundedCornerShape(16.dp))
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = colors.textSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun localizedBillDate(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    return time.format(formatter)
}

@Composable
private fun localizedBillTime(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val pattern = remember(locale, use24Hour) {
        DateFormat.getBestDateTimePattern(locale, if (use24Hour) "Hm" else "hm")
    }
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return time.format(formatter)
}

@Composable
private fun entryFieldColors(
    textColor: Color = MaterialTheme.appColors.textPrimary,
    prefixColor: Color = textColor,
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    disabledTextColor = MaterialTheme.appColors.textSecondary,
    focusedContainerColor = MaterialTheme.appColors.cardBackground,
    unfocusedContainerColor = MaterialTheme.appColors.cardBackground,
    disabledContainerColor = MaterialTheme.appColors.cardBackground,
    cursorColor = MaterialTheme.appColors.primary,
    focusedBorderColor = MaterialTheme.appColors.primary.copy(alpha = 0.45f),
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.appColors.primary,
    unfocusedLabelColor = MaterialTheme.appColors.textSecondary,
    focusedPlaceholderColor = MaterialTheme.appColors.textTertiary,
    unfocusedPlaceholderColor = MaterialTheme.appColors.textTertiary,
    focusedPrefixColor = prefixColor,
    unfocusedPrefixColor = prefixColor,
)

@Composable
private fun SaveBar(
    saving: Boolean,
    includeBottomInset: Boolean,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val bottomInset = navigationBottomInset()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (includeBottomInset) {
                    Modifier
                        .windowInsetsPadding(bottomInset)
                        .consumeWindowInsets(bottomInset)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.primary.copy(alpha = 0.4f),
                disabledContentColor = colors.onPrimary.copy(alpha = 0.7f),
            ),
        ) {
            Text(
                text = stringResource(R.string.bill_entry_save),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun navigationBottomInset() = screenContentWindowInsets()
    .exclude(WindowInsets.ime)
    .only(WindowInsetsSides.Bottom)

@Composable
private fun BillType.amountColor(): Color {
    val colors = MaterialTheme.appColors
    return when (this) {
        BillType.Expense -> colors.textPrimary
        BillType.Income -> colors.textPrimary
        BillType.Transfer -> colors.textPrimary
    }
}

private val FormMaxWidth = 640.dp

@FormFactorPreviews
@Composable
private fun AddExpenseTagsPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AddExpenseScreen(
                uiState = previewState(categoryId = null, amountText = ""),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun AddExpenseTransferPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AddExpenseScreen(
                uiState = previewState(categoryId = null, amountText = "80").copy(
                    type = BillType.Transfer,
                    accounts = listOf(
                        AccountChoice(1, "现金", "cash", "teal"),
                        AccountChoice(2, "银行卡", "account_balance", "blue"),
                    ),
                    accountId = 1L,
                    toAccountId = 2L,
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun AddExpenseAmountPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AddExpenseScreen(
                uiState = previewState(categoryId = 1L, amountText = "128.50"),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

private fun previewState(categoryId: Long?, amountText: String): AddExpenseUiState {
    val categories = listOf(
        CategoryChoice(1, "餐饮", "restaurant", "#FFF03E3E"),
        CategoryChoice(2, "交通", "directions_bus", "#FF0CA678"),
        CategoryChoice(3, "购物", "shopping_bag", "#FFFD7E14"),
        CategoryChoice(4, "居住", "home", "#FF228BE6"),
    )
    return AddExpenseUiState(
        ready = true,
        type = BillType.Expense,
        amountText = amountText,
        categories = categories,
        categoryId = categoryId,
        accounts = listOf(AccountChoice(1, "现金", "cash", "teal")),
        accountId = 1L,
        billTime = LocalDateTime.of(2026, 10, 18, 9, 30),
        currencySymbol = "¥",
    )
}
