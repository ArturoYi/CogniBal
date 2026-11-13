package com.cyr.cognobal.screen.feature.expense

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.screen.feature.bill.model.accountIconImage
import com.cyr.cognobal.screen.feature.expense.model.AddBillIssue
import com.cyr.cognobal.screen.feature.expense.model.categoryIcon
import com.cyr.cognobal.screen.feature.expense.model.messageRes
import com.cyr.cognobal.screen.feature.expense.state.AccountChoice
import com.cyr.cognobal.screen.feature.expense.state.AddExpenseUiEvent
import com.cyr.cognobal.screen.feature.expense.state.AddExpenseUiState
import com.cyr.cognobal.screen.feature.expense.state.CategoryChoice
import com.cyr.cognobal.screen.feature.expense.state.CategoryCreateResult
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.background.PageBackgroundStyle
import com.cyr.cognobal.ui.background.RequestPageBackground
import com.cyr.cognobal.ui.format.formatCalendarDay
import com.cyr.cognobal.ui.insets.screenContentWindowInsets
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.softBadgeTone
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 记一笔（参考设计重构）：
 * 1. 顶部：左侧关闭 ✕ · 居中「支出 / 收入 / 转账」轻指示栏
 * 2. 金额舞台：大号等宽金额 + 选中分类与入账账户胶囊（点按可切换账户）
 * 3. 中部：分类矩阵（进入时不预选）或双账户转账卡
 * 4. 底部：备注 + 日期时间。支出/收入在选中分类后才展开数字键盘，转账直接展开。
 *    键盘右侧上为退格，右下为贯通的「完成」
 */
