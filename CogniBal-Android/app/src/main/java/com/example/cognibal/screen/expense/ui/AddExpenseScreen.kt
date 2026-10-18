package com.example.cognibal.screen.expense.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BillType
import com.example.cognibal.screen.bill.model.accountColorValue
import com.example.cognibal.screen.bill.model.accountIconImage
import com.example.cognibal.screen.expense.model.AddBillIssue
import com.example.cognibal.screen.expense.model.categoryIcon
import com.example.cognibal.screen.expense.model.messageRes
import com.example.cognibal.screen.expense.model.onCategory
import com.example.cognibal.screen.expense.tool.AmountKey
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.imeFit
import com.example.cognibal.ui.insets.screenContentWindowInsets
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.screen.expense.viewmodel.AccountChoice
import com.example.cognibal.screen.expense.viewmodel.AddExpenseUiEvent
import com.example.cognibal.screen.expense.viewmodel.AddExpenseUiState
import com.example.cognibal.screen.expense.viewmodel.AddExpenseViewModel
import com.example.cognibal.screen.expense.viewmodel.CategoryChoice
import com.example.cognibal.screen.expense.viewmodel.CategoryCreateResult
import java.time.LocalDateTime
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 记一笔。顶栏是返回和标题，保存贴在页面底部。
 * 金额用页面自己的数字键盘，打开时就停在金额上。点备注时收起数字键盘，改用系统输入法。
 * 系统键盘还没退完之前，数字键盘不会叠上来。窄屏键盘贴在保存按钮下面；宽屏放到右侧。
 * 新建不显示删除。
 */
