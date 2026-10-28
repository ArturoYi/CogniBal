package com.example.cognibal.screen.feature.settings.backup

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognibal.R
import com.example.cognibal.data.backup.bluetooth.MigrationProtocol
import com.example.cognibal.screen.feature.settings.SettingActionItem
import com.example.cognibal.screen.feature.settings.SettingsDivider
import com.example.cognibal.screen.feature.settings.SettingsSectionCard
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiEvent
import com.example.cognibal.screen.feature.settings.backup.state.MigrationUi
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.interaction.interactiveClick
import com.example.cognibal.ui.loading.CogniBalLoadingIndicator
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.util.Locale
import kotlin.math.roundToInt

private val searchSeconds: Int = (MigrationProtocol.SCAN_MILLIS / 1_000L).toInt()
private val confirmSeconds: Int = (MigrationProtocol.CONFIRM_MILLIS / 1_000L).toInt()

/** 蓝牙迁移的步骤。说明在中间滚动，操作固定在底部。 */
@Composable
internal fun MigrationPanel(
    migration: MigrationUi,
    onEvent: (BackupSettingsUiEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MigrationBody(migration = migration, onEvent = onEvent)
        }
        MigrationActions(migration = migration, onEvent = onEvent)
    }
}

@Composable
private fun MigrationBody(
    migration: MigrationUi,
    onEvent: (BackupSettingsUiEvent) -> Unit,
) {
    when (migration) {
        MigrationUi.Role -> {
            MigrationHint(stringResource(R.string.backup_migrate_file_hint))
            SettingsSectionCard {
                RoleSummary(
                    title = stringResource(R.string.backup_migrate_send),
                    body = stringResource(R.string.backup_migrate_send_body),
                    onClick = { onEvent(BackupSettingsUiEvent.ChooseSend) },
                )
                SettingsDivider()
                RoleSummary(
                    title = stringResource(R.string.backup_migrate_receive),
                    body = stringResource(R.string.backup_migrate_receive_body),
                    onClick = { onEvent(BackupSettingsUiEvent.ChooseReceive) },
                )
            }
        }
        is MigrationUi.Receive -> {
            val showClock = migration.waiting || migration.paused || migration.timedOut
            if (showClock) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BluetoothSearchVisual(
                        active = migration.waiting,
                        secondsLeft = migration.secondsLeft,
                        totalSeconds = searchSeconds,
                    )
                }
            }
            MigrationStatus(
                when {
                    migration.waiting -> stringResource(R.string.backup_migrate_receive_ready)
                    migration.paused -> stringResource(R.string.backup_migrate_paused)
                    migration.timedOut -> stringResource(R.string.backup_migrate_wait_timeout)
                    else -> stringResource(R.string.backup_migrate_receive_body)
                },
            )
            if (migration.waiting) {
                MigrationHint(stringResource(R.string.backup_migrate_keep_open))
            }
        }
        is MigrationUi.Scan -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BluetoothSearchVisual(
                    active = migration.scanning,
                    secondsLeft = migration.secondsLeft,
                    totalSeconds = searchSeconds,
                )
            }
            MigrationStatus(
                when {
                    migration.paused -> stringResource(R.string.backup_migrate_paused)
                    migration.scanning && migration.devices.isEmpty() ->
                        stringResource(R.string.backup_migrate_scanning)
                    migration.scanning -> stringResource(R.string.backup_migrate_pick)
                    else -> stringResource(R.string.backup_migrate_scan_done)
                },
            )
            if (migration.scanning) {
                MigrationHint(stringResource(R.string.backup_migrate_keep_open))
            }
            if (migration.devices.isEmpty()) {
                MigrationHint(stringResource(R.string.backup_migrate_empty_list))
            } else if (!migration.scanning) {
                MigrationHint(stringResource(R.string.backup_migrate_pick))
            }
            if (migration.devices.isNotEmpty()) {
                SettingsSectionCard {
                    migration.devices.forEachIndexed { index, peer ->
                        if (index > 0) SettingsDivider()
                        SettingActionItem(
                            icon = Icons.Outlined.Bluetooth,
                            title = peer.name,
                            subtitle = peer.address,
                            onClick = { onEvent(BackupSettingsUiEvent.SelectPeer(peer.address)) },
                        )
                    }
                }
            }
        }
        MigrationUi.Connecting -> {
            Waiting(stringResource(R.string.backup_migrate_connecting))
            MigrationHint(stringResource(R.string.backup_migrate_connecting_hint))
        }
        is MigrationUi.Code -> {
            MigrationTitle(stringResource(R.string.backup_migrate_code_title))
            MigrationHint(stringResource(R.string.backup_migrate_code_body))
            Text(
                text = migration.digits,
                color = MaterialTheme.appColors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            CountdownBar(
                secondsLeft = migration.secondsLeft,
                totalSeconds = confirmSeconds,
            )
            if (migration.waitingForPeer) {
                MigrationHint(stringResource(R.string.backup_migrate_code_wait))
            }
        }
        MigrationUi.WaitReplace -> {
            Waiting(stringResource(R.string.backup_migrate_wait_replace))
            MigrationHint(stringResource(R.string.backup_migrate_replace_prompt))
        }
        MigrationUi.Replace -> {
            MigrationTitle(stringResource(R.string.backup_migrate_replace_title))
            MigrationHint(stringResource(R.string.backup_migrate_replace_message))
        }
        is MigrationUi.Transfer -> {
            TransferProgress(migration)
            MigrationHint(stringResource(R.string.backup_migrate_transfer_hint))
        }
        is MigrationUi.Failed -> {
            MigrationStatus(stringResource(migration.messageRes))
            MigrationHint(stringResource(R.string.backup_migrate_failed_hint))
        }
    }
}

