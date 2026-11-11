package com.cyr.cognobal.screen.feature.settings.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyr.cognobal.R
import com.cyr.cognobal.ui.interaction.interactiveClick
import com.cyr.cognobal.ui.theme.appColors

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
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(messageRes),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
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
        containerColor = colors.cardBackground,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
internal fun BackupImportSourceDialog(
    localEnabled: Boolean,
    localSubtitle: String,
    onRestoreLocal: () -> Unit,
    onChooseFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.backup_import_source_title),
                color = colors.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportSourceRow(
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.backup_import_from_local),
                    subtitle = localSubtitle,
                    enabled = localEnabled,
                    onClick = onRestoreLocal,
                )
                ImportSourceRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(R.string.backup_import_from_file),
                    subtitle = "",
                    enabled = true,
                    onClick = onChooseFile,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel), color = colors.textSecondary)
            }
        },
        containerColor = colors.cardBackground,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
private fun ImportSourceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.background)
            .interactiveClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) colors.primary else colors.textTertiary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title,
                color = if (enabled) colors.textPrimary else colors.textTertiary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
