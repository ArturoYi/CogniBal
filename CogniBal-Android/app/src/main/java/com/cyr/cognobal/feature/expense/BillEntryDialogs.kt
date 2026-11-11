package com.cyr.cognobal.screen.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cyr.cognobal.R
import com.cyr.cognobal.screen.feature.bill.model.accountIconImage
import com.cyr.cognobal.screen.feature.expense.state.AccountChoice
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.picker.CogniBalWheelDateTimePicker
import com.cyr.cognobal.ui.picker.WheelDateTime
import com.cyr.cognobal.ui.picker.WheelDateTimeMode
import com.cyr.cognobal.ui.theme.appColors
import com.cyr.cognobal.ui.theme.softBadgeTone
import java.time.LocalDate

@Composable
internal fun BillDateDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember {
        mutableStateOf(WheelDateTime.of(initial.year, initial.monthValue, initial.dayOfMonth))
    }
    WheelConfirmDialog(
        title = stringResource(R.string.bill_entry_date),
        onConfirm = { onConfirm(LocalDate.of(value.year, value.month, value.day)) },
        onDismiss = onDismiss,
    ) {
        CogniBalWheelDateTimePicker(
            value = value,
            onValueChange = { value = it },
            mode = WheelDateTimeMode.Date,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillTimeSheet(
    hour: Int,
    minute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var value by remember {
        mutableStateOf(WheelDateTime.of(2000, 1, 1, hour, minute))
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) },
        tonalElevation = 0.dp,
        contentWindowInsets = {
            WindowInsets.navigationBars.exclude(WindowInsets.ime).only(WindowInsetsSides.Bottom)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.bill_entry_time),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                CogniBalWheelDateTimePicker(
                    value = value,
                    onValueChange = { value = it },
                    mode = WheelDateTimeMode.Time,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                }
                TextButton(onClick = { onConfirm(value.hour, value.minute) }) {
                    Text(
                        text = stringResource(R.string.done),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    picker: @Composable () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.cardBackground) {
            Column(
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    picker()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                    }
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = stringResource(R.string.done),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountPickDialog(
    title: String,
    accounts: List<AccountChoice>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bill_entry_no_account),
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                accounts.forEach { account ->
                    val selected = account.id == selectedId
                    val tone = softBadgeTone(account.colorKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selected) colors.primaryContainer else colors.background)
                            .semantics { this.selected = selected }
                            .interactiveClick(role = Role.RadioButton, onClick = { onSelect(account.id) })
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(tone.container, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = accountIconImage(account.iconKey),
                                contentDescription = null,
                                tint = tone.content,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text = account.name,
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = MaterialTheme.shapes.extraLarge,
    )
}