@Composable
internal fun AddExpenseRoute(
    typeCode: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExpenseViewModel = viewModel(
        key = "add-expense-$typeCode",
        factory = AddExpenseViewModel.factory(typeCode),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
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
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var input by rememberSaveable { mutableStateOf(EntryInput.Amount) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var accountTarget by rememberSaveable { mutableStateOf<AccountTarget?>(null) }
    var typeMenu by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val handingOffToIme = input == EntryInput.Remark
    val showKeypad = input == EntryInput.Amount && !imeVisible && !showCreateCategory
    val snapHideKeypad = handingOffToIme || showCreateCategory
    RequestPageBackground()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= TwoPaneMinWidth
        Column(Modifier.fillMaxSize()) {
            EntryTopBar(
                type = uiState.type,
                saving = uiState.saving,
                typeMenu = typeMenu,
                onBack = onBack,
                onTypeMenu = { typeMenu = true },
                onDismissTypeMenu = { typeMenu = false },
                onType = { type ->
                    typeMenu = false
                    onEvent(AddExpenseUiEvent.TypeSelected(type))
                },
            )
            if (uiState.ready) {
                EntryBody(
                    uiState = uiState,
                    wide = wide,
                    showKeypad = showKeypad,
                    snapHideKeypad = snapHideKeypad,
                    imeVisible = imeVisible,
                    amountActive = input == EntryInput.Amount,
                    isEdit = isEdit,
                    onAmount = {
                        input = EntryInput.Amount
                        focusManager.clearFocus()
                    },
                    onRemarkFocus = { input = EntryInput.Remark },
                    onRemarkDone = {
                        input = EntryInput.Idle
                        focusManager.clearFocus()
                    },
                    onHideKeypad = {
                        input = EntryInput.Idle
                        focusManager.clearFocus()
                    },
                    onSave = { onEvent(AddExpenseUiEvent.Save) },
                    onPickDate = { showDate = true },
                    onPickTime = { showTime = true },
                    onPickAccount = { accountTarget = it },
                    onEvent = onEvent,
                    onCreateCategory = {
                        input = EntryInput.Amount
                        focusManager.clearFocus()
                        onShowCreateCategory()
                    },
                    onDelete = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .windowInsetsPadding(
                            screenContentWindowInsets().only(WindowInsetsSides.Horizontal),
                        ),
                )
            }
        }
    }
    if (showDate && uiState.ready) {
        BillDateDialog(
            initial = uiState.billTime.toLocalDate(),
            onConfirm = { date ->
                showDate = false
                onEvent(AddExpenseUiEvent.DatePicked(date))
            },
            onDismiss = { showDate = false },
        )
    }
    if (showTime && uiState.ready) {
        BillTimeDialog(
            hour = uiState.billTime.hour,
            minute = uiState.billTime.minute,
            onConfirm = { hour, minute ->
                showTime = false
                onEvent(AddExpenseUiEvent.TimePicked(hour, minute))
            },
            onDismiss = { showTime = false },
        )
    }
    val picking = accountTarget
    if (picking != null && uiState.ready) {
        val selected = if (picking == AccountTarget.To) uiState.toAccountId else uiState.accountId
        val accounts = if (picking == AccountTarget.To) {
            uiState.accounts.filter { it.id != uiState.accountId }
        } else {
            uiState.accounts
        }
        AccountPickDialog(
            title = stringResource(
                when (picking) {
                    AccountTarget.Single -> R.string.bill_entry_account
                    AccountTarget.From -> R.string.bill_entry_from
                    AccountTarget.To -> R.string.bill_entry_to
                },
            ),
            accounts = accounts,
            selectedId = selected,
            onSelect = { id ->
                accountTarget = null
                val event = if (picking == AccountTarget.To) {
                    AddExpenseUiEvent.ToAccountSelected(id)
                } else {
                    AddExpenseUiEvent.AccountSelected(id)
                }
                onEvent(event)
            },
            onDismiss = { accountTarget = null },
        )
    }
    if (showCreateCategory) {
        CreateCategoryDialog(
            error = createError,
            onConfirm = { name, iconKey, colorHex ->
                onEvent(AddExpenseUiEvent.CreateCategory(name, iconKey, colorHex))
            },
            onDismiss = {
                onDismissCreateCategory()
                input = EntryInput.Amount
            },
        )
    }
}

@Composable
private fun EntryBody(
    uiState: AddExpenseUiState,
    wide: Boolean,
    showKeypad: Boolean,
    snapHideKeypad: Boolean,
    imeVisible: Boolean,
    amountActive: Boolean,
    isEdit: Boolean,
    onAmount: () -> Unit,
    onRemarkFocus: () -> Unit,
    onRemarkDone: () -> Unit,
    onHideKeypad: () -> Unit,
    onSave: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onPickAccount: (AccountTarget) -> Unit,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onCreateCategory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        if (wide) {
            Row(
                modifier = Modifier
                    .widthIn(max = WideFormMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .imeFit(),
            ) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    BillForm(
                        uiState = uiState,
                        amountActive = amountActive,
                        isEdit = isEdit,
                        onAmount = onAmount,
                        onRemarkFocus = onRemarkFocus,
                        onRemarkDone = onRemarkDone,
                        onPickDate = onPickDate,
                        onPickTime = onPickTime,
                        onPickAccount = onPickAccount,
                        onEvent = onEvent,
                        onCreateCategory = onCreateCategory,
                        onDelete = onDelete,
                        modifier = Modifier.weight(1f),
                    )
                    SaveBar(
                        saving = uiState.saving,
                        includeBottomInset = !imeVisible,
                        onSave = onSave,
                    )
                }
                if (!snapHideKeypad) {
                    SideKeypad(
                        visible = showKeypad,
                        enabled = !uiState.saving,
                        onKey = { onEvent(AddExpenseUiEvent.AmountKeyPressed(it)) },
                        onHide = onHideKeypad,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().imeFit()) {
                BillForm(
                    uiState = uiState,
                    amountActive = amountActive,
                    isEdit = isEdit,
                    onAmount = onAmount,
                    onRemarkFocus = onRemarkFocus,
                    onRemarkDone = onRemarkDone,
                    onPickDate = onPickDate,
                    onPickTime = onPickTime,
                    onPickAccount = onPickAccount,
                    onEvent = onEvent,
                    onCreateCategory = onCreateCategory,
                    onDelete = onDelete,
                    modifier = Modifier.weight(1f),
                )
                PhoneDock(
                    showKeypad = showKeypad,
                    snapHideKeypad = snapHideKeypad,
                    imeVisible = imeVisible,
                    saving = uiState.saving,
                    onKey = { onEvent(AddExpenseUiEvent.AmountKeyPressed(it)) },
                    onHideKeypad = onHideKeypad,
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun PhoneDock(
    showKeypad: Boolean,
    snapHideKeypad: Boolean,
    imeVisible: Boolean,
    saving: Boolean,
    onKey: (AmountKey) -> Unit,
    onHideKeypad: () -> Unit,
    onSave: () -> Unit,
) {
    if (snapHideKeypad) {
        SaveBar(
            saving = saving,
            includeBottomInset = !imeVisible,
            onSave = onSave,
        )
        return
    }
    val transition = remember { MutableTransitionState(showKeypad) }
    transition.targetState = showKeypad
    SaveBar(
        saving = saving,
        includeBottomInset = !showKeypad && !imeVisible,
        onSave = onSave,
    )
    AnimatedVisibility(
        visibleState = transition,
        enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
    ) {
        AmountKeypad(
            enabled = !saving,
            onKey = onKey,
            onHide = onHideKeypad,
            includeBottomInset = showKeypad,
        )
    }
}

@Composable
private fun RowScope.SideKeypad(
    visible: Boolean,
    enabled: Boolean,
    onKey: (AmountKey) -> Unit,
    onHide: () -> Unit,
) {
    val transition = remember { MutableTransitionState(visible) }
    transition.targetState = visible
    AnimatedVisibility(
        visibleState = transition,
        modifier = Modifier.align(Alignment.Bottom),
        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
    ) {
        AmountKeypad(
            enabled = enabled,
            onKey = onKey,
            onHide = onHide,
            modifier = Modifier.width(KeypadWidth),
        )
    }
}

@Composable
private fun BillForm(
    uiState: AddExpenseUiState,
    amountActive: Boolean,
    isEdit: Boolean,
    onAmount: () -> Unit,
    onRemarkFocus: () -> Unit,
    onRemarkDone: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onPickAccount: (AccountTarget) -> Unit,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onCreateCategory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxHeight()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AmountDisplay(
            amountText = uiState.amountText,
            symbol = uiState.currencySymbol,
            color = uiState.type.amountColor(),
            active = amountActive,
            enabled = !uiState.saving,
            onClick = onAmount,
        )
        DetailCard {
            DateRow(
                time = uiState.billTime,
                enabled = !uiState.saving,
                onDate = onPickDate,
                onTime = onPickTime,
            )
            HorizontalDivider(color = MaterialTheme.appColors.divider)
            if (uiState.type == BillType.Transfer) {
                AccountRow(
                    label = stringResource(R.string.bill_entry_from),
                    account = uiState.accounts.firstOrNull { it.id == uiState.accountId },
                    enabled = !uiState.saving,
                    onClick = { onPickAccount(AccountTarget.From) },
                )
                HorizontalDivider(color = MaterialTheme.appColors.divider)
                AccountRow(
                    label = stringResource(R.string.bill_entry_to),
                    account = uiState.accounts.firstOrNull { it.id == uiState.toAccountId },
                    emptyText = stringResource(R.string.bill_entry_error_second_account),
                    enabled = !uiState.saving,
                    onClick = { onPickAccount(AccountTarget.To) },
                )
            } else {
                AccountRow(
                    label = stringResource(R.string.bill_entry_account),
                    account = uiState.accounts.firstOrNull { it.id == uiState.accountId },
                    enabled = !uiState.saving,
                    onClick = { onPickAccount(AccountTarget.Single) },
                )
            }
        }
        if (uiState.type != BillType.Transfer) {
            CategorySection(
                categories = uiState.categories,
                selectedId = uiState.categoryId,
                enabled = !uiState.saving,
                onSelect = { onEvent(AddExpenseUiEvent.CategorySelected(it)) },
                onCreate = onCreateCategory,
            )
        }
        RemarkField(
            value = uiState.remark,
            enabled = !uiState.saving,
            onFocus = onRemarkFocus,
            onChange = { onEvent(AddExpenseUiEvent.RemarkChanged(it)) },
            onDone = onRemarkDone,
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun EntryTopBar(
    type: BillType,
    saving: Boolean,
    typeMenu: Boolean,
    onBack: () -> Unit,
    onTypeMenu: () -> Unit,
    onDismissTypeMenu: () -> Unit,
    onType: (BillType) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val showBack = LocalListDetailSceneScope.current == null
    val title = stringResource(type.titleRes())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPaddingTop()
            .heightIn(min = 48.dp),
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .interactiveClick(enabled = !saving, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    tint = colors.textPrimary,
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            Row(
                modifier = Modifier
                    .interactiveClick(enabled = !saving, onClick = onTypeMenu)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(R.string.bill_entry_switch_type),
                    tint = colors.textSecondary,
                )
            }
            DropdownMenu(
                expanded = typeMenu,
                onDismissRequest = onDismissTypeMenu,
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(16.dp),
            ) {
                BillType.entries.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(item.titleRes()),
                                color = colors.textPrimary,
                                fontWeight = if (item == type) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = if (item == type) {
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
                        onClick = { onType(item) },
                    )
                }
            }
        }
        if (showBack) {
            Box(Modifier.align(Alignment.CenterEnd).size(48.dp))
        }
    }
}

@Composable
private fun AmountDisplay(
    amountText: String,
    symbol: String,
    color: Color,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val shown = amountText.ifEmpty { stringResource(R.string.bill_entry_amount_hint) }
    val faded = amountText.isEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = symbol,
            color = colors.textSecondary,
            fontSize = 16.sp,
        )
        Text(
            text = shown,
            color = if (faded) colors.textTertiary else color,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(72.dp)
                .height(2.dp)
                .background(
                    if (active) color else Color.Transparent,
                    RoundedCornerShape(1.dp),
                ),
        )
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.appColors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = { content() })
    }
}

@Composable
private fun DateRow(
    time: LocalDateTime,
    enabled: Boolean,
    onDate: () -> Unit,
    onTime: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .interactiveClick(enabled = enabled, onClick = onDate),
        ) {
            Text(
                text = stringResource(R.string.bill_entry_date),
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            Text(
                text = formatEntryDate(time),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = formatEntryClock(time),
            color = colors.textPrimary,
            fontSize = 16.sp,
            modifier = Modifier.interactiveClick(enabled = enabled, onClick = onTime),
        )
    }
}

@Composable
private fun AccountRow(
    label: String,
    account: AccountChoice?,
    enabled: Boolean,
    onClick: () -> Unit,
    emptyText: String = stringResource(R.string.bill_entry_no_account),
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = colors.textSecondary, fontSize = 12.sp)
            if (account == null) {
                Text(text = emptyText, color = colors.textTertiary, fontSize = 16.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tint = accountColorValue(account.colorKey)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(tint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = accountIconImage(account.iconKey),
                            contentDescription = null,
                            tint = tint.onCategory(),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = account.name,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.bill_entry_category),
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp),
        )
        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.bill_entry_no_category),
                color = colors.textTertiary,
                fontSize = 14.sp,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            categories.forEach { category ->
                CategoryTile(
                    category = category,
                    selected = category.id == selectedId,
                    enabled = enabled,
                    onClick = { onSelect(category.id) },
                )
            }
            NewCategoryTile(enabled = enabled, onClick = onCreate)
        }
    }
}

