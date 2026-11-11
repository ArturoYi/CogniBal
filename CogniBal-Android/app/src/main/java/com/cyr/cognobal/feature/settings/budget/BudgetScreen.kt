package com.cyr.cognobal.screen.feature.settings.budget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.BudgetPeriodType
import com.cyr.cognobal.data.repository.BudgetLevel
import com.cyr.cognobal.screen.feature.bill.BudgetStatusBanner
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.settings.SettingsSectionCard
import com.cyr.cognobal.screen.feature.settings.SettingsSubpage
import com.cyr.cognobal.screen.feature.settings.budget.state.BudgetEditorState
import com.cyr.cognobal.screen.feature.settings.budget.state.BudgetIssue
import com.cyr.cognobal.screen.feature.settings.budget.state.BudgetLineUi
import com.cyr.cognobal.screen.feature.settings.budget.state.BudgetUiEvent
import com.cyr.cognobal.screen.feature.settings.budget.state.BudgetUiState
import com.cyr.cognobal.screen.feature.settings.component.InsightCard
import com.cyr.cognobal.screen.feature.settings.component.formatYearMonth
import com.cyr.cognobal.ui.background.PageBackgroundHost
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.preview.FormFactorPreviews
import com.cyr.cognobal.ui.theme.AppThemeStyle
import com.cyr.cognobal.ui.theme.CogniBalTheme
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.budgetNearColor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun BudgetRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(BudgetUiEvent.NotifyChanged(granted))
    }
    BudgetScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        notificationsAllowed = notificationsAllowed(context),
        onNotifyChange = { enabled ->
            if (!enabled) {
                viewModel.onEvent(BudgetUiEvent.NotifyChanged(false))
            } else if (needsNotificationPermission(context)) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onEvent(BudgetUiEvent.NotifyChanged(true))
            }
        },
        modifier = modifier,
    )
}

/**
 * 预算设置：移除顶部说明句、添加提示语与开关下的重复解释，只保留进度数据卡、历史周期与干净的双开关组。
 */
@Composable
internal fun BudgetScreen(
    uiState: BudgetUiState,
    onEvent: (BudgetUiEvent) -> Unit,
    onBack: () -> Unit,
    notificationsAllowed: Boolean,
    onNotifyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.editor) {
        if (uiState.editor == null) confirmDelete = false
    }
    SettingsSubpage(
        title = stringResource(R.string.shortcut_budget),
        onBack = onBack,
        modifier = modifier,
    ) {
        val budget = uiState.current.firstOrNull()
        val banner = budgetBanner(listOfNotNull(budget).filter { it.enabled })
        if (banner != null) {
            BudgetStatusBanner(over = banner.over, text = banner.text)
        }
        InsightCard {
            if (uiState.loading && budget == null) {
                Text(
                    text = "—",
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (budget == null) {
                Text(
                    text = stringResource(R.string.budget_empty),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = { onEvent(BudgetUiEvent.Add) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.inkStrong,
                        contentColor = colors.onInkStrong,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.budget_add),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.bill_budget_global),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.budget_edit),
                        color = colors.primary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.interactiveClick {
                            onEvent(BudgetUiEvent.Edit(budget.id))
                        },
                    )
                }
                BudgetLine(
                    line = budget,
                    currencySymbol = uiState.currencySymbol,
                    onClick = { onEvent(BudgetUiEvent.Edit(budget.id)) },
                )
            }
        }
        if (uiState.history.isNotEmpty()) {
            Text(
                text = stringResource(R.string.budget_history),
                color = colors.textSecondary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            InsightCard {
                uiState.history.forEach { line ->
                    BudgetLine(
                        line = line,
                        currencySymbol = uiState.currencySymbol,
                        onClick = null,
                    )
                }
            }
        }
        SettingsSectionCard {
            BudgetToggleRow(
                title = stringResource(R.string.budget_home_visible),
                checked = uiState.homeVisible,
                onCheckedChange = { visible ->
                    onEvent(BudgetUiEvent.HomeVisibleChanged(visible))
                },
            )
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
            BudgetToggleRow(
                title = stringResource(R.string.budget_notify),
                checked = uiState.notify && notificationsAllowed,
                onCheckedChange = onNotifyChange,
            )
        }
    }
    val editor = uiState.editor
    if (editor != null) {
        if (editor.id == null) {
            AddBudgetSheet(
                editor = editor,
                currencySymbol = uiState.currencySymbol,
                issue = uiState.issue,
                onEvent = onEvent,
            )
        } else {
            EditBudgetSheet(
                editor = editor,
                currencySymbol = uiState.currencySymbol,
                issue = uiState.issue,
                onEvent = onEvent,
                onDelete = { confirmDelete = true },
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    text = stringResource(R.string.budget_delete_title),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.budget_delete_message),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onEvent(BudgetUiEvent.Delete)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.budget_delete),
                        color = colors.expense,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                }
            },
            containerColor = colors.cardBackground,
            shape = MaterialTheme.shapes.large,
        )
    }
}

