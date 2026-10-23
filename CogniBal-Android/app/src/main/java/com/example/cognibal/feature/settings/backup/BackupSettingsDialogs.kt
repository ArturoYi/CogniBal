package com.example.cognibal.screen.feature.settings.backup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.cognibal.R
import com.example.cognibal.ui.theme.appColors

@Composable
internal fun BackupConfirmDialog(
    titleRes: Int,
    messageRes: Int,
    confirmRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(titleRes),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(messageRes),
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(confirmRes),
                    color = colors.expense,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textSecondary)
            }
        },
    )
}