@Composable
internal fun AddExpenseRoute(
    typeCode: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    billId: Long? = null,
    viewModel: AddExpenseViewModel = viewModel(
        key = AddExpenseViewModel.key(typeCode, billId),
        factory = AddExpenseViewModel.factory(typeCode, billId),
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
    val colors = MaterialTheme.appColors
    RequestPageBackground(PageBackgroundStyle(color = colors.cardBackground))
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val transfer = uiState.type == BillType.Transfer
    var pickingAccount by rememberSaveable { mutableStateOf(false) }
    var pickingFrom by rememberSaveable { mutableStateOf(false) }
    var pickingTo by rememberSaveable { mutableStateOf(false) }
    var remarkFocused by remember { mutableStateOf(false) }
    val formScroll = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(transfer) {
        if (!transfer) {
            pickingFrom = false
            pickingTo = false
        } else {
            pickingAccount = false
        }
    }

    val horizontalInsets = screenContentWindowInsets().only(WindowInsetsSides.Horizontal)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.cardBackground)
            .onPreviewKeyEvent { event ->
                if (remarkFocused || event.type != KeyEventType.KeyDown || uiState.saving) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.Backspace, Key.Delete -> {
                        onEvent(AddExpenseUiEvent.AmountChanged(uiState.amountText.dropLast(1)))
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        onEvent(AddExpenseUiEvent.Save)
                        true
                    }
                    else -> {
                        val ch = event.utf16CodePoint.toChar()
                        if (ch.isDigit() || ch == '.') {
                            onEvent(AddExpenseUiEvent.AmountChanged(uiState.amountText + ch))
                            true
                        } else {
                            false
                        }
                    }
                }
            },
    ) {
        TypeHeaderBar(
            type = uiState.type,
            typeEnabled = uiState.ready && !uiState.saving && !uiState.editing,
            actionEnabled = uiState.ready && !uiState.saving,
            backEnabled = !uiState.saving,
            isEdit = isEdit,
            onBack = onBack,
            onType = { onEvent(AddExpenseUiEvent.TypeSelected(it)) },
            onDelete = onDelete,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(horizontalInsets)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val compactHeight = maxHeight < 520.dp
            if (uiState.missing) {
                Text(
                    text = stringResource(R.string.bill_detail_missing),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            } else if (uiState.ready) {
                Column(
                    modifier = Modifier
                        .widthIn(max = FormMaxWidth)
                        .fillMaxSize(),
                ) {
                    val selectedCategory = uiState.categories.firstOrNull { it.id == uiState.categoryId }
                    val selectedAccount = uiState.accounts.firstOrNull { it.id == uiState.accountId }

                    HeroAmountDisplay(
                        amountText = uiState.amountText,
                        symbol = uiState.currencySymbol,
                        transfer = transfer,
                        category = selectedCategory,
                        account = selectedAccount,
                        canPickAccount = !transfer && uiState.accounts.isNotEmpty() && !uiState.saving,
                        issue = uiState.issue,
                        compact = compactHeight,
                        onFocusAmount = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        },
                        onPickAccount = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            pickingAccount = true
                        },
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(formScroll)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (transfer) {
                            TransferAccountsBento(
                                accounts = uiState.accounts,
                                fromId = uiState.accountId,
                                toId = uiState.toAccountId,
                                enabled = !uiState.saving,
                                onPickFrom = { pickingFrom = true },
                                onPickTo = { pickingTo = true },
                            )
                        } else {
                            CategoryBentoSection(
                                categories = uiState.categories,
                                selectedId = uiState.categoryId,
                                enabled = !uiState.saving,
                                onSelect = { categoryId ->
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    onEvent(AddExpenseUiEvent.CategorySelected(categoryId))
                                },
                                onCreate = {
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    onShowCreateCategory()
                                },
                            )
                        }
                    }

                    val keypadReady = transfer || uiState.categoryId != null
                    EntryMetaAndKeypad(
                        remark = uiState.remark,
                        billTime = uiState.billTime,
                        amountText = uiState.amountText,
                        enabled = !uiState.saving,
                        showKeypad = keypadReady && !imeVisible,
                        compactKeypad = compactHeight,
                        onRemarkChange = { onEvent(AddExpenseUiEvent.RemarkChanged(it)) },
                        onRemarkFocus = { remarkFocused = it },
                        onRemarkDone = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        },
                        onPickDate = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onShowDate()
                        },
                        onPickTime = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onShowTime()
                        },
                        onKey = { next -> onEvent(AddExpenseUiEvent.AmountChanged(next)) },
                        onDone = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            onEvent(AddExpenseUiEvent.Save)
                        },
                    )
                }
            }
        }
    }

    if (showCreateCategory) {
        CreateCategorySheet(
            error = createError,
            onConfirm = { name, iconKey ->
                onEvent(AddExpenseUiEvent.CreateCategory(name, iconKey))
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
    if (!transfer && pickingAccount) {
        AccountPickDialog(
            title = stringResource(R.string.bill_entry_account),
            accounts = uiState.accounts,
            selectedId = uiState.accountId,
            onSelect = { accountId ->
                onEvent(AddExpenseUiEvent.AccountSelected(accountId))
                pickingAccount = false
            },
            onDismiss = { pickingAccount = false },
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
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.budget_over_message, names),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
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
            shape = MaterialTheme.shapes.extraLarge,
        )
    }

    if (uiState.confirmSave) {
        AlertDialog(
            onDismissRequest = { onEvent(AddExpenseUiEvent.DismissSaveConfirm) },
            title = {
                Text(
                    text = stringResource(R.string.bill_entry_save_confirm_title),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.bill_entry_save_confirm_message),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(AddExpenseUiEvent.ConfirmSave) }) {
                    Text(
                        text = stringResource(R.string.bill_entry_save_confirm_action),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AddExpenseUiEvent.DismissSaveConfirm) }) {
                    Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                }
            },
            containerColor = colors.cardBackground,
            shape = MaterialTheme.shapes.extraLarge,
        )
    }
}

@Composable
private fun TypeHeaderBar(
    type: BillType,
    typeEnabled: Boolean,
    actionEnabled: Boolean,
    backEnabled: Boolean,
    isEdit: Boolean,
    onBack: () -> Unit,
    onType: (BillType) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = FormMaxWidth)
                .fillMaxWidth()
                .windowInsetsPadding(
                    screenContentWindowInsets().only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .interactiveClick(enabled = backEnabled, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypeUnderlineTab(
                    label = stringResource(R.string.bill_fab_expense),
                    selected = type == BillType.Expense,
                    enabled = typeEnabled,
                    onClick = { onType(BillType.Expense) },
                )
                TypeUnderlineTab(
                    label = stringResource(R.string.bill_fab_income),
                    selected = type == BillType.Income,
                    enabled = typeEnabled,
                    onClick = { onType(BillType.Income) },
                )
                TypeUnderlineTab(
                    label = stringResource(R.string.bill_fab_transfer),
                    selected = type == BillType.Transfer,
                    enabled = typeEnabled,
                    onClick = { onType(BillType.Transfer) },
                )
            }

            if (isEdit) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .interactiveClick(enabled = actionEnabled, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.bill_entry_delete),
                        tint = colors.expense,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun TypeUnderlineTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .semantics { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = if (selected) colors.textPrimary else colors.textTertiary,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(if (selected) colors.primary else Color.Transparent),
        )
    }
}