@Composable
private fun CategoryTile(
    category: CategoryChoice,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val tint = accountColorValue(category.colorKey)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
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
                .width(68.dp),
        )
    }
}

@Composable
private fun NewCategoryTile(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
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
                imageVector = Icons.Rounded.Add,
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
                .width(68.dp),
        )
    }
}

@Composable
private fun RemarkField(
    value: String,
    enabled: Boolean,
    onFocus: () -> Unit,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.appColors
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
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) {
                    onFocus()
                    scope.launch { bringIntoView.bringIntoView() }
                }
            },
        enabled = enabled,
        label = { Text(stringResource(R.string.account_field_remark)) },
        placeholder = { Text(stringResource(R.string.account_field_remark_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            disabledTextColor = colors.textSecondary,
            focusedContainerColor = colors.cardBackground,
            unfocusedContainerColor = colors.cardBackground,
            disabledContainerColor = colors.cardBackground,
            cursorColor = colors.primary,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.divider,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textSecondary,
            focusedPlaceholderColor = colors.textTertiary,
            unfocusedPlaceholderColor = colors.textTertiary,
        ),
    )
}

@Composable
private fun Modifier.windowInsetsPaddingTop(): Modifier = windowInsetsPadding(
    screenContentWindowInsets().only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
)

@Composable
private fun SaveBar(
    saving: Boolean,
    includeBottomInset: Boolean,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val bottomInset = screenContentWindowInsets()
        .exclude(WindowInsets.ime)
        .only(WindowInsetsSides.Bottom)
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

private enum class EntryInput { Amount, Remark, Idle }

@Composable
private fun formatEntryDate(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "zh") "yyyy年M月d日" else "MMM d, yyyy"
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return time.format(formatter)
}