@Composable
private fun MigrationActions(
    migration: MigrationUi,
    onEvent: (BackupSettingsUiEvent) -> Unit,
) {
    val colors = MaterialTheme.appColors
    val buttons: @Composable () -> Unit = when (migration) {
        MigrationUi.Role,
        MigrationUi.Connecting,
        MigrationUi.WaitReplace,
        is MigrationUi.Transfer,
        -> return
        is MigrationUi.Receive -> {
            {
                MigrationPrimaryButton(
                    text = stringResource(
                        if (migration.waiting) {
                            R.string.backup_migrate_waiting_action
                        } else {
                            R.string.backup_migrate_receive_action
                        },
                    ),
                    onClick = { onEvent(BackupSettingsUiEvent.StartReceive) },
                    enabled = !migration.waiting,
                )
            }
        }
        is MigrationUi.Scan -> {
            {
                MigrationPrimaryButton(
                    text = stringResource(
                        if (migration.scanning) {
                            R.string.backup_migrate_scanning_action
                        } else {
                            R.string.backup_bluetooth_scan
                        },
                    ),
                    onClick = { onEvent(BackupSettingsUiEvent.Rescan) },
                    enabled = !migration.scanning,
                )
            }
        }
        is MigrationUi.Code -> {
            {
                if (migration.waitingForPeer) {
                    MigrationPrimaryButton(
                        text = stringResource(R.string.backup_migrate_code_waiting_action),
                        onClick = {},
                        enabled = false,
                    )
                } else {
                    MigrationPrimaryButton(
                        text = stringResource(R.string.backup_migrate_code_match),
                        onClick = { onEvent(BackupSettingsUiEvent.CodeMatches) },
                    )
                    MigrationTextButton(
                        text = stringResource(R.string.backup_migrate_code_differ),
                        onClick = { onEvent(BackupSettingsUiEvent.CodeDiffers) },
                        color = colors.expense,
                    )
                }
            }
        }
        MigrationUi.Replace -> {
            {
                MigrationPrimaryButton(
                    text = stringResource(R.string.backup_migrate_replace_action),
                    onClick = { onEvent(BackupSettingsUiEvent.ConfirmReplace) },
                    container = colors.expense.copy(alpha = if (colors.isDark) 0.22f else 0.12f),
                    content = colors.expense,
                )
                MigrationTextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { onEvent(BackupSettingsUiEvent.DeclineReplace) },
                    color = colors.textSecondary,
                )
            }
        }
        is MigrationUi.Failed -> {
            {
                MigrationPrimaryButton(
                    text = stringResource(R.string.backup_migrate_retry),
                    onClick = { onEvent(BackupSettingsUiEvent.RetryMigration) },
                )
                MigrationTextButton(
                    text = stringResource(R.string.backup_migrate_copy),
                    onClick = { onEvent(BackupSettingsUiEvent.CopyDiagnostic) },
                    color = colors.textSecondary,
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { buttons() },
    )
}

@Composable
private fun BluetoothSearchVisual(
    active: Boolean,
    secondsLeft: Int,
    totalSeconds: Int,
) {
    val colors = MaterialTheme.appColors
    val context = LocalContext.current
    val reduceMotion = remember(context) { animationsDisabled(context) }
    val transition = rememberInfiniteTransition(label = "bluetoothSearch")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800, easing = LinearEasing),
        ),
        label = "bluetoothSearchPhase",
    )
    val safeSeconds = secondsLeft.coerceAtLeast(0)
    val fraction = if (totalSeconds <= 0) {
        0f
    } else {
        safeSeconds.coerceAtMost(totalSeconds) / totalSeconds.toFloat()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(168.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val radius = size.minDimension / 2f - stroke
                val arcSize = Size(radius * 2f, radius * 2f)
                val topLeft = Offset(
                    (size.width - arcSize.width) / 2f,
                    (size.height - arcSize.height) / 2f,
                )
                val center = this.center
                if (active && !reduceMotion) {
                    repeat(3) { index ->
                        val shift = (phase + index / 3f) % 1f
                        drawCircle(
                            color = colors.primary.copy(alpha = (1f - shift) * 0.22f),
                            radius = radius * (0.34f + 0.66f * shift),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                    rotate(degrees = phase * 360f, pivot = center) {
                        drawArc(
                            color = colors.primary.copy(alpha = 0.16f),
                            startAngle = -32f,
                            sweepAngle = 32f,
                            useCenter = true,
                            topLeft = topLeft,
                            size = arcSize,
                        )
                    }
                } else {
                    drawCircle(
                        color = colors.primary.copy(alpha = 0.10f),
                        radius = radius * 0.62f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                drawArc(
                    color = colors.tagBackground,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (fraction > 0f) {
                    drawArc(
                        color = colors.primary,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        colors.primary.copy(alpha = if (colors.isDark) 0.20f else 0.12f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bluetooth,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SearchClock(active = active, secondsLeft = safeSeconds)
    }
}

@Composable
private fun SearchClock(
    active: Boolean,
    secondsLeft: Int,
) {
    val colors = MaterialTheme.appColors
    val clock = String.format(Locale.US, "%02d:%02d", secondsLeft / 60, secondsLeft % 60)
    Text(
        text = clock,
        modifier = Modifier.clearAndSetSemantics {},
        color = if (active) colors.primary else colors.textTertiary,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    )
    if (active) {
        Text(
            text = stringResource(R.string.backup_migrate_code_seconds, secondsLeft),
            color = if (secondsLeft <= 10) colors.primary else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (secondsLeft <= 10) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CountdownBar(
    secondsLeft: Int,
    totalSeconds: Int,
) {
    val colors = MaterialTheme.appColors
    val safeSeconds = secondsLeft.coerceAtLeast(0)
    val fraction = if (totalSeconds <= 0) 0f else safeSeconds / totalSeconds.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(99.dp)),
            color = colors.primary,
            trackColor = colors.tagBackground,
            drawStopIndicator = {},
        )
        Text(
            text = stringResource(R.string.backup_migrate_code_seconds, safeSeconds),
            color = if (safeSeconds <= 10) colors.primary else colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = if (safeSeconds <= 10) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun TransferProgress(migration: MigrationUi.Transfer) {
    val colors = MaterialTheme.appColors
    val progress = migration.progress
    if (progress == null) {
        CogniBalLoadingIndicator(contentDescription = null)
    } else {
        CircularProgressIndicator(
            progress = { progress },
            color = colors.primary,
            modifier = Modifier.size(42.dp),
        )
    }
    MigrationStatus(stringResource(migration.labelRes))
    if (progress != null) {
        val percent = (progress * 100).roundToInt().coerceIn(0, 100)
        Text(
            text = stringResource(R.string.backup_working_percent, percent),
            color = colors.textSecondary,
            fontSize = 13.sp,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(99.dp)),
            color = colors.primary,
            trackColor = colors.tagBackground,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun MigrationTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.appColors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
}

@Composable
private fun MigrationStatus(text: String) {
    Text(
        text = text,
        color = MaterialTheme.appColors.textPrimary,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
}

@Composable
private fun MigrationHint(text: String) {
    Text(
        text = text,
        color = MaterialTheme.appColors.textSecondary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun MigrationPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    container: Color = MaterialTheme.appColors.primary,
    content: Color = MaterialTheme.appColors.onPrimary,
) {
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
            disabledContainerColor = container.copy(alpha = 0.38f),
            disabledContentColor = content.copy(alpha = 0.72f),
        ),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MigrationTextButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RoleSummary(
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
            imageVector = Icons.Outlined.PhoneAndroid,
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
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun Waiting(text: String) {
    CogniBalLoadingIndicator(contentDescription = null)
    MigrationStatus(text)
}

private fun animationsDisabled(context: Context): Boolean {
    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
    return scale == 0f
}

@FormFactorPreviews
@Composable
private fun MigrationScanPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            MigrationPanel(
                migration = MigrationUi.Scan(
                    devices = emptyList(),
                    scanning = true,
                    paused = false,
                    secondsLeft = 42,
                ),
                onEvent = {},
            )
        }
    }
}
