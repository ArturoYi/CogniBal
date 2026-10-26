package com.example.cognibal.screen.feature.settings.backup

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.AndroidRuntimeException
import android.bluetooth.BluetoothAdapter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cognibal.R
import com.example.cognibal.screen.feature.settings.SettingsAction
import com.example.cognibal.screen.feature.settings.SettingsActionGroup
import com.example.cognibal.screen.feature.settings.SettingsSubpage
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsAction
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsEffect
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiEvent
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiState
import com.example.cognibal.screen.feature.settings.backup.state.BackupWorkUi
import com.example.cognibal.ui.background.PageBackgroundHost
import com.example.cognibal.ui.loading.CogniBalLoadingIndicator
import com.example.cognibal.ui.preview.FormFactorPreviews
import com.example.cognibal.ui.theme.AppThemeStyle
import com.example.cognibal.ui.theme.CogniBalTheme
import com.example.cognibal.ui.theme.appColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.system.exitProcess

/** 备份和迁移。文件分享、蓝牙系统框留在这里，账本读写留给 ViewModel。 */
@Suppress("DEPRECATION")
@Composable
internal fun BackupSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupSettingsViewModel = viewModel(factory = BackupSettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onEvent(BackupSettingsUiEvent.SystemPromptFinished)
        val granted = result.isNotEmpty() && result.values.all { it }
        viewModel.onEvent(
            if (granted) {
                BackupSettingsUiEvent.BluetoothPermissionGranted
            } else {
                BackupSettingsUiEvent.BluetoothPermissionDenied
            },
        )
    }
    val enableBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onEvent(BackupSettingsUiEvent.SystemPromptFinished)
        viewModel.onEvent(
            if (result.resultCode == Activity.RESULT_OK) {
                BackupSettingsUiEvent.BluetoothEnabled
            } else {
                BackupSettingsUiEvent.BluetoothEnableDeclined
            },
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (context.findActivity()?.isChangingConfigurations == true) return@LifecycleEventEffect
        viewModel.onEvent(BackupSettingsUiEvent.ForegroundLost)
    }
    DisposableEffect(viewModel) {
        onDispose {
            if (context.findActivity()?.isChangingConfigurations != true) {
                viewModel.onEvent(BackupSettingsUiEvent.PageLeft)
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BackupSettingsEffect.Toast ->
                    Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
                is BackupSettingsEffect.Share -> {
                    if (!context.shareBackup(effect.uri)) {
                        Toast.makeText(context, R.string.backup_error_share, Toast.LENGTH_SHORT).show()
                    }
                }
                is BackupSettingsEffect.RequestBluetoothPermission -> {
                    if (effect.permissions.isEmpty()) {
                        viewModel.onEvent(BackupSettingsUiEvent.BluetoothPermissionGranted)
                    } else {
                        permissionLauncher.launch(effect.permissions.toTypedArray())
                    }
                }
                BackupSettingsEffect.RequestEnableBluetooth -> {
                    try {
                        enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onEvent(BackupSettingsUiEvent.SystemPromptFinished)
                        viewModel.onEvent(BackupSettingsUiEvent.BluetoothEnableDeclined)
                    } catch (_: SecurityException) {
                        viewModel.onEvent(BackupSettingsUiEvent.SystemPromptFinished)
                        viewModel.onEvent(BackupSettingsUiEvent.BluetoothEnableDeclined)
                    }
                }
                is BackupSettingsEffect.CopyDiagnostic -> {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("cognibal-migration", effect.text))
                    Toast.makeText(context, R.string.backup_migrate_copied, Toast.LENGTH_SHORT).show()
                }
                BackupSettingsEffect.OpenAppSettings -> context.openAppSettings()
                BackupSettingsEffect.RestartApp -> context.reopenApp()
            }
        }
    }
    BackupSettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun BackupSettingsScreen(
    uiState: BackupSettingsUiState,
    onEvent: (BackupSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedAt = uiState.savedAtMillis
    val migration = uiState.migration
    BackHandler(enabled = uiState.work != null || migration != null) {
        when {
            uiState.work?.cancellable == true -> onEvent(BackupSettingsUiEvent.CancelWork)
            migration != null -> onEvent(BackupSettingsUiEvent.MigrationBack)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        SettingsSubpage(
            title = stringResource(R.string.settings_backup),
            onBack = {
                when {
                    uiState.work != null -> Unit
                    migration != null -> onEvent(BackupSettingsUiEvent.MigrationBack)
                    else -> onBack()
                }
            },
        ) {
            if (migration != null) {
                MigrationPanel(migration = migration, onEvent = onEvent)
            } else SettingsActionGroup(
                actions = uiState.rows.map { row ->
                    val subtitle = if (row.action == BackupSettingsAction.Backup && savedAt != null) {
                        stringResource(R.string.backup_last_time, formatBackupTime(savedAt))
                    } else {
                        stringResource(row.subtitleRes)
                    }
                    SettingsAction(
                        icon = row.action.icon,
                        title = stringResource(row.titleRes),
                        subtitle = subtitle,
                    )
                },
                onAction = { index ->
                    onEvent(BackupSettingsUiEvent.RowClicked(uiState.rows[index].action))
                },
            )
        }
        uiState.work?.let { work ->
            BackupWorkOverlay(work = work, onCancel = { onEvent(BackupSettingsUiEvent.CancelWork) })
        }
    }
    if (uiState.confirmImport) {
        BackupConfirmDialog(
            titleRes = R.string.backup_import_confirm_title,
            messageRes = R.string.backup_import_confirm_message,
            confirmRes = R.string.backup_import_action,
            onConfirm = { onEvent(BackupSettingsUiEvent.ConfirmImport) },
            onDismiss = { onEvent(BackupSettingsUiEvent.DismissImportConfirm) },
        )
    }
}

@Composable
private fun BackupWorkOverlay(
    work: BackupWorkUi,
    onCancel: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(interactionSource = scrimInteraction, indication = null, onClick = {}),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 220.dp, max = 280.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .clickable(interactionSource = cardInteraction, indication = null, onClick = {})
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val progress = work.progress
            if (progress == null) {
                CogniBalLoadingIndicator(contentDescription = null)
            } else {
                CircularProgressIndicator(
                    progress = { progress },
                    color = colors.primary,
                    modifier = Modifier.size(42.dp),
                )
            }
            Text(
                text = stringResource(work.labelRes),
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(99.dp)),
                    color = colors.primary,
                    trackColor = colors.tagBackground,
                    drawStopIndicator = {},
                )
            }
            if (work.cancellable) {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.cancel), color = colors.textSecondary)
                }
            }
        }
    }
}

private val BackupSettingsAction.icon: ImageVector
    get() = when (this) {
        BackupSettingsAction.Backup -> Icons.Rounded.Backup
        BackupSettingsAction.Export -> Icons.Rounded.CloudUpload
        BackupSettingsAction.Import -> Icons.Rounded.CloudDownload
        BackupSettingsAction.NearbyShare -> Icons.Rounded.Bluetooth
    }

private fun formatBackupTime(millis: Long): String {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(millis))
}

private fun Context.shareBackup(uri: Uri): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("cognibal-backup", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, getString(R.string.backup_export)).apply {
        clipData = ClipData.newRawUri("cognibal-backup", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (findActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        startActivity(chooser)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: AndroidRuntimeException) {
        false
    }
}

private fun Context.reopenApp() {
    val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(launch)
    exitProcess(0)
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@FormFactorPreviews
@Composable
private fun BackupSettingsPreview() {
    CogniBalTheme(themeStyle = AppThemeStyle.WarmCream, darkTheme = false) {
        PageBackgroundHost {
            BackupSettingsScreen(
                uiState = BackupSettingsUiState(),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
