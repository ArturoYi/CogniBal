package com.example.cognibal.screen.feature.settings.budget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.data.database.model.BudgetPeriodType
import com.example.cognibal.data.repository.BudgetLevel
import com.example.cognibal.screen.feature.bill.BudgetStatusBanner
import com.example.cognibal.screen.feature.bill.tool.formatMinorAmount
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.budget.state.BudgetCategoryOption
import com.example.cognibal.screen.feature.settings.budget.state.BudgetEditorState
import com.example.cognibal.screen.feature.settings.budget.state.BudgetIssue
import com.example.cognibal.screen.feature.settings.budget.state.BudgetLineUi
import com.example.cognibal.screen.feature.settings.budget.state.BudgetUiEvent
import com.example.cognibal.screen.feature.settings.budget.state.BudgetUiState
import com.example.cognibal.screen.feature.settings.component.InsightCard
import com.example.cognibal.screen.feature.settings.component.InsightSegments
import com.example.cognibal.screen.feature.settings.component.formatYearMonth
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import com.example.cognibal.ui.theme.budgetNearColor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

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
        Text(
            text = stringResource(R.string.budget_scope_note),
            color = colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        val banner = budgetBanner(uiState.current.filter { it.enabled })
        if (banner != null) {
            BudgetStatusBanner(over = banner.over, text = banner.text)
        }
        InsightCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.bill_budget_title),
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (!uiState.loading && uiState.editor == null) {
                    Text(
                        text = stringResource(R.string.budget_add),
                        color = colors.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.interactiveClick { onEvent(BudgetUiEvent.Add) },
                    )
                }
            }
            if (uiState.current.isEmpty() && !uiState.loading) {
                Text(
                    text = stringResource(R.string.budget_empty),
                    color = colors.textTertiary,
                    fontSize = 14.sp,
                )
            }
            uiState.current.forEach { line ->
                BudgetLine(
                    line = line,
                    currencySymbol = uiState.currencySymbol,
                    onClick = { onEvent(BudgetUiEvent.Edit(line.id)) },
                )
            }
        }
        val editor = uiState.editor
        if (editor != null) {
            BudgetEditor(
                editor = editor,
                categories = uiState.categories,
                currencySymbol = uiState.currencySymbol,
                issue = uiState.issue,
                onEvent = onEvent,
                onDelete = { confirmDelete = true },
            )
        } else if (uiState.issue == BudgetIssue.NoScope) {
            Text(
                text = stringResource(R.string.budget_no_scope),
                color = colors.expense,
                fontSize = 13.sp,
            )
        }
        if (uiState.history.isNotEmpty()) {
            Text(
                text = stringResource(R.string.budget_history),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
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
        InsightCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.budget_notify),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.budget_notify_subtitle),
                        color = colors.textTertiary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Switch(
                    checked = uiState.notify && notificationsAllowed,
                    onCheckedChange = onNotifyChange,
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    text = stringResource(R.string.budget_delete_title),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.budget_delete_message),
                    color = colors.textSecondary,
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
            shape = RoundedCornerShape(20.dp),
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
    val title = budgetTitle(line.global, line.categoryName, line.missingCategory)
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
        BudgetLevel.Near -> budgetNearColor(colors.isDark)
        BudgetLevel.Normal -> colors.primary
    }
    val amountColor = if (line.level == BudgetLevel.Normal) colors.textSecondary else tint
    val remaining = line.limit - line.spent
    val status = if (remaining < 0) {
        stringResource(R.string.budget_over_amount, currencySymbol + formatMinorAmount(-remaining))
    } else {
        stringResource(R.string.budget_remaining, currencySymbol + formatMinorAmount(remaining))
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
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = if (line.enabled) colors.textPrimary else colors.textTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = currencySymbol + formatMinorAmount(line.spent) +
                    " / " + currencySymbol + formatMinorAmount(line.limit),
                color = amountColor,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = if (line.enabled) "$period · $status" else "$period · " + stringResource(R.string.budget_disabled),
            color = colors.textTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        LinearProgressIndicator(
            progress = { line.progress },
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp)),
            color = tint,
            trackColor = colors.tagBackground,
            drawStopIndicator = {},
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetEditor(
    editor: BudgetEditorState,
    categories: List<BudgetCategoryOption>,
    currencySymbol: String,
    issue: BudgetIssue?,
    onEvent: (BudgetUiEvent) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    InsightCard {
        Text(
            text = stringResource(if (editor.id == null) R.string.budget_add else R.string.budget_edit),
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        if (editor.id == null && editor.allowGlobal && editor.allowCategory) {
            InsightSegments(
                labels = listOf(
                    stringResource(R.string.bill_budget_global),
                    stringResource(R.string.budget_category),
                ),
                selectedIndex = if (editor.global) 0 else 1,
                onSelect = { index -> onEvent(BudgetUiEvent.ScopeGlobal(index == 0)) },
            )
        } else {
            val name = categories.firstOrNull { it.id == editor.categoryId }?.name
            Text(
                text = budgetTitle(editor.global, name, missing = !editor.global && name == null && editor.id != null),
                color = colors.textSecondary,
                fontSize = 14.sp,
            )
        }
        if (!editor.global && editor.id == null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        name = category.name,
                        selected = category.id == editor.categoryId,
                        onClick = { onEvent(BudgetUiEvent.CategorySelected(category.id)) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = editor.amountText,
            onValueChange = { onEvent(BudgetUiEvent.AmountChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !editor.saving,
            singleLine = true,
            prefix = { Text(text = currencySymbol, color = colors.textSecondary) },
            label = { Text(text = stringResource(R.string.budget_amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.divider,
                cursorColor = colors.primary,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.textTertiary,
                focusedContainerColor = colors.cardBackground,
                unfocusedContainerColor = colors.cardBackground,
            ),
        )
        if (editor.id == null || editor.periodType == BudgetPeriodType.Month) {
            Text(
                text = if (editor.cycleDay == 1) {
                    stringResource(R.string.budget_cycle_calendar)
                } else {
                    stringResource(R.string.budget_cycle_day, editor.cycleDay)
                },
                color = colors.textPrimary,
                fontSize = 14.sp,
            )
            Text(
                text = stringResource(R.string.budget_cycle_hint, editor.cycleDay),
                color = colors.textTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            if (editor.cycleDay > 28) {
                Text(
                    text = stringResource(R.string.budget_cycle_short_month, editor.cycleDay),
                    color = colors.textTertiary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Slider(
                value = editor.cycleDay.toFloat(),
                onValueChange = { value ->
                    onEvent(BudgetUiEvent.CycleDayChanged(value.roundToInt()))
                },
                enabled = !editor.saving,
                valueRange = 1f..31f,
                steps = 29,
            )
        }
        Text(
            text = stringResource(R.string.budget_alert) + " " +
                stringResource(R.string.budget_alert_value, editor.alertPercent),
            color = colors.textPrimary,
            fontSize = 14.sp,
        )
        Slider(
            value = editor.alertPercent.toFloat(),
            onValueChange = { value ->
                onEvent(BudgetUiEvent.AlertChanged(value.roundToInt()))
            },
            enabled = !editor.saving,
            valueRange = BudgetMathMin..BudgetMathMax,
            steps = 49,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.budget_enabled),
                color = colors.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = editor.enabled,
                onCheckedChange = { onEvent(BudgetUiEvent.EnabledChanged(it)) },
                enabled = !editor.saving,
            )
        }
        if (issue != null && issue != BudgetIssue.NoScope) {
            Text(
                text = stringResource(issue.messageRes()),
                color = colors.expense,
                fontSize = 13.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editor.id != null) {
                TextButton(onClick = onDelete, enabled = !editor.saving) {
                    Text(text = stringResource(R.string.budget_delete), color = colors.expense)
                }
            }
            TextButton(onClick = { onEvent(BudgetUiEvent.DismissEditor) }, enabled = !editor.saving) {
                Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
            }
            TextButton(onClick = { onEvent(BudgetUiEvent.Save) }, enabled = !editor.saving) {
                Text(
                    text = stringResource(R.string.bill_entry_save),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Text(
        text = name,
        color = if (selected) colors.onPrimary else colors.textPrimary,
        fontSize = 13.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) colors.primary else colors.tagBackground)
            .semantics { this.selected = selected }
            .interactiveClick(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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

@Composable
private fun budgetTitle(global: Boolean, categoryName: String?, missing: Boolean): String = when {
    global -> stringResource(R.string.bill_budget_global)
    missing || categoryName == null -> stringResource(R.string.bill_budget_missing)
    else -> categoryName
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
