package com.example.cognibal.screen.feature.settings.backup

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cognibal.R
import com.example.cognibal.data.backup.BackupException
import com.example.cognibal.data.backup.LedgerBackup
import com.example.cognibal.data.backup.bluetooth.BackupBluetooth
import com.example.cognibal.data.backup.bluetooth.BluetoothReadiness
import com.example.cognibal.data.backup.bluetooth.MigrationCallbacks
import com.example.cognibal.data.backup.bluetooth.MigrationException
import com.example.cognibal.data.database.AppDatabase
import com.example.cognibal.di.LedgerEntryPoint
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsAction
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsEffect
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiEvent
import com.example.cognibal.screen.feature.settings.backup.state.BackupSettingsUiState
import com.example.cognibal.screen.feature.settings.backup.state.BackupWorkUi
import com.example.cognibal.screen.feature.settings.backup.state.BluetoothPeerUi
import com.example.cognibal.screen.feature.settings.backup.state.MigrationRole
import com.example.cognibal.screen.feature.settings.backup.state.MigrationUi
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * 备份、文件导出导入，以及蓝牙整库迁移。
 *
 * 文件导出和导入仍走原来的口令压缩包。蓝牙迁移另走协议 2：
 * 先选旧手机发送或新手机接收，核对 6 位数字后再替换。发送端原库保持不动。
 */