@Composable
private fun formatEntryClock(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)) {
        "HH:mm"
    } else {
        "h:mm a"
    }
    val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
    return time.format(formatter)
}

@Composable
private fun BillType.amountColor(): Color {
    val colors = MaterialTheme.appColors
    return when (this) {
        BillType.Expense -> colors.expense
        BillType.Income -> colors.income
        BillType.Transfer -> colors.textPrimary
    }
}

private fun BillType.titleRes(): Int = when (this) {
    BillType.Expense -> R.string.add_expense
    BillType.Income -> R.string.add_income
    BillType.Transfer -> R.string.add_transfer
}

private enum class AccountTarget { Single, From, To }

private val TwoPaneMinWidth = 840.dp
private val WideFormMaxWidth = 1040.dp
private val KeypadWidth = 320.dp

@FormFactorPreviews
@Composable
private fun AddExpensePreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AddExpenseScreen(
                uiState = previewState(type = BillType.Expense),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun AddTransferPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AddExpenseScreen(
                uiState = previewState(type = BillType.Transfer),
                onEvent = {},
                onBack = {},
            )
        }
    }
}

private fun previewState(type: BillType): AddExpenseUiState {
    val categories = listOf(
        CategoryChoice(1, "餐饮", "restaurant", "#FFD96C6C"),
        CategoryChoice(2, "交通", "directions_bus", "#FF78A898"),
        CategoryChoice(3, "购物", "shopping_bag", "#FFE0A15A"),
        CategoryChoice(4, "居住", "home", "#FF7EA0C4"),
    )
    return AddExpenseUiState(
        ready = true,
        type = type,
        amountText = "128.50",
        categories = if (type == BillType.Transfer) emptyList() else categories,
        categoryId = if (type == BillType.Transfer) null else 1L,
        accounts = listOf(
            AccountChoice(1, "Cash", "cash", "teal"),
            AccountChoice(2, "Salary", "card", "blue"),
        ),
        accountId = 1L,
        toAccountId = if (type == BillType.Transfer) 2L else null,
        billTime = LocalDateTime.of(2026, 10, 18, 9, 30),
        currencySymbol = "¥",
    )
}
