package com.cyr.cognobal.screen.feature.bill.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.AccountType
import com.cyr.cognobal.data.repository.LedgerReject
import com.cyr.cognobal.screen.feature.bill.AccountHeroSurface
import com.cyr.cognobal.screen.feature.bill.account.state.AccountEditorUiEvent
import com.cyr.cognobal.screen.feature.bill.account.state.AccountEditorUiState
import com.cyr.cognobal.screen.feature.bill.account.state.deleteBlock
import com.cyr.cognobal.screen.feature.bill.model.AccountColor
import com.cyr.cognobal.screen.feature.bill.model.accountMessageRes
import com.cyr.cognobal.screen.feature.bill.model.labelRes
import com.cyr.cognobal.screen.feature.bill.model.pairedIcon
import com.cyr.cognobal.screen.feature.bill.tool.formatMinorAmount
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.bill.tool.parseMajorAmount
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.background.RequestPageBackground
import com.cyr.cognobal.ui.insets.listContentPadding
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors

@Composable
internal fun AccountEditorRoute(
    accountId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountEditorViewModel = viewModel(
        key = "account-editor-${accountId ?: "new"}",
        factory = AccountEditorViewModel.factory(accountId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.finished.collect { onBack() }
    }
    AccountEditorScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun AccountEditorScreen(
    uiState: AccountEditorUiState,
    onEvent: (AccountEditorUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(
        if (uiState.isEdit) R.string.account_editor_edit_title else R.string.account_editor_create_title,
    )
    AccountEditorScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
    ) {
        if (!uiState.ready) return@AccountEditorScaffold
        if (uiState.missing) {
            Text(
                text = stringResource(R.string.account_editor_missing),
                color = MaterialTheme.appColors.textSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.close), color = MaterialTheme.appColors.primary)
            }
            return@AccountEditorScaffold
        }
        AccountForm(uiState = uiState, onEvent = onEvent)
    }
    if (uiState.confirmDelete) {
        DeleteAccountDialog(
            onConfirm = { onEvent(AccountEditorUiEvent.ConfirmDelete) },
            onDismiss = { onEvent(AccountEditorUiEvent.DismissDelete) },
        )
    }
}

