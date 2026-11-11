package com.cyr.cognobal.screen.feature.bill.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.expense.model.categoryIcon
import com.cyr.cognobal.screen.feature.settings.SettingsSectionCard
import com.cyr.cognobal.screen.feature.settings.SettingsSubpage
import com.cyr.cognobal.ui.format.formatBillTime
import com.cyr.cognobal.ui.theme.appColors
import java.util.Locale

@Composable
internal fun BillDetailRoute(
    billId: Long,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillDetailViewModel = viewModel(factory = BillDetailViewModel.factory(billId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.finished.collect { onBack() }
    }
    BillDetailScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEdit = { onEdit(uiState.type.code) },
        modifier = modifier,
    )
}

@Composable
internal fun BillDetailScreen(
    uiState: BillDetailUiState,
    onEvent: (BillDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val settled = !uiState.loading && !uiState.missing
    SettingsSubpage(
        title = stringResource(R.string.bill_detail_title),
        onBack = onBack,
        modifier = modifier,
        scrollContent = false,
        placeholder = if (settled) {
            null
        } else {
            { DetailPlaceholder(missing = uiState.missing) }
        },
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AmountStage(uiState = uiState, locale = locale)
            FactsCard(uiState = uiState, locale = locale)
            if (uiState.brokenTransfer) {
                Text(
                    text = stringResource(R.string.bill_detail_broken),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (uiState.failed) {
                Text(
                    text = stringResource(R.string.bill_detail_failed),
                    color = colors.expense,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DetailButton(
                text = stringResource(R.string.bill_detail_delete),
                container = colors.cardBackground,
                content = colors.expense,
                enabled = !uiState.deleting,
                onClick = { onEvent(BillDetailUiEvent.RequestDelete) },
                modifier = Modifier.weight(1f),
            )
            DetailButton(
                text = stringResource(R.string.bill_detail_edit),
                container = colors.inkStrong,
                content = colors.onInkStrong,
                enabled = !uiState.deleting && !uiState.brokenTransfer,
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onEvent(BillDetailUiEvent.DismissDelete) },
            title = {
                Text(
                    text = stringResource(R.string.bill_detail_delete_title),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.bill_detail_delete_message),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(BillDetailUiEvent.ConfirmDelete) }) {
                    Text(
                        text = stringResource(R.string.bill_detail_delete_confirm),
                        color = colors.expense,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(BillDetailUiEvent.DismissDelete) }) {
                    Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                }
            },
            containerColor = colors.cardBackground,
            shape = MaterialTheme.shapes.extraLarge,
        )
    }
}

@Composable
private fun DetailPlaceholder(missing: Boolean) {
    val colors = MaterialTheme.appColors
    if (missing) {
        Text(
            text = stringResource(R.string.bill_detail_missing),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    } else {
        val description = stringResource(R.string.loading_indicator_cd)
        Text(
            text = "—",
            color = colors.textTertiary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        )
    }
}

@Composable
private fun AmountStage(uiState: BillDetailUiState, locale: Locale) {
    val colors = MaterialTheme.appColors
    val title = when (uiState.type) {
        BillType.Transfer -> stringResource(R.string.bill_transfer)
        else -> uiState.categoryName ?: stringResource(R.string.bill_category_missing)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DetailMark(uiState)
        Text(
            text = title,
            color = colors.textSecondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = formatMoney(uiState.currencySymbol, uiState.signedAmount, locale),
            color = if (uiState.type == BillType.Income) colors.income else colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 26.sp,
                maxFontSize = 44.sp,
                stepSize = 1.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun DetailMark(uiState: BillDetailUiState) {
    val icon = when (uiState.type) {
        BillType.Transfer -> Icons.Outlined.SwapHoriz
        else -> categoryIcon(uiState.categoryIconKey.orEmpty())
    }
    val tone = com.cyr.cognobal.ui.theme.softBadgeTone(uiState.categoryColorKey.orEmpty())
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tone.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone.content,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun FactsCard(uiState: BillDetailUiState, locale: Locale) {
    val remark = uiState.remark.trim()
    SettingsSectionCard {
        if (uiState.type == BillType.Transfer) {
            FactRow(stringResource(R.string.bill_detail_from), uiState.accountName)
            FactRow(stringResource(R.string.bill_detail_to), uiState.counterpartyName.orEmpty())
        } else {
            FactRow(stringResource(R.string.bill_entry_account), uiState.accountName)
        }
        val time = uiState.time
        if (time != null) {
            FactRow(stringResource(R.string.bill_entry_when), formatBillTime(time, locale))
        }
        if (remark.isNotEmpty()) {
            FactRow(stringResource(R.string.bill_detail_remark), remark)
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textTertiary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp),
        )
        Text(
            text = value,
            color = colors.textPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
    }
}

@Composable
private fun DetailButton(
    text: String,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = colors.cardBackground,
            disabledContentColor = colors.textTertiary,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}
