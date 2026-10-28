package com.example.cognibal.screen.feature.bill.account

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.AccountType
import com.example.cognibal.data.repository.LedgerReject
import com.example.cognibal.screen.feature.bill.AccountHeroSurface
import com.example.cognibal.screen.feature.bill.model.AccountColor
import com.example.cognibal.screen.feature.bill.model.accountColorValue
import com.example.cognibal.screen.feature.bill.model.pairedIcon
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.bill.tool.parseMajorAmount
import com.example.cognibal.screen.feature.expense.model.toHexArgb
import com.example.cognibal.screen.feature.bill.account.state.AccountDeleteBlock
import com.example.cognibal.screen.feature.bill.account.state.AccountEditorUiEvent
import com.example.cognibal.screen.feature.bill.account.state.AccountEditorUiState
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.background.RequestPageBackground
import com.example.cognibal.ui.insets.listContentPadding
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.CogniBalCardElevation
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
    val accent = accountColorValue(uiState.previewColorKey())
    AccountPreviewCard(uiState = uiState)
    FormCard {
        SoftField(
            value = uiState.name,
            onValueChange = { onEvent(AccountEditorUiEvent.NameChanged(it)) },
            label = stringResource(R.string.account_field_name),
            placeholder = stringResource(R.string.account_field_name_hint),
            enabled = !uiState.saving,
        )
        FieldLabel(stringResource(R.string.account_field_type))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            AccountType.entries.forEach { type ->
                TypeChoice(
                    type = type,
                    selected = uiState.type == type,
                    accent = accent,
                    enabled = !uiState.saving,
                    onClick = { onEvent(AccountEditorUiEvent.TypeSelected(type)) },
                    modifier = Modifier.width(52.dp),
                )
            }
        }
    }
    ColorSection(
        uiState = uiState,
        enabled = !uiState.saving,
        onEvent = onEvent,
    )
    FormCard {
        if (uiState.isEdit) {
            FieldLabel(stringResource(R.string.account_field_balance))
            val balance = uiState.currentBalance ?: 0L
            Text(
                text = uiState.currencySymbol + formatMinorAmount(balance),
                color = colors.textPrimary,
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
            SoftField(
                value = uiState.openingBalanceText,
                onValueChange = { onEvent(AccountEditorUiEvent.OpeningBalanceChanged(it)) },
                label = stringResource(R.string.account_field_opening),
                placeholder = "0",
                prefix = uiState.currencySymbol,
                supporting = stringResource(
                    if (uiState.type == AccountType.CreditCard) {
                        R.string.account_field_opening_credit
                    } else {
                        R.string.account_field_opening_hint
                    },
                ),
                enabled = !uiState.saving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        SoftField(
            value = uiState.remark,
            onValueChange = { onEvent(AccountEditorUiEvent.RemarkChanged(it)) },
            label = stringResource(R.string.account_field_remark),
            placeholder = stringResource(R.string.account_field_remark_hint),
            enabled = !uiState.saving,
            singleLine = false,
            minLines = 2,
        )
    }
    if (uiState.isEdit) {
        AccountAvailability(uiState = uiState, onEvent = onEvent)
    }
    Column {
        FormMessage(error = uiState.error, color = colors.expense)
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

/**
 * 和首页结余卡同一张底。左上是金额含义，右上是名字，大字是金额，右下角是类型图标。
 */
@Composable
private fun AccountPreviewCard(uiState: AccountEditorUiState) {
    val unnamed = uiState.name.isBlank()
    val name = if (unnamed) {
        stringResource(R.string.account_field_name_hint)
    } else {
        uiState.name
    }
    val amount = if (uiState.isEdit) {
        uiState.currencySymbol + formatMinorAmount(uiState.currentBalance ?: 0L)
    } else {
        openingPreviewAmount(uiState)
    }
    AccountHeroSurface(color = accountColorValue(uiState.previewColorKey())) { paint ->
        val muted = paint.on.copy(alpha = 0.78f)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isEdit) {
                            R.string.account_field_balance
                        } else {
                            R.string.account_field_opening
                        },
                    ),
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = name,
                    color = paint.on.copy(alpha = if (unnamed) 0.62f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.widthIn(max = 132.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = amount,
                    color = paint.on,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.displaySmall.merge(
                        TextStyle(fontFeatureSettings = "tnum"),
                    ),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 22.sp,
                        maxFontSize = 34.sp,
                        stepSize = 1.sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = uiState.type.pairedIcon().image,
                    contentDescription = null,
                    tint = paint.on.copy(alpha = 0.28f),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp),
                )
            }
            Text(
                text = stringResource(uiState.type.labelRes()),
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun openingPreviewAmount(uiState: AccountEditorUiState): String {
    val parsed = parseMajorAmount(uiState.openingBalanceText)
    val body = if (parsed != null) {
        formatMinorAmount(parsed)
    } else {
        uiState.openingBalanceText.trim()
    }
    return uiState.currencySymbol + body
}

private fun AccountEditorUiState.previewColorKey(): String = colorKeyOverride ?: color.storageKey

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.appColors.cardBackground,
        shadowElevation = CogniBalCardElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
private fun TypeChoice(
    type: AccountType,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val label = stringResource(type.labelRes())
    Column(
        modifier = modifier
            .semantics { this.selected = selected }
            .interactiveClick(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (selected) accent.copy(alpha = 0.16f) else colors.tagBackground,
                    CircleShape,
                )
                .then(if (selected) Modifier.border(1.5.dp, accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = type.pairedIcon().image,
                contentDescription = null,
                tint = if (selected) accent else colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 错误占固定高度。出现或消失时，保存按钮和上面的卡片都不再被撑高或压矮。 */
@Composable
private fun FormMessage(
    error: LedgerReject?,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (error != null) {
            Text(
                text = stringResource(error.accountMessageRes()),
                color = color,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 名称固定在输入区上方，不再浮到描边缺口里。描边输入框抬标题时会垫一块纯色，这里不用那层。
 */
@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    supporting: String? = null,
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
    val labelColor by animateColorAsState(
        if (focused) colors.primary else colors.textSecondary,
        label = "soft-field-label",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(
                color = if (enabled) colors.textPrimary else colors.textSecondary,
                fontSize = 16.sp,
            ),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged { focused = it.isFocused }
                .then(
                    if (focused) {
                        Modifier.border(1.dp, colors.primary.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    },
                )
                .background(
                    if (focused) colors.primary.copy(alpha = 0.06f) else colors.tagBackground,
                    RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                    if (prefix != null) {
                        Text(
                            text = prefix,
                            color = colors.textSecondary,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = colors.textTertiary,
                                fontSize = 16.sp,
                            )
                        }
                        inner()
                    }
                }
            },
        )
        if (supporting != null) {
            Text(
                text = supporting,
                color = colors.textTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ColorSection(
    uiState: AccountEditorUiState,
    enabled: Boolean,
    onEvent: (AccountEditorUiEvent) -> Unit,
) {
    val current = accountColorValue(uiState.previewColorKey())
    val start = remember { current.toHsv() }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var writingBoard by remember { mutableStateOf(false) }
    LaunchedEffect(current.toArgb()) {
        if (writingBoard) {
            writingBoard = false
            return@LaunchedEffect
        }
        val hsv = current.toHsv()
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    FormCard {
        FieldLabel(stringResource(R.string.account_field_color))
        ColorPad(
            hue = hue,
            saturation = saturation,
            value = value,
            display = current,
            enabled = enabled,
            onChange = { nextHue, nextSaturation, nextValue ->
                hue = nextHue
                saturation = nextSaturation
                value = nextValue
                val picked = hsv(nextHue, nextSaturation, nextValue)
                if (picked.toArgb() != current.toArgb()) {
                    writingBoard = true
                    onEvent(colorEvent(picked))
                }
            },
        )
    }
}

/** 饱和度在横轴，明度在纵轴。点或滑都会落到手指下的颜色。 */
@Composable
private fun ColorPad(
    hue: Float,
    saturation: Float,
    value: Float,
    display: Color,
    enabled: Boolean,
    onChange: (Float, Float, Float) -> Unit,
) {
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var hueSize by remember { mutableStateOf(IntSize.Zero) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .onSizeChanged { padSize = it }
                .dragColor(enabled) { position ->
                    if (padSize.width == 0 || padSize.height == 0) return@dragColor
                    onChange(
                        hue,
                        (position.x / padSize.width).coerceIn(0f, 1f),
                        (1f - position.y / padSize.height).coerceIn(0f, 1f),
                    )
                },
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                drawRect(hsv(hue, 1f, 1f))
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            }
            if (padSize != IntSize.Zero) {
                ColorThumb(
                    color = hsv(hue, saturation, value),
                    modifier = Modifier.offset {
                        val diameter = 22.dp.roundToPx()
                        IntOffset(
                            (saturation * padSize.width - diameter / 2f).roundToInt(),
                            ((1f - value) * padSize.height - diameter / 2f).roundToInt(),
                        )
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { hueSize = it }
                .dragColor(enabled) { position ->
                    if (hueSize.width == 0) return@dragColor
                    onChange(
                        (position.x / hueSize.width).coerceIn(0f, 1f) * 360f,
                        saturation,
                        value,
                    )
                },
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
            ) {
                drawRect(Brush.horizontalGradient(HueGradient))
            }
            if (hueSize != IntSize.Zero) {
                ColorThumb(
                    color = hsv(hue, 1f, 1f),
                    modifier = Modifier.offset {
                        val diameter = 18.dp.roundToPx()
                        IntOffset(
                            (hue / 360f * hueSize.width - diameter / 2f).roundToInt(),
                            (hueSize.height - diameter) / 2,
                        )
                    },
                    diameter = 18.dp,
                )
            }
        }
        ColorReadout(display)
    }
}

@Composable
private fun ColorThumb(
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(diameter)
            .border(2.dp, Color.White, CircleShape)
            .padding(2.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun ColorReadout(color: Color) {
    val argb = color.toArgb()
    val red = (argb shr 16) and 0xFF
    val green = (argb shr 8) and 0xFF
    val blue = argb and 0xFF
    val hex = String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
    val colors = MaterialTheme.appColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = "$hex  R $red  G $green  B $blue"
        },
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color, CircleShape),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = hex,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            Text(
                text = "R $red    G $green    B $blue",
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

private fun Modifier.dragColor(
    enabled: Boolean,
    onPosition: (Offset) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            down.consume()
            onPosition(down.position)
            drag(down.id) { change ->
                onPosition(change.position)
                change.consume()
            }
        }
    }
}

private fun colorEvent(color: Color): AccountEditorUiEvent {
    val preset = AccountColor.entries.firstOrNull { it.color.toArgb() == color.toArgb() }
    return if (preset != null) {
        AccountEditorUiEvent.ColorSelected(preset)
    } else {
        AccountEditorUiEvent.CustomColorSelected(color.toHexArgb())
    }
}

private fun hsv(hue: Float, saturation: Float, value: Float): Color {
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv
}

private val HueGradient = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hsv(it, 1f, 1f) }

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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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

@FormFactorPreviews
@Composable
private fun AccountEditorPreview() {
    CogniBalTheme {
        PageBackgroundHost {
            AccountEditorScreen(
                uiState = AccountEditorUiState(
                ready = true,
                name = "工资卡",
                type = AccountType.DebitCard,
                color = AccountColor.Blue,
                openingBalanceText = "1280",
                currencySymbol = "¥",
            ),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
