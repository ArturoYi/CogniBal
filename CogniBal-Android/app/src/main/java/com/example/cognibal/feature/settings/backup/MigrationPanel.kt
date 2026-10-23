package com.example.cognibal.screen.feature.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiEvent
import com.example.cognibal.screen.feature.settings.backup.state.MigrationUi
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.theme.appColors
import kotlin.math.roundToInt

@Composable
internal fun MigrationPanel(
    migration: MigrationUi,
    onEvent: (BackupSettingsUiEvent) -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (migration) {
            MigrationUi.Role -> {
                Text(
                    text = stringResource(R.string.backup_migrate_file_hint),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
                RoleRow(
                    title = stringResource(R.string.backup_migrate_send),
                    body = stringResource(R.string.backup_migrate_send_body),
                    onClick = { onEvent(BackupSettingsUiEvent.ChooseSend) },
                )
                RoleRow(
                    title = stringResource(R.string.backup_migrate_receive),
                    body = stringResource(R.string.backup_migrate_receive_body),
                    onClick = { onEvent(BackupSettingsUiEvent.ChooseReceive) },
                )
            }
            is MigrationUi.Receive -> {
                Text(
                    text = stringResource(R.string.backup_migrate_receive_body),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
                if (migration.waiting) {
                    Text(
                        text = stringResource(R.string.backup_migrate_scanning),
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                    )
                } else if (migration.paused) {
                    Text(
                        text = stringResource(R.string.backup_migrate_paused),
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                    )
                }
                TextButton(
                    onClick = { onEvent(BackupSettingsUiEvent.StartReceive) },
                    enabled = !migration.waiting,
                ) {
                    Text(
                        text = stringResource(R.string.backup_migrate_receive_action),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            is MigrationUi.Scan -> {
                Text(
                    text = stringResource(R.string.backup_migrate_pick),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
                if (migration.devices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.backup_migrate_empty_list),
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                    )
                } else {
                    migration.devices.forEach { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .interactiveClick {
                                    onEvent(BackupSettingsUiEvent.SelectPeer(peer.address))
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = peer.name,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text = peer.address,
                                    color = colors.textTertiary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = when {
                        migration.scanning -> stringResource(R.string.backup_migrate_scanning)
                        migration.paused -> stringResource(R.string.backup_migrate_paused)
                        else -> stringResource(R.string.backup_migrate_scan_done)
                    },
                    color = colors.textTertiary,
                    fontSize = 13.sp,
                )
                TextButton(onClick = { onEvent(BackupSettingsUiEvent.Rescan) }) {
                    Text(text = stringResource(R.string.backup_bluetooth_scan), color = colors.primary)
                }
            }
            MigrationUi.Connecting -> Waiting(stringResource(R.string.backup_migrate_connecting))
            is MigrationUi.Code -> {
                Text(
                    text = stringResource(R.string.backup_migrate_code_title),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = stringResource(R.string.backup_migrate_code_body),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
                Text(
                    text = migration.digits,
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.backup_migrate_code_seconds, migration.secondsLeft),
                    color = colors.textTertiary,
                    fontSize = 13.sp,
                )
                if (migration.waitingForPeer) {
                    Text(
                        text = stringResource(R.string.backup_migrate_code_wait),
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                    )
                } else {
                    TextButton(onClick = { onEvent(BackupSettingsUiEvent.CodeMatches) }) {
                        Text(
                            text = stringResource(R.string.backup_migrate_code_match),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(onClick = { onEvent(BackupSettingsUiEvent.CodeDiffers) }) {
                        Text(
                            text = stringResource(R.string.backup_migrate_code_differ),
                            color = colors.expense,
                        )
                    }
                }
            }
            MigrationUi.WaitReplace -> Waiting(stringResource(R.string.backup_migrate_wait_replace))
            MigrationUi.Replace -> {
                Text(
                    text = stringResource(R.string.backup_migrate_replace_title),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = stringResource(R.string.backup_migrate_replace_message),
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                )
                TextButton(onClick = { onEvent(BackupSettingsUiEvent.ConfirmReplace) }) {
                    Text(
                        text = stringResource(R.string.backup_migrate_replace_action),
                        color = colors.expense,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = { onEvent(BackupSettingsUiEvent.DeclineReplace) }) {
                    Text(text = stringResource(R.string.cancel), color = colors.textSecondary)
                }
            }
            is MigrationUi.Transfer -> {
                val progress = migration.progress
                if (progress == null) {
                    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(42.dp))
                } else {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = colors.primary,
                        modifier = Modifier.size(42.dp),
                    )
                }
                Text(
                    text = stringResource(migration.labelRes),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                if (progress != null) {
                    val percent = (progress * 100).roundToInt().coerceIn(0, 100)
                    Text(
                        text = stringResource(R.string.backup_working_percent, percent),
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.primary,
                        trackColor = colors.tagBackground,
                        drawStopIndicator = {},
                    )
                }
            }
            is MigrationUi.Failed -> {
                Text(
                    text = stringResource(migration.messageRes),
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                )
                TextButton(onClick = { onEvent(BackupSettingsUiEvent.RetryMigration) }) {
                    Text(
                        text = stringResource(R.string.backup_migrate_retry),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = { onEvent(BackupSettingsUiEvent.CopyDiagnostic) }) {
                    Text(text = stringResource(R.string.backup_migrate_copy), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun RoleRow(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .interactiveClick(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.PhoneAndroid,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
            Text(
                text = body,
                color = colors.textSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun Waiting(text: String) {
    val colors = MaterialTheme.appColors
    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(42.dp))
    Text(
        text = text,
        color = colors.textPrimary,
        fontSize = 15.sp,
    )
}