@Composable
private fun HeroAmountDisplay(
    amountText: String,
    symbol: String,
    transfer: Boolean,
    category: CategoryChoice?,
    account: AccountChoice?,
    canPickAccount: Boolean,
    issue: AddBillIssue?,
    compact: Boolean,
    onFocusAmount: () -> Unit,
    onPickAccount: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val amountLabel = stringResource(R.string.bill_entry_amount)
    val displayAmount = amountText.ifEmpty { stringResource(R.string.bill_entry_amount_hint) }
    val hasAmount = amountText.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = if (compact) 4.dp else 12.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .semantics { contentDescription = "$amountLabel $symbol$displayAmount" }
                .interactiveClick(onClick = onFocusAmount)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = symbol,
                color = if (hasAmount) colors.textPrimary else colors.textTertiary.copy(alpha = 0.55f),
                style = TextStyle(
                    fontSize = if (compact) 22.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier
                    .alignByBaseline()
                    .padding(end = 4.dp),
            )
            Text(
                text = displayAmount,
                color = if (hasAmount) colors.textPrimary else colors.textTertiary.copy(alpha = 0.45f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = if (compact) 32.sp else 40.sp,
                    lineHeight = if (compact) 38.sp else 46.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    fontFeatureSettings = "tnum",
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 22.sp,
                    maxFontSize = if (compact) 32.sp else 40.sp,
                    stepSize = 1.sp,
                ),
                modifier = Modifier.alignByBaseline(),
            )
        }

        if (!transfer && (category != null || account != null)) {
            val tone = softBadgeTone(category?.colorKey.orEmpty())
            val pillLabel = buildList {
                if (category != null) add(category.name)
                if (account != null) add(account.name)
            }.joinToString(" · ")

            Row(
                modifier = Modifier
                    .heightIn(min = 34.dp)
                    .clip(CircleShape)
                    .background(colors.background)
                    .then(
                        if (canPickAccount) {
                            Modifier.interactiveClick(onClick = onPickAccount)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (category != null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(tone.container, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIcon(category.iconKey),
                            contentDescription = null,
                            tint = tone.content,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Text(
                    text = pillLabel,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (canPickAccount && account != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        if (issue != null) {
            Text(
                text = stringResource(issue.messageRes()),
                color = colors.expense,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun TransferAccountsBento(
    accounts: List<AccountChoice>,
    fromId: Long?,
    toId: Long?,
    enabled: Boolean,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val from = accounts.firstOrNull { it.id == fromId }
    val to = accounts.firstOrNull { it.id == toId }
    val fromSemantic = stringResource(R.string.bill_entry_from) + " " +
        (from?.name ?: stringResource(R.string.bill_entry_no_account))
    val toSemantic = stringResource(R.string.bill_entry_to) + " " +
        (to?.name ?: stringResource(R.string.bill_entry_error_second_account))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TransferAccountCard(
            account = from,
            fallback = stringResource(R.string.bill_entry_no_account),
            semanticLabel = fromSemantic,
            enabled = enabled && accounts.isNotEmpty(),
            emphasized = false,
            onClick = onPickFrom,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        TransferAccountCard(
            account = to,
            fallback = stringResource(R.string.bill_entry_error_second_account),
            semanticLabel = toSemantic,
            enabled = enabled && accounts.size > 1,
            emphasized = true,
            onClick = onPickTo,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TransferAccountCard(
    account: AccountChoice?,
    fallback: String,
    semanticLabel: String,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val bg = if (emphasized && account != null) colors.primaryContainer else colors.background
    val tone = softBadgeTone(account?.colorKey.orEmpty())
    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .semantics(mergeDescendants = true) { contentDescription = semanticLabel }
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (account != null) tone.container else colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (account != null) {
                Icon(
                    imageVector = accountIconImage(account.iconKey),
                    contentDescription = null,
                    tint = tone.content,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = account?.name ?: fallback,
            color = if (account != null) colors.textPrimary else colors.textTertiary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryBentoSection(
    categories: List<CategoryChoice>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (categories.isEmpty()) {
            Text(
                text = stringResource(R.string.bill_entry_no_category),
                color = colors.textTertiary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = (maxWidth / CategoryTileMinWidth).toInt().coerceAtLeast(4)
            val count = categories.size + 1
            val rows = (count + columns - 1) / columns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
}

private val CategoryTileMinWidth = 76.dp

@Composable
private fun CategoryTile(
    category: CategoryChoice,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val tone = softBadgeTone(category.colorKey)
    val tileShape = RoundedCornerShape(16.dp)
    val iconShape = RoundedCornerShape(14.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(tileShape)
            .then(
                if (selected) {
                    Modifier
                        .background(colors.primaryContainer, tileShape)
                        .border(1.dp, colors.primary.copy(alpha = 0.22f), tileShape)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (selected) colors.cardBackground.copy(alpha = 0.85f) else tone.container,
                    iconShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = categoryIcon(category.iconKey),
                contentDescription = null,
                tint = tone.content,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = category.name,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
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
    val label = stringResource(R.string.bill_entry_new_category)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) { contentDescription = label }
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.tagBackground, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = label,
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun EntryMetaAndKeypad(
    remark: String,
    billTime: LocalDateTime,
    amountText: String,
    enabled: Boolean,
    showKeypad: Boolean,
    compactKeypad: Boolean,
    onRemarkChange: (String) -> Unit,
    onRemarkFocus: (Boolean) -> Unit,
    onRemarkDone: () -> Unit,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onKey: (String) -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val bottomInset = navigationBottomInset()
    val remarkPlaceholder = stringResource(R.string.account_field_remark)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (imeVisible) {
                        Modifier
                    } else {
                        Modifier
                            .windowInsetsPadding(bottomInset)
                            .consumeWindowInsets(bottomInset)
                    },
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = remark,
                onValueChange = onRemarkChange,
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRemarkDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.cardBackground)
                    .onFocusChanged { onRemarkFocus(it.isFocused) }
                    .semantics { contentDescription = remarkPlaceholder }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (remark.isEmpty()) {
                            Text(
                                text = remarkPlaceholder,
                                color = colors.textTertiary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        inner()
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val dateValue = localizedEntryDate(billTime)
                val timeValue = localizedBillTime(billTime)
                val dateSemantic = "${stringResource(R.string.bill_entry_date)} $dateValue"
                val timeSemantic = "${stringResource(R.string.bill_entry_time)} $timeValue"

                Row(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .semantics(mergeDescendants = true) { contentDescription = dateSemantic }
                        .interactiveClick(enabled = enabled, onClick = onPickDate)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = dateValue,
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }

                Row(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .semantics(mergeDescendants = true) { contentDescription = timeSemantic }
                        .interactiveClick(enabled = enabled, onClick = onPickTime)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = timeValue,
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelLarge.merge(
                            TextStyle(fontFeatureSettings = "tnum"),
                        ),
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

            AnimatedVisibility(
                visible = showKeypad,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                NumericKeypad(
                    amountText = amountText,
                    enabled = enabled,
                    keyHeight = if (compactKeypad) 48.dp else 56.dp,
                    onKey = onKey,
                    onDone = onDone,
                )
            }
        }
    }
}

@Composable
private fun NumericKeypad(
    amountText: String,
    enabled: Boolean,
    keyHeight: Dp,
    onKey: (String) -> Unit,
    onDone: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )
    val doneHeight = keyHeight * 3 + KeypadGap * 2
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KeypadGap),
    ) {
        Column(
            modifier = Modifier.weight(3f),
            verticalArrangement = Arrangement.spacedBy(KeypadGap),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KeypadGap),
                ) {
                    row.forEach { digit ->
                        KeypadDigitButton(
                            label = digit,
                            contentDesc = digit,
                            enabled = enabled,
                            height = keyHeight,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKey(amountText + digit)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KeypadGap),
            ) {
                KeypadDigitButton(
                    label = ".",
                    contentDesc = stringResource(R.string.bill_entry_key_decimal),
                    enabled = enabled,
                    height = keyHeight,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKey(if (amountText.isEmpty()) "0." else "$amountText.")
                    },
                    modifier = Modifier.weight(1f),
                )
                KeypadDigitButton(
                    label = "0",
                    contentDesc = "0",
                    enabled = enabled,
                    height = keyHeight,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKey(amountText + "0")
                    },
                    modifier = Modifier.weight(2f),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KeypadGap),
        ) {
            KeypadBackspaceButton(
                enabled = enabled,
                height = keyHeight,
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onKey(amountText.dropLast(1))
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onKey("")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            KeypadDoneButton(
                enabled = enabled,
                height = doneHeight,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    contentDesc: String,
    enabled: Boolean,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "keyScale",
    )
    val shape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(shape)
            .background(if (pressed) colors.tagBackground else colors.cardBackground)
            .semantics {
                role = Role.Button
                contentDescription = contentDesc
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .interactiveClick(
                enabled = enabled,
                interactionSource = interaction,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = colors.textPrimary,
            style = TextStyle(
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

@Composable
private fun KeypadDoneButton(
    enabled: Boolean,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(R.string.done)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "doneScale",
    )
    val shape = RoundedCornerShape(13.dp)
    val background = when {
        !enabled -> colors.tagBackground
        pressed -> colors.inkStrong.copy(alpha = 0.86f)
        else -> colors.inkStrong
    }
    val content = if (enabled) colors.onInkStrong else colors.textTertiary

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(shape)
            .background(background)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .interactiveClick(
                enabled = enabled,
                interactionSource = interaction,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 11.sp,
                maxFontSize = 16.sp,
                stepSize = 1.sp,
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadBackspaceButton(
    enabled: Boolean,
    height: Dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val desc = stringResource(R.string.bill_entry_key_delete)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (pressed) colors.tagBackground else colors.cardBackground)
            .semantics {
                role = Role.Button
                contentDescription = desc
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Backspace,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun localizedEntryDate(time: LocalDateTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val date = time.toLocalDate()
    val dayShort = formatCalendarDay(date, locale)
    return if (date == LocalDate.now()) {
        "${stringResource(R.string.bill_recent_today)} $dayShort"
    } else {
        dayShort
    }
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
private fun navigationBottomInset() = screenContentWindowInsets()
    .exclude(WindowInsets.ime)
    .only(WindowInsetsSides.Bottom)

private val FormMaxWidth = 600.dp
private val KeypadGap = 8.dp

@FormFactorPreviews
@Composable
private fun AddExpenseAwaitCategoryPreview() {
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
private fun AddExpenseTagsPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AddExpenseScreen(
                uiState = previewState(categoryId = 3L, amountText = "156.80"),
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
                        AccountChoice(2, "储蓄卡", "bank", "blue"),
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

private fun previewState(categoryId: Long?, amountText: String): AddExpenseUiState {
    val categories = listOf(
        CategoryChoice(1, "餐饮", "restaurant", "#FFF03E3E"),
        CategoryChoice(2, "交通", "directions_bus", "#FF0CA678"),
        CategoryChoice(3, "购物", "shopping_bag", "#FFFD7E14"),
        CategoryChoice(4, "居住", "home", "#FF228BE6"),
        CategoryChoice(5, "娱乐", "movie", "#FFBE4BDB"),
        CategoryChoice(6, "医疗", "medical_services", "#FF1098AD"),
        CategoryChoice(7, "教育", "school", "#FF495057"),
        CategoryChoice(8, "通讯", "chat", "#FF4C6EF5"),
    )
    return AddExpenseUiState(
        ready = true,
        type = BillType.Expense,
        amountText = amountText,
        categories = categories,
        categoryId = categoryId,
        accounts = listOf(AccountChoice(1, "储蓄卡", "bank", "blue")),
        accountId = 1L,
        billTime = LocalDateTime.now(),
        currencySymbol = "¥",
    )
}