@Composable
private fun AccountForm(
    uiState: AccountEditorUiState,
    onEvent: (AccountEditorUiEvent) -> Unit,
) {
    val colors = MaterialTheme.appColors
    AccountPreviewCard(uiState = uiState)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountType.entries.forEach { type ->
            TypeChoice(
                type = type,
                selected = uiState.type == type,
                enabled = !uiState.saving,
                onClick = { onEvent(AccountEditorUiEvent.TypeSelected(type)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SoftField(
            value = uiState.name,
            onValueChange = { onEvent(AccountEditorUiEvent.NameChanged(it)) },
            placeholder = stringResource(R.string.account_field_name),
            enabled = !uiState.saving,
        )
        if (!uiState.isEdit) {
            SoftField(
                value = uiState.openingBalanceText,
                onValueChange = { onEvent(AccountEditorUiEvent.OpeningBalanceChanged(it)) },
                placeholder = stringResource(R.string.account_field_opening),
                prefix = uiState.currencySymbol,
                enabled = !uiState.saving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        SoftField(
            value = uiState.remark,
            onValueChange = { onEvent(AccountEditorUiEvent.RemarkChanged(it)) },
            placeholder = stringResource(R.string.account_field_remark),
            enabled = !uiState.saving,
            singleLine = false,
            minLines = 2,
        )
    }
    if (uiState.isEdit && !(uiState.enabled && uiState.enabledCount <= 1)) {
        AccountEnableToggle(
            enabled = uiState.enabled,
            saving = uiState.saving,
            onToggle = { onEvent(AccountEditorUiEvent.SetEnabled(it)) },
        )
    }
    Column {
        FormMessage(error = uiState.error, color = colors.expense)
        Button(
            onClick = { onEvent(AccountEditorUiEvent.Save) },
            enabled = !uiState.saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.inkStrong,
                contentColor = colors.onInkStrong,
                disabledContainerColor = colors.inkStrong.copy(alpha = 0.4f),
                disabledContentColor = colors.onInkStrong.copy(alpha = 0.7f),
            ),
        ) {
            Text(
                text = stringResource(R.string.account_save),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
    if (uiState.isEdit && uiState.deleteBlock() == null) {
        TextButton(
            onClick = { onEvent(AccountEditorUiEvent.DeleteClicked) },
            enabled = !uiState.saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.account_delete),
                color = colors.expense,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun AccountEnableToggle(
    enabled: Boolean,
    saving: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = colors.cardBackground,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .then(
                    if (focused && !saving) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, shape)
                    } else {
                        Modifier
                    },
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .toggleable(
                    value = enabled,
                    enabled = !saving,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.account_status_enabled),
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = null,
                enabled = !saving,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .clearAndSetSemantics {},
            )
        }
    }
}

/**
 * 与首页 Bento 账户卡一脉相承的纯平品牌主色预览卡：左上半透明白圆图标块，左下账户名与大号等宽余额。
 */
@Composable
private fun AccountPreviewCard(uiState: AccountEditorUiState) {
    val unnamed = uiState.name.isBlank()
    val name = if (unnamed) {
        stringResource(R.string.account_field_name)
    } else {
        uiState.name
    }
    val amount = if (uiState.isEdit) {
        formatMoney(uiState.currencySymbol, uiState.currentBalance ?: 0L)
    } else {
        openingPreviewAmount(uiState)
    }
    AccountHeroSurface { paint ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp)
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(paint.identity.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = uiState.type.pairedIcon().image,
                    contentDescription = null,
                    tint = paint.identity,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = name,
                    color = paint.on.copy(alpha = if (unnamed) 0.68f else 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    text = amount,
                    color = paint.on,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.displayMedium,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 22.sp,
                        maxFontSize = 32.sp,
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )
            }
        }
    }
}

private fun openingPreviewAmount(uiState: AccountEditorUiState): String {
    val parsed = parseMajorAmount(uiState.openingBalanceText)
    val body = if (parsed != null) {
        formatMinorAmount(parsed)
    } else {
        uiState.openingBalanceText.trim().ifEmpty { "0.00" }
    }
    return uiState.currencySymbol + body
}

@Composable
private fun TypeChoice(
    type: AccountType,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(type.labelRes())
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (selected) colors.inkStrong else colors.cardBackground,
                    RoundedCornerShape(18.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = type.pairedIcon().image,
                contentDescription = null,
                tint = if (selected) colors.onInkStrong else colors.textSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun FormMessage(
    error: LedgerReject?,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (error != null) {
            Text(
                text = stringResource(error.accountMessageRes()),
                color = color,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val colors = MaterialTheme.appColors
    var focused by remember { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) {
        if (focused) bringIntoView.bringIntoView()
    }
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = if (enabled) colors.textPrimary else colors.textSecondary,
        ),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged { focused = it.isFocused }
            .then(
                if (focused) {
                    Modifier.border(1.dp, colors.primary.copy(alpha = 0.35f), shape)
                } else {
                    Modifier
                },
            )
            .background(colors.cardBackground, shape)
            .semantics { contentDescription = placeholder }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                if (prefix != null) {
                    Text(
                        text = prefix,
                        color = colors.primary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.account_delete_title),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.account_delete_message),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.account_delete_confirm),
                    color = colors.expense,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
            }
        },
        containerColor = colors.cardBackground,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
internal fun AccountDetailPlaceholder(modifier: Modifier = Modifier) {
    RequestPageBackground()
    val colors = MaterialTheme.appColors
    val contentPadding = listContentPadding()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.bill_account_placeholder),
            color = colors.textTertiary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AccountEditorScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showBack = LocalListDetailSceneScope.current == null
    val contentPadding = listContentPadding()
    val scrollState = rememberScrollState()
    RequestPageBackground()
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .padding(horizontal = if (maxWidth >= 600.dp) 28.dp else 20.dp)
                .verticalScroll(scrollState)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            EditorHeader(title = title, onBack = onBack, showBack = showBack)
            content()
        }
    }
}

@Composable
private fun EditorHeader(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .interactiveClick(onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.cardBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@FormFactorPreviews
@Composable
private fun AccountEditorPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AccountEditorScreen(
                uiState = AccountEditorUiState(
                    ready = true,
                    name = "储蓄卡",
                    type = AccountType.DebitCard,
                    color = AccountColor.Ink,
                    openingBalanceText = "1280",
                    currencySymbol = "¥",
                ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