internal class BackupSettingsViewModel(
    private val backup: LedgerBackup,
    private val bluetooth: BackupBluetooth,
) : ViewModel() {
    private val state = MutableStateFlow(BackupSettingsUiState())
    private val effectChannel = Channel<BackupSettingsEffect>(Channel.BUFFERED)
    private val running = AtomicBoolean(false)
    private var workJob: Job? = null
    private var sessionJob: Job? = null
    private var scanObserve: Job? = null
    private var scanTimer: Job? = null
    private var ticker: Job? = null
    private var role: MigrationRole? = null
    private var pending: PendingBluetooth? = null
    private var expectingSystemUi = false
    private var engaged = false
    private var failureShown = false
    private var stopSearchOnly = false
    private var codeDeferred: CompletableDeferred<Boolean>? = null
    private var replaceDeferred: CompletableDeferred<Boolean>? = null

    val uiState: StateFlow<BackupSettingsUiState> = state
    val effects: Flow<BackupSettingsEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    fun onEvent(event: BackupSettingsUiEvent) {
        when (event) {
            is BackupSettingsUiEvent.RowClicked -> onRow(event.action)
            BackupSettingsUiEvent.DismissImportConfirm ->
                state.update { it.copy(confirmImport = false) }
            BackupSettingsUiEvent.ConfirmImport -> {
                state.update { it.copy(confirmImport = false) }
                restoreLocal()
            }
            BackupSettingsUiEvent.ChooseSend -> chooseSend()
            BackupSettingsUiEvent.ChooseReceive -> chooseReceive()
            BackupSettingsUiEvent.StartReceive -> startReceive()
            BackupSettingsUiEvent.Rescan -> rescan()
            is BackupSettingsUiEvent.SelectPeer -> selectPeer(event.address)
            BackupSettingsUiEvent.CodeMatches -> codeDeferred?.complete(true).also {
                state.update { current ->
                    val code = current.migration as? MigrationUi.Code ?: return@update current
                    current.copy(migration = code.copy(waitingForPeer = true))
                }
            }
            BackupSettingsUiEvent.CodeDiffers -> codeDeferred?.complete(false)
            BackupSettingsUiEvent.ConfirmReplace -> replaceDeferred?.complete(true)
            BackupSettingsUiEvent.DeclineReplace -> replaceDeferred?.complete(false)
            BackupSettingsUiEvent.RetryMigration -> retry()
            BackupSettingsUiEvent.CopyDiagnostic -> copyDiagnostic()
            BackupSettingsUiEvent.MigrationBack -> onMigrationBack()
            BackupSettingsUiEvent.ForegroundLost -> onForegroundLost()
            BackupSettingsUiEvent.PageLeft -> onPageLeft()
            BackupSettingsUiEvent.CancelWork -> {
                workJob?.cancel()
            }
            BackupSettingsUiEvent.BluetoothPermissionGranted -> beginPending()
            BackupSettingsUiEvent.BluetoothPermissionDenied -> {
                pending = null
                toast(R.string.backup_bluetooth_permission)
                emit(BackupSettingsEffect.OpenAppSettings)
            }
            BackupSettingsUiEvent.BluetoothEnabled -> beginPending()
            BackupSettingsUiEvent.BluetoothEnableDeclined -> {
                pending = null
                toast(R.string.backup_bluetooth_disabled)
            }
            BackupSettingsUiEvent.SystemPromptFinished -> expectingSystemUi = false
        }
    }

    override fun onCleared() {
        scanTimer?.cancel()
        scanObserve?.cancel()
        ticker?.cancel()
        bluetooth.stop()
        sessionJob?.cancel()
        workJob?.cancel()
    }

    private fun onRow(action: BackupSettingsAction) {
        val current = state.value
        if (running.get() || current.work != null || current.confirmImport || current.migration != null) {
            return
        }
        when (action) {
            BackupSettingsAction.Backup -> saveBackup()
            BackupSettingsAction.Export -> viewModelScope.launch { startExport() }
            BackupSettingsAction.Import -> viewModelScope.launch { startImport() }
            BackupSettingsAction.NearbyShare -> state.update { it.copy(migration = MigrationUi.Role) }
        }
    }

    private fun saveBackup() {
        launchWork {
            setWork(R.string.backup_working_save, null, false)
            yield()
            backup.create()
            refresh()
            toast(R.string.backup_saved)
        }
    }

    private suspend fun startExport() {
        if (running.get()) return
        if (backup.exists()) {
            launchWork {
                setWork(R.string.backup_working_share, progress = null, cancellable = false)
                yield()
                emit(BackupSettingsEffect.Share(backup.shareUri()))
            }
        } else {
            launchWork {
                setWork(R.string.backup_working_save, null, false)
                yield()
                backup.create()
                refresh()
                setWork(R.string.backup_working_share, null, false)
                emit(BackupSettingsEffect.Share(backup.shareUri()))
            }
        }
    }

    private suspend fun startImport() {
        if (running.get()) return
        if (!backup.exists()) {
            toast(R.string.backup_missing)
            return
        }
        state.update { it.copy(confirmImport = true) }
    }

    private fun restoreLocal() {
        launchWork {
            setWork(R.string.backup_working_import, null, false)
            yield()
            backup.importLocal()
            refresh()
            toast(R.string.backup_imported)
        }
    }

    private fun chooseSend() {
        role = MigrationRole.Send
        viewModelScope.launch {
            val hasEntries = try {
                bluetooth.hasEntriesInUse()
            } catch (exception: Exception) {
                Log.w(TAG, "无法确认保险柜是否有条目", exception)
                toast(R.string.backup_error_generic)
                return@launch
            }
            if (!hasEntries) {
                state.update { it.copy(migration = null) }
                toast(R.string.backup_migrate_empty)
                return@launch
            }
            ensureBluetooth(PendingBluetooth.Scan)
        }
    }

    private fun chooseReceive() {
        role = MigrationRole.Receive
        state.update { it.copy(migration = MigrationUi.Receive(waiting = false, paused = false)) }
    }

    private fun startReceive() {
        if (running.get()) return
        role = MigrationRole.Receive
        ensureBluetooth(PendingBluetooth.Advertise)
    }

    private fun ensureBluetooth(next: PendingBluetooth) {
        pending = next
        when (bluetooth.readiness()) {
            BluetoothReadiness.Unavailable -> {
                pending = null
                toast(R.string.backup_bluetooth_unavailable)
            }
            BluetoothReadiness.PermissionRequired -> {
                expectingSystemUi = true
                emit(BackupSettingsEffect.RequestBluetoothPermission(bluetooth.requiredRuntimePermissions()))
            }
            BluetoothReadiness.Disabled -> {
                expectingSystemUi = true
                emit(BackupSettingsEffect.RequestEnableBluetooth)
            }
            BluetoothReadiness.Ready -> beginPending()
        }
    }

    private fun beginPending() {
        expectingSystemUi = false
        when (pending) {
            PendingBluetooth.Scan -> openScan()
            PendingBluetooth.Advertise -> beginAdvertise()
            null -> Unit
        }
        pending = null
    }

    private fun openScan() {
        state.update {
            it.copy(migration = MigrationUi.Scan(devices = emptyList(), scanning = true, paused = false))
        }
        observeScan()
        try {
            bluetooth.startDiscovery()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            showFailure(exception, MigrationRole.Send)
            return
        }
        armScanTimer()
    }

    private fun beginAdvertise() {
        if (!running.compareAndSet(false, true)) return
        failureShown = false
        stopSearchOnly = false
        engaged = false
        state.update { it.copy(migration = MigrationUi.Receive(waiting = true, paused = false)) }
        sessionJob = viewModelScope.launch {
            try {
                bluetooth.receive(callbacks(MigrationRole.Receive))
                toast(R.string.backup_migrate_received)
                state.update { it.copy(migration = null) }
                emit(BackupSettingsEffect.RestartApp)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "蓝牙接收没有完成", exception)
                if (!failureShown) showFailure(exception, MigrationRole.Receive)
            } finally {
                running.set(false)
                engaged = false
            }
        }
    }

    private fun rescan() {
        if (running.get() || state.value.migration !is MigrationUi.Scan) return
        state.update { current ->
            val scan = current.migration as? MigrationUi.Scan ?: return@update current
            current.copy(migration = scan.copy(scanning = true, paused = false))
        }
        try {
            bluetooth.startDiscovery()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            showFailure(exception, MigrationRole.Send)
            return
        }
        armScanTimer()
    }

    private fun selectPeer(address: String) {
        if (!running.compareAndSet(false, true)) return
        scanTimer?.cancel()
        bluetooth.stopScan()
        failureShown = false
        stopSearchOnly = false
        engaged = false
        role = MigrationRole.Send
        state.update { it.copy(migration = MigrationUi.Connecting) }
        sessionJob = viewModelScope.launch {
            try {
                bluetooth.send(address, callbacks(MigrationRole.Send))
                toast(R.string.backup_migrate_sent)
                state.update { it.copy(migration = null) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "蓝牙发送没有完成", exception)
                if (!failureShown) showFailure(exception, MigrationRole.Send)
            } finally {
                running.set(false)
                engaged = false
            }
        }
    }

    private fun retry() {
        val failed = state.value.migration as? MigrationUi.Failed ?: return
        failureShown = false
        when (failed.role) {
            MigrationRole.Send -> chooseSend()
            MigrationRole.Receive ->
                state.update { it.copy(migration = MigrationUi.Receive(waiting = false, paused = false)) }
        }
    }

    private fun onMigrationBack() {
        when (val current = state.value.migration) {
            null -> Unit
            MigrationUi.Role,
            is MigrationUi.Failed,
            -> state.update { it.copy(migration = null) }
            is MigrationUi.Scan,
            is MigrationUi.Receive,
            -> {
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = MigrationUi.Role) }
            }
            else -> abortConnected()
        }
    }

    private fun onForegroundLost() {
        if (expectingSystemUi) return
        when (val current = state.value.migration) {
            is MigrationUi.Scan -> {
                bluetooth.stopScan()
                scanTimer?.cancel()
                state.update { it.copy(migration = current.copy(scanning = false, paused = true)) }
            }
            is MigrationUi.Receive -> if (current.waiting) {
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = current.copy(waiting = false, paused = true)) }
            }
            MigrationUi.Connecting,
            is MigrationUi.Code,
            MigrationUi.WaitReplace,
            MigrationUi.Replace,
            is MigrationUi.Transfer,
            -> abortConnected()
            else -> Unit
        }
    }

    private fun onPageLeft() {
        when (val current = state.value.migration) {
            null -> bluetooth.stop()
            MigrationUi.Role,
            is MigrationUi.Failed,
            -> state.update { it.copy(migration = null) }
            is MigrationUi.Scan,
            is MigrationUi.Receive,
            -> {
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = null) }
            }
            else -> abortConnected()
        }
    }

    private fun pauseSearch() {
        scanTimer?.cancel()
        bluetooth.stopDiscovery()
        if (sessionJob?.isActive == true) {
            bluetooth.cancel()
            sessionJob?.cancel()
        }
    }

    private fun abortConnected() {
        failureShown = true
        engaged = false
        ticker?.cancel()
        codeDeferred?.cancel()
        replaceDeferred?.cancel()
        val currentRole = role ?: MigrationRole.Send
        state.update {
            it.copy(
                migration = MigrationUi.Failed(
                    messageRes = R.string.backup_migrate_aborted,
                    diagnostic = diagnostic(
                        MigrationException(MigrationException.Kind.Rejected, "leave", "页面离开前台"),
                        currentRole,
                    ),
                    role = currentRole,
                ),
            )
        }
        bluetooth.cancel()
        sessionJob?.cancel()
    }

    private fun callbacks(owner: MigrationRole): MigrationCallbacks {
        return object : MigrationCallbacks {
            override fun onConnected() {
                engaged = true
                state.update { it.copy(migration = MigrationUi.Connecting) }
            }

            override fun onCode(digits: String) {
                codeDeferred = CompletableDeferred()
                state.update {
                    it.copy(migration = MigrationUi.Code(digits = digits, secondsLeft = 60, waitingForPeer = false))
                }
                startTicker()
            }

            override suspend fun confirmCode(): Boolean = codeDeferred?.await() ?: false

            override fun onWaitReplace() {
                ticker?.cancel()
                if (owner == MigrationRole.Receive) {
                    replaceDeferred = CompletableDeferred()
                    state.update { it.copy(migration = MigrationUi.Replace) }
                } else {
                    state.update { it.copy(migration = MigrationUi.WaitReplace) }
                }
            }

            override suspend fun confirmReplace(): Boolean = replaceDeferred?.await() ?: false

            override fun onTransfer() {
                val label = if (owner == MigrationRole.Receive) {
                    R.string.backup_migrate_receiving
                } else {
                    R.string.backup_migrate_sending
                }
                state.update { it.copy(migration = MigrationUi.Transfer(label, progress = null)) }
            }

            override fun onProgress(sent: Long, total: Long) {
                val fraction = if (total <= 0L) 0f else (sent.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                state.update { current ->
                    val transfer = current.migration as? MigrationUi.Transfer ?: return@update current
                    current.copy(migration = transfer.copy(progress = fraction))
                }
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            for (left in 60 downTo 0) {
                state.update { current ->
                    val code = current.migration as? MigrationUi.Code ?: return@update current
                    current.copy(migration = code.copy(secondsLeft = left))
                }
                if (left == 0) break
                delay(1_000)
            }
        }
    }

    private fun observeScan() {
        scanObserve?.cancel()
        scanObserve = viewModelScope.launch {
            bluetooth.scan.collect { scan ->
                state.update { current ->
                    val ui = current.migration as? MigrationUi.Scan ?: return@update current
                    current.copy(
                        migration = ui.copy(
                            devices = scan.devices.map { peer -> BluetoothPeerUi(peer.address, peer.name) },
                            scanning = scan.scanning,
                        ),
                    )
                }
            }
        }
    }

    private fun armScanTimer() {
        scanTimer?.cancel()
        scanTimer = viewModelScope.launch {
            delay(60_000)
            bluetooth.stopScan()
        }
    }

    private fun showFailure(error: Throwable, owner: MigrationRole) {
        failureShown = true
        ticker?.cancel()
        state.update {
            it.copy(
                migration = MigrationUi.Failed(
                    messageRes = messageRes(error),
                    diagnostic = diagnostic(error, owner),
                    role = owner,
                ),
            )
        }
    }

    private fun copyDiagnostic() {
        val failed = state.value.migration as? MigrationUi.Failed ?: return
        emit(BackupSettingsEffect.CopyDiagnostic(failed.diagnostic))
    }

    private fun diagnostic(error: Throwable, owner: MigrationRole): String {
        val phase = (error as? MigrationException)?.phase ?: "unknown"
        val kind = (error as? MigrationException)?.kind?.name ?: error.javaClass.simpleName
        val detail = error.message.orEmpty().lineSequence().firstOrNull().orEmpty().take(180)
        return listOf(
            "CogniBal migration",
            "role=${owner.name.lowercase()}",
            "phase=$phase",
            "protocol=2",
            "schema=${AppDatabase.VERSION}",
            "kind=$kind",
            "detail=$detail",
        ).joinToString("\n")
    }

    private fun launchWork(block: suspend () -> Unit): Boolean {
        if (!running.compareAndSet(false, true)) return false
        workJob = viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "备份操作没有完成", exception)
                toast(messageRes(exception))
            } finally {
                state.update { it.copy(work = null) }
                running.set(false)
            }
        }
        return true
    }

    private fun setWork(@StringRes labelRes: Int, progress: Float?, cancellable: Boolean) {
        state.update { it.copy(work = BackupWorkUi(labelRes, progress, cancellable)) }
    }

    private suspend fun refresh() {
        val savedAt = backup.savedAtMillis()
        state.update { it.copy(savedAtMillis = savedAt) }
    }

    private fun toast(@StringRes messageRes: Int) {
        emit(BackupSettingsEffect.Toast(messageRes))
    }

    private fun emit(effect: BackupSettingsEffect) {
        effectChannel.trySend(effect)
    }

    private fun messageRes(error: Throwable): Int = when (error) {
        is BackupException.Format -> R.string.backup_error_format
        is BackupException.Io -> R.string.backup_error_io
        is BackupException.Rejected -> R.string.backup_error_rejected
        is BackupException.NewerDatabase -> R.string.backup_migrate_schema
        is MigrationException -> when (error.kind) {
            MigrationException.Kind.Unavailable -> R.string.backup_bluetooth_unavailable
            MigrationException.Kind.Permission -> R.string.backup_bluetooth_permission
            MigrationException.Kind.Disabled -> R.string.backup_bluetooth_disabled
            MigrationException.Kind.Connection -> R.string.backup_error_bluetooth
            MigrationException.Kind.Protocol -> R.string.backup_error_protocol
            MigrationException.Kind.Version -> R.string.backup_migrate_version
            MigrationException.Kind.Self -> R.string.backup_migrate_self
            MigrationException.Kind.TooLarge -> R.string.backup_migrate_too_large
            MigrationException.Kind.Schema -> R.string.backup_migrate_schema
            MigrationException.Kind.Hash -> R.string.backup_migrate_hash
            MigrationException.Kind.Import -> R.string.backup_migrate_import
            MigrationException.Kind.Rejected -> R.string.backup_migrate_aborted
            MigrationException.Kind.Timeout -> R.string.backup_migrate_timeout
            MigrationException.Kind.NotFound -> R.string.backup_bluetooth_missing_device
        }
        else -> R.string.backup_error_generic
    }

    private enum class PendingBluetooth {
        Scan,
        Advertise,
    }

    companion object {
        private const val TAG: String = "LedgerBackup"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("BackupSettingsViewModel 需要 Application")
                val entry = EntryPointAccessors.fromApplication(
                    application,
                    LedgerEntryPoint::class.java,
                )
                BackupSettingsViewModel(
                    backup = entry.ledgerBackup(),
                    bluetooth = entry.backupBluetooth(),
                )
            }
        }
    }
}
