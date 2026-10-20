package com.example.cognibal.screen.feature.bill.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.screen.feature.bill.model.AccountColor
import com.example.cognibal.screen.feature.bill.model.AccountIcon
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.bill.account.state.AccountDeleteBlock
import com.example.cognibal.screen.feature.bill.account.state.AccountEditorUiEvent
import com.example.cognibal.screen.feature.bill.account.state.AccountEditorUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.screen.feature.bill.model.accountMessageRes
import com.example.cognibal.screen.feature.bill.model.labelRes
import com.example.cognibal.screen.feature.bill.account.state.deleteBlock

/** 新建或编辑账户。宽屏时它是账单列表右侧的详情，窄屏时单独占满一页。 */
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
                fontSize = 15.sp,
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
    val fieldColors = accountFieldColors()
    FormCard {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onEvent(AccountEditorUiEvent.NameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.saving,
            label = { Text(stringResource(R.string.account_field_name)) },
            placeholder = { Text(stringResource(R.string.account_field_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
        )
        FieldLabel(stringResource(R.string.account_field_type))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountType.entries.forEach { type ->
                ChoiceChip(
                    label = stringResource(type.labelRes()),
                    selected = uiState.type == type,
                    enabled = !uiState.saving,
                    onClick = { onEvent(AccountEditorUiEvent.TypeSelected(type)) },
                )
            }
        }
    }
    FormCard {
        FieldLabel(stringResource(R.string.account_field_icon))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountIcon.entries.forEach { icon ->
                IconChoice(
                    icon = icon,
                    selected = uiState.iconKeyOverride == null && uiState.icon == icon,
                    enabled = !uiState.saving,
                    onClick = { onEvent(AccountEditorUiEvent.IconSelected(icon)) },
                )
            }
        }
        FieldLabel(stringResource(R.string.account_field_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccountColor.entries.forEach { color ->
                ColorChoice(
                    color = color,
                    selected = uiState.colorKeyOverride == null && uiState.color == color,
                    enabled = !uiState.saving,
                    onClick = { onEvent(AccountEditorUiEvent.ColorSelected(color)) },
                )
            }
        }
    }
    FormCard {
        if (uiState.isEdit) {
            FieldLabel(stringResource(R.string.account_field_balance))
            val balance = uiState.currentBalance ?: 0L
            Text(
                text = uiState.currencySymbol + formatMinorAmount(balance),
                color = if (balance < 0) colors.expense else colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.account_field_balance_hint),
                color = colors.textTertiary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        } else {
            OutlinedTextField(
                value = uiState.openingBalanceText,
                onValueChange = { onEvent(AccountEditorUiEvent.OpeningBalanceChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.saving,
                label = { Text(stringResource(R.string.account_field_opening)) },
                prefix = { Text(uiState.currencySymbol) },
                supportingText = {
                    Text(
                        text = stringResource(
                            if (uiState.type == AccountType.CreditCard) {
                                R.string.account_field_opening_credit
                            } else {
                                R.string.account_field_opening_hint
                            },
                        ),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
            )
        }
        OutlinedTextField(
            value = uiState.remark,
            onValueChange = { onEvent(AccountEditorUiEvent.RemarkChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.saving,
            label = { Text(stringResource(R.string.account_field_remark)) },
            placeholder = { Text(stringResource(R.string.account_field_remark_hint)) },
            minLines = 2,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
        )
    }
    if (uiState.isEdit) {
        AccountAvailability(uiState = uiState, onEvent = onEvent)
    }
    val error = uiState.error
    if (error != null) {
        Text(
            text = stringResource(error.accountMessageRes()),
            color = colors.expense,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
    Button(
        onClick = { onEvent(AccountEditorUiEvent.Save) },
        enabled = !uiState.saving,
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
            text = stringResource(R.string.account_save),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    if (uiState.isEdit) {
        AccountRemoval(uiState = uiState, onEvent = onEvent)
    }
}

@Composable
private fun AccountAvailability(
    uiState: AccountEditorUiState,
    onEvent: (AccountEditorUiEvent) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val lastEnabled = uiState.enabled && uiState.enabledCount <= 1
    FormCard {
        FieldLabel(stringResource(R.string.account_status))
        Text(
            text = stringResource(
                if (uiState.enabled) R.string.account_status_enabled else R.string.account_status_disabled,
            ),
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (uiState.isDefault) {
            Text(
                text = stringResource(R.string.account_current_display),
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        Text(
            text = stringResource(
                if (uiState.enabled) R.string.account_disable_hint else R.string.account_enable_hint,
            ),
            color = colors.textTertiary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (lastEnabled) {
            Text(
                text = stringResource(R.string.account_last_enabled_hint),
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        } else if (uiState.enabled) {
            TextButton(
                onClick = { onEvent(AccountEditorUiEvent.SetEnabled(false)) },
                enabled = !uiState.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.account_disable),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            TextButton(
                onClick = { onEvent(AccountEditorUiEvent.SetEnabled(true)) },
                enabled = !uiState.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.account_enable),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AccountRemoval(
    uiState: AccountEditorUiState,
    onEvent: (AccountEditorUiEvent) -> Unit,
) {
    val colors = MaterialTheme.appColors
    when (uiState.deleteBlock()) {
        null -> TextButton(
            onClick = { onEvent(AccountEditorUiEvent.DeleteClicked) },
            enabled = !uiState.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.account_delete),
                color = colors.expense,
                fontWeight = FontWeight.Bold,
            )
        }
        AccountDeleteBlock.Bills -> RemovalNote(stringResource(R.string.account_delete_blocked_bills))
        AccountDeleteBlock.Default -> RemovalNote(stringResource(R.string.account_delete_blocked_default))
        AccountDeleteBlock.LastEnabled -> Unit
    }
}

@Composable
private fun RemovalNote(text: String) {
    Text(
        text = text,
        color = MaterialTheme.appColors.textTertiary,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.appColors.cardBackground,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.appColors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val container = if (selected) colors.primary else colors.tagBackground
    val content = if (selected) colors.onPrimary else colors.textPrimary
    Text(
        text = label,
        color = content,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .semantics { this.selected = selected }
                .background(container, RoundedCornerShape(999.dp))
                .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun IconChoice(
    icon: AccountIcon,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(icon.labelRes)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (selected) colors.primary else colors.tagBackground,
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon.image,
                contentDescription = label,
                tint = if (selected) colors.onPrimary else colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            color = if (selected) colors.primary else colors.textSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).width(52.dp),
        )
    }
}

@Composable
private fun ColorChoice(
    color: AccountColor,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(color.labelRes)
    val ring = if (selected) {
        Modifier.border(2.dp, colors.primary, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                this.selected = selected
                contentDescription = label
            }
            .then(ring)
            .padding(4.dp)
            .background(color.color, CircleShape)
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick),
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.account_delete_message),
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
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
        shape = RoundedCornerShape(20.dp),
    )
}

/**
 * 宽屏还没打开表单时，详情窗格里的空状态。
 */
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
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 表单骨架。安全区（含输入法）垫在滚动外面，焦点不会被键盘挡住，也不会垫出两层。
 * 列表和详情并排时不画返回键。
 */
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
                .padding(horizontal = if (maxWidth >= 600.dp) 24.dp else 16.dp)
                .verticalScroll(scrollState)
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        .size(36.dp)
                        .background(
                            colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.12f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun accountFieldColors(): TextFieldColors {
    val colors = MaterialTheme.appColors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        disabledTextColor = colors.textSecondary,
        focusedContainerColor = colors.background,
        unfocusedContainerColor = colors.background,
        disabledContainerColor = colors.background,
        cursorColor = colors.primary,
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.divider,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.textSecondary,
        focusedPlaceholderColor = colors.textTertiary,
        unfocusedPlaceholderColor = colors.textTertiary,
        focusedSupportingTextColor = colors.textTertiary,
        unfocusedSupportingTextColor = colors.textTertiary,
        focusedPrefixColor = colors.textSecondary,
        unfocusedPrefixColor = colors.textSecondary,
    )
}

@FormFactorPreviews
@Composable
private fun AccountEditorPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AccountEditorScreen(
                uiState = AccountEditorUiState(ready = true, name = "工资卡", type = AccountType.DebitCard),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
