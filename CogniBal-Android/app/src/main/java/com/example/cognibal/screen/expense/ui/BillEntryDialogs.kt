package com.example.cognibal.screen.expense.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cognibal.R
import com.example.cognibal.data.repository.LedgerLimits
import com.example.cognibal.screen.bill.model.AccountColor
import com.example.cognibal.screen.bill.model.accountColorValue
import com.example.cognibal.screen.bill.model.accountIconImage
import com.example.cognibal.screen.expense.model.AddBillIssue
import com.example.cognibal.screen.expense.model.categoryIcon
import com.example.cognibal.screen.expense.model.messageRes
import com.example.cognibal.screen.expense.model.newCategoryIconKeys
import com.example.cognibal.screen.expense.model.onCategory
import com.example.cognibal.screen.expense.model.toHexArgb
import com.example.cognibal.screen.expense.tool.pickerMillisToLocalDate
import com.example.cognibal.screen.expense.tool.toPickerMillis
import com.example.cognibal.screen.expense.viewmodel.AccountChoice
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillDateDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    onConfirm(pickerMillisToLocalDate(millis))
                },
            ) {
                Text(
                    text = stringResource(R.string.done),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = DatePickerDefaults.colors(containerColor = colors.cardBackground),
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillTimeDialog(
    hour: Int,
    minute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val state = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.cardBackground) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = state)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel), color = colors.textTertiary)
                    }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
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
            Text(text = title, color = colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bill_entry_no_account),
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                    )
                }
                accounts.forEach { account ->
                    val selected = account.id == selectedId
                    val tint = accountColorValue(account.colorKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { this.selected = selected }
                            .interactiveClick(role = Role.RadioButton, onClick = { onSelect(account.id) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(tint, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = accountIconImage(account.iconKey),
                                contentDescription = null,
                                tint = tint.onCategory(),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text = account.name,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(28.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CreateCategoryDialog(
    error: AddBillIssue?,
    onConfirm: (name: String, iconKey: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf(newCategoryIconKeys.first()) }
    var colorKey by remember { mutableStateOf(AccountColor.Clay.storageKey) }
    val colorHex = AccountColor.fromKey(colorKey)?.color?.toHexArgb()
        ?: AccountColor.Clay.color.toHexArgb()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.bill_entry_create_category),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(LedgerLimits.NAME) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.bill_entry_category_name)) },
                    placeholder = { Text(stringResource(R.string.bill_entry_category_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.textSecondary,
                        cursorColor = colors.primary,
                        focusedContainerColor = colors.background,
                        unfocusedContainerColor = colors.background,
                    ),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    newCategoryIconKeys.forEach { key ->
                        val selected = key == iconKey
                        val tint = accountColorValue(colorHex)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .semantics { this.selected = selected }
                                .background(
                                    if (selected) tint else colors.tagBackground,
                                    RoundedCornerShape(12.dp),
                                )
                                .interactiveClick(role = Role.RadioButton, onClick = { iconKey = key }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = categoryIcon(key),
                                contentDescription = null,
                                tint = if (selected) tint.onCategory() else colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AccountColor.entries.forEach { color ->
                        val selected = color.storageKey == colorKey
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { this.selected = selected }
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, colors.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .padding(4.dp)
                                .background(color.color, CircleShape)
                                .interactiveClick(role = Role.RadioButton, onClick = { colorKey = color.storageKey }),
                        )
                    }
                }
                if (error != null) {
                    Text(
                        text = stringResource(error.messageRes()),
                        color = colors.expense,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, iconKey, colorHex) }) {
                Text(
                    text = stringResource(R.string.done),
                    color = colors.primary,
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
        shape = RoundedCornerShape(28.dp),
    )
}
