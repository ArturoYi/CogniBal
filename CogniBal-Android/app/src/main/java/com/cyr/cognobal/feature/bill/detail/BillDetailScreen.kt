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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyr.cognobal.R
import com.cyr.cognobal.data.database.model.BillType
import com.cyr.cognobal.screen.feature.bill.model.accountColorValue
import com.cyr.cognobal.screen.feature.bill.tool.formatMoney
import com.cyr.cognobal.screen.feature.expense.model.categoryIcon
import com.cyr.cognobal.screen.feature.settings.SettingsSectionCard
import com.cyr.cognobal.screen.feature.settings.SettingsSubpage
import com.cyr.cognobal.ui.format.formatBillTime
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.asThemeIdentity
import java.util.Locale

/** 账单详情。编辑进入记一笔，删除前先确认。 */
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AmountCard(uiState = uiState, locale = locale)
            FactsCard(uiState = uiState, locale = locale)
            if (uiState.brokenTransfer) {
                Text(
                    text = stringResource(R.string.bill_detail_broken),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            if (uiState.failed) {
                Text(
                    text = stringResource(R.string.bill_detail_failed),
                    color = colors.expense,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
        DetailButton(
            text = stringResource(R.string.bill_detail_edit),
            container = colors.primary,
            content = colors.onPrimary,
            enabled = !uiState.deleting && !uiState.brokenTransfer,
            onClick = onEdit,
        )
        DetailButton(
            text = stringResource(R.string.bill_detail_delete),
            container = colors.expense.copy(alpha = if (colors.isDark) 0.22f else 0.12f),
            content = colors.expense,
            enabled = !uiState.deleting,
            onClick = { onEvent(BillDetailUiEvent.RequestDelete) },
        )
    }
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onEvent(BillDetailUiEvent.DismissDelete) },
            title = {
                Text(
                    text = stringResource(R.string.bill_detail_delete_title),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.bill_detail_delete_message),
                    color = colors.textSecondary,
                    lineHeight = 20.sp,
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
            shape = RoundedCornerShape(24.dp),
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
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    } else {
        val description = stringResource(R.string.loading_indicator_cd)
        Text(
            text = "—",
            color = colors.textTertiary,
            fontSize = 16.sp,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        )
    }
}

@Composable
private fun AmountCard(uiState: BillDetailUiState, locale: Locale) {
    val colors = MaterialTheme.appColors
    val title = when (uiState.type) {
        BillType.Transfer -> stringResource(R.string.bill_transfer)
        else -> uiState.categoryName ?: stringResource(R.string.bill_category_missing)
    }
    val typeLabel = stringResource(
        when (uiState.type) {
            BillType.Expense -> R.string.bill_fab_expense
            BillType.Income -> R.string.bill_fab_income
            BillType.Transfer -> R.string.bill_fab_transfer
        },
    )
    Surface(
        color = colors.cardBackground,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DetailMark(uiState)
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = typeLabel,
                color = colors.textTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = formatMoney(uiState.currencySymbol, uiState.signedAmount, locale),
                color = if (uiState.type == BillType.Income) colors.income else colors.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 20.sp,
                    maxFontSize = 32.sp,
                    stepSize = 1.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailMark(uiState: BillDetailUiState) {
    val colors = MaterialTheme.appColors
    val tint = when {
        uiState.type == BillType.Transfer -> colors.textSecondary
        uiState.categoryColorKey != null -> accountColorValue(uiState.categoryColorKey).asThemeIdentity()
        uiState.type == BillType.Income -> colors.income
        else -> colors.textSecondary
    }
    val icon = when (uiState.type) {
        BillType.Transfer -> Icons.Outlined.SwapHoriz
        else -> categoryIcon(uiState.categoryIconKey.orEmpty())
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = if (colors.isDark) 0.22f else 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun FactsCard(uiState: BillDetailUiState, locale: Locale) {
    SettingsSectionCard {
        if (uiState.type == BillType.Transfer) {
            FactRow(stringResource(R.string.bill_detail_from), uiState.accountName)
            FactRow(stringResource(R.string.bill_detail_to), uiState.counterpartyName.orEmpty())
        } else {
            FactRow(stringResource(R.string.bill_entry_account), uiState.accountName)
            FactRow(
                stringResource(R.string.bill_detail_category),
                uiState.categoryName ?: stringResource(R.string.bill_category_missing),
            )
        }
        val time = uiState.time
        if (time != null) {
            FactRow(stringResource(R.string.bill_entry_when), formatBillTime(time, locale))
        }
        if (uiState.remark.isNotBlank()) {
            FactRow(stringResource(R.string.account_field_remark), uiState.remark)
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textTertiary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp),
        )
        Text(
            text = value.ifBlank { stringResource(R.string.bill_detail_remark_empty) },
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
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
) {
    val colors = MaterialTheme.appColors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = colors.tagBackground,
            disabledContentColor = colors.textTertiary,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