@Composable
private fun BudgetToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (focused) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                },
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .clearAndSetSemantics {},
        )
    }
}

@Composable
private fun BudgetLine(
    line: BudgetLineUi,
    currencySymbol: String,
    onClick: (() -> Unit)?,
) {
    val colors = MaterialTheme.appColors
    val monthLabel = if (line.periodType == BudgetPeriodType.Month) {
        formatYearMonth(YearMonth.from(line.periodStart))
    } else {
        line.periodStart.toString()
    }
    val chosenDay = line.cycleDay ?: line.periodStart.dayOfMonth
    val period = if (
        line.periodType == BudgetPeriodType.Month &&
        (chosenDay != 1 || line.periodStart.dayOfMonth != 1)
    ) {
        stringResource(R.string.budget_period_custom, monthLabel, chosenDay)
    } else {
        monthLabel
    }
    val tint = when (line.level) {
        BudgetLevel.Over -> colors.expense
        BudgetLevel.Near -> budgetNearColor(colors)
        BudgetLevel.Normal -> colors.primary
    }
    val remaining = line.limit - line.spent
    val status = if (remaining < 0) {
        stringResource(R.string.budget_over_amount, formatMoney(currencySymbol, -remaining))
    } else {
        stringResource(R.string.budget_remaining, formatMoney(currencySymbol, remaining))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.interactiveClick(enabled = line.editable, onClick = onClick)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (line.enabled) "$period · $status" else "$period · " + stringResource(R.string.budget_disabled),
                color = if (line.enabled) colors.textSecondary else colors.textTertiary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatMoney(currencySymbol, line.spent) +
                    " / " + formatMoney(currencySymbol, line.limit),
                color = if (line.level == BudgetLevel.Over) colors.expense else colors.textPrimary,
                style = MaterialTheme.typography.labelLarge.merge(
                    TextStyle(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                ),
                maxLines = 1,
            )
        }
        LinearProgressIndicator(
            progress = { line.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = tint,
            trackColor = colors.tagBackground,
            drawStopIndicator = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetSheet(
    editor: BudgetEditorState,
    currencySymbol: String,
    issue: BudgetIssue?,
    onEvent: (BudgetUiEvent) -> Unit,
) {
    BudgetFormSheet(
        title = stringResource(R.string.budget_add),
        subtitle = null,
        editor = editor,
        currencySymbol = currencySymbol,
        issue = issue,
        onEvent = onEvent,
        onDelete = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetSheet(
    editor: BudgetEditorState,
    currencySymbol: String,
    issue: BudgetIssue?,
    onEvent: (BudgetUiEvent) -> Unit,
    onDelete: () -> Unit,
) {
    val month = editor.periodStart?.let { formatYearMonth(YearMonth.from(it.toLocalDate())) }
    BudgetFormSheet(
        title = stringResource(R.string.budget_edit),
        subtitle = month,
        editor = editor,
        currencySymbol = currencySymbol,
        issue = issue,
        onEvent = onEvent,
        onDelete = onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormSheet(
    title: String,
    subtitle: String?,
    editor: BudgetEditorState,
    currencySymbol: String,
    issue: BudgetIssue?,
    onEvent: (BudgetUiEvent) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        if (editor.saving) return
        scope.launch {
            sheetState.hide()
            onEvent(BudgetUiEvent.DismissEditor)
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (!editor.saving) onEvent(BudgetUiEvent.DismissEditor) },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            BudgetAmountField(
                amountText = editor.amountText,
                currencySymbol = currencySymbol,
                enabled = !editor.saving,
                onAmount = { onEvent(BudgetUiEvent.AmountChanged(it)) },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.budget_alert),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(R.string.budget_alert_value, editor.alertPercent),
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.labelLarge.merge(
                            TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        ),
                    )
                }
                Slider(
                    value = editor.alertPercent.toFloat(),
                    onValueChange = { value ->
                        onEvent(BudgetUiEvent.AlertChanged(value.roundToInt()))
                    },
                    enabled = !editor.saving,
                    valueRange = BudgetMathMin..BudgetMathMax,
                    steps = 49,
                )
            }
            if (issue != null && issue != BudgetIssue.NoScope) {
                Text(
                    text = stringResource(issue.messageRes()),
                    color = colors.expense,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(
                    onClick = ::close,
                    enabled = !editor.saving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.tagBackground,
                        contentColor = colors.textPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    )
                }
                Button(
                    onClick = { onEvent(BudgetUiEvent.Save) },
                    enabled = !editor.saving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.inkStrong,
                        contentColor = colors.onInkStrong,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.bill_entry_save),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    enabled = !editor.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.budget_delete),
                        color = colors.expense,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetAmountField(
    amountText: String,
    currencySymbol: String,
    enabled: Boolean,
    onAmount: (String) -> Unit,
) {
    val colors = MaterialTheme.appColors
    var focused by remember { mutableStateOf(false) }
    val label = stringResource(R.string.budget_amount)
    BasicTextField(
        value = amountText,
        onValueChange = onAmount,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.displaySmall.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .then(
                if (focused) {
                    Modifier.border(
                        1.dp,
                        colors.primary.copy(alpha = 0.35f),
                        MaterialTheme.shapes.medium,
                    )
                } else {
                    Modifier
                },
            )
            .background(colors.background, MaterialTheme.shapes.medium)
            .semantics { contentDescription = label }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = currencySymbol,
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (amountText.isEmpty()) {
                        Text(
                            text = "0.00",
                            color = colors.textTertiary,
                            style = MaterialTheme.typography.displaySmall,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

private data class BannerText(val over: Boolean, val text: String)

@Composable
private fun budgetBanner(lines: List<BudgetLineUi>): BannerText? {
    val concerning = lines.filter { it.level != BudgetLevel.Normal }
    if (concerning.isEmpty()) return null
    val over = concerning.any { it.level == BudgetLevel.Over }
    val worst = if (over) BudgetLevel.Over else BudgetLevel.Near
    val separator = stringResource(R.string.budget_name_separator)
    val global = stringResource(R.string.bill_budget_global)
    val missing = stringResource(R.string.bill_budget_missing)
    val names = concerning.filter { it.level == worst }.joinToString(separator) { line ->
        when {
            line.global -> global
            line.missingCategory || line.categoryName == null -> missing
            else -> line.categoryName
        }
    }
    return BannerText(
        over = over,
        text = stringResource(if (over) R.string.budget_banner_over else R.string.budget_banner_near, names),
    )
}

private fun BudgetIssue.messageRes(): Int = when (this) {
    BudgetIssue.InvalidAmount -> R.string.budget_invalid_amount
    BudgetIssue.Overlap -> R.string.budget_overlap
    BudgetIssue.NeedCategory -> R.string.budget_need_category
    BudgetIssue.NoScope -> R.string.budget_no_scope
    BudgetIssue.Failed -> R.string.budget_failed
}

private const val BudgetMathMin = 50f
private const val BudgetMathMax = 100f

private fun needsNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
}

private fun notificationsAllowed(context: Context): Boolean {
    if (needsNotificationPermission(context)) return false
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@FormFactorPreviews
@Composable
private fun BudgetScreenPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            BudgetScreen(
                uiState = BudgetUiState(
                    loading = false,
                    current = listOf(
                        BudgetLineUi(
                            id = 1L,
                            global = true,
                            categoryName = null,
                            missingCategory = false,
                            spent = 246_000L,
                            limit = 300_000L,
                            progress = 0.82f,
                            level = BudgetLevel.Near,
                            alertPercent = 80,
                            enabled = true,
                            editable = true,
                            periodStart = LocalDate.of(2026, 10, 1),
                            periodType = BudgetPeriodType.Month,
                        ),
                    ),
                ),
                onEvent = {},
                onBack = {},
                notificationsAllowed = true,
                onNotifyChange = {},
            )
        }
    }
}
