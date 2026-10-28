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
import com.example.cognibal.data.backup.bluetooth.MigrationProtocol
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
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
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
    private var toldFound = false
    private val searchGeneration = AtomicInteger(0)
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
            BackupSettingsUiEvent.CodeMatches -> {
                if (codeDeferred?.complete(true) != true) return
                state.update { current ->
                    val code = current.migration as? MigrationUi.Code ?: return@update current
                    current.copy(migration = code.copy(waitingForPeer = true))
                }
                toast(R.string.backup_migrate_code_wait, long = true)
            }
            BackupSettingsUiEvent.CodeDiffers -> codeDeferred?.complete(false)
            BackupSettingsUiEvent.ConfirmReplace -> {
                if (replaceDeferred?.complete(true) != true) return
                state.update {
                    it.copy(
                        migration = MigrationUi.Transfer(
                            labelRes = R.string.backup_migrate_replace_accepted,
                            progress = null,
                        ),
                    )
                }
                toast(R.string.backup_migrate_replace_accepted, long = true)
            }
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
        searchGeneration.incrementAndGet()
        toldFound = false
        state.update {
            it.copy(
                migration = MigrationUi.Scan(
                    devices = emptyList(),
                    scanning = true,
                    paused = false,
                    secondsLeft = searchSeconds,
                ),
            )
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
        toast(R.string.backup_migrate_scan_started, long = true)
    }

    private fun beginAdvertise() {
        if (!running.compareAndSet(false, true)) return
        searchGeneration.incrementAndGet()
        failureShown = false
        stopSearchOnly = false
        engaged = false
        state.update {
            it.copy(
                migration = MigrationUi.Receive(
                    waiting = true,
                    paused = false,
                    secondsLeft = searchSeconds,
                    timedOut = false,
                ),
            )
        }
        sessionJob = viewModelScope.launch {
            val job = currentCoroutineContext()[Job]
            try {
                bluetooth.receive(callbacks(MigrationRole.Receive))
                if (sessionJob !== job) return@launch
                toast(R.string.backup_migrate_received)
                state.update { it.copy(migration = null) }
                emit(BackupSettingsEffect.RestartApp)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (!failureShown && !stopSearchOnly && sessionJob === job) {
                    Log.w(TAG, "蓝牙接收没有完成", exception)
                    showFailure(exception, MigrationRole.Receive)
                }
            } finally {
                if (sessionJob === job) {
                    running.set(false)
                    engaged = false
                }
            }
        }
        armReceiveTimer()
        toast(R.string.backup_migrate_receive_started, long = true)
    }

    private fun rescan() {
        if (running.get() || state.value.migration !is MigrationUi.Scan) return
        searchGeneration.incrementAndGet()
        toldFound = false
        state.update { current ->
            val scan = current.migration as? MigrationUi.Scan ?: return@update current
            current.copy(
                migration = scan.copy(
                    devices = emptyList(),
                    scanning = true,
                    paused = false,
                    secondsLeft = searchSeconds,
                ),
            )
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
        toast(R.string.backup_migrate_scan_started, long = true)
    }

    private fun selectPeer(address: String) {
        if (!running.compareAndSet(false, true)) return
        searchGeneration.incrementAndGet()
        scanTimer?.cancel()
        bluetooth.stopScan()
        failureShown = false
        stopSearchOnly = false
        engaged = false
        role = MigrationRole.Send
        state.update { it.copy(migration = MigrationUi.Connecting) }
        sessionJob = viewModelScope.launch {
            val job = currentCoroutineContext()[Job]
            try {
                bluetooth.send(address, callbacks(MigrationRole.Send))
                if (sessionJob !== job) return@launch
                toast(R.string.backup_migrate_sent)
                state.update { it.copy(migration = null) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (!failureShown && !stopSearchOnly && sessionJob === job) {
                    Log.w(TAG, "蓝牙发送没有完成", exception)
                    showFailure(exception, MigrationRole.Send)
                }
            } finally {
                if (sessionJob === job) {
                    running.set(false)
                    engaged = false
                }
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
            is MigrationUi.Scan -> {
                val scanning = current.scanning
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = MigrationUi.Role) }
                if (scanning) toast(R.string.backup_migrate_stopped)
            }
            is MigrationUi.Receive -> {
                val waiting = current.waiting
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = MigrationUi.Role) }
                if (waiting) toast(R.string.backup_migrate_stopped)
            }
            else -> abortConnected()
        }
    }

    private fun onForegroundLost() {
        if (expectingSystemUi) return
        when (val current = state.value.migration) {
            is MigrationUi.Scan -> {
                if (!current.scanning) return
                searchGeneration.incrementAndGet()
                bluetooth.stopScan()
                scanTimer?.cancel()
                state.update { it.copy(migration = current.copy(scanning = false, paused = true)) }
                toast(R.string.backup_migrate_paused, long = true)
            }
            is MigrationUi.Receive -> if (current.waiting) {
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = current.copy(waiting = false, paused = true)) }
                toast(R.string.backup_migrate_paused, long = true)
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
            is MigrationUi.Scan -> {
                val scanning = current.scanning
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = null) }
                if (scanning) toast(R.string.backup_migrate_stopped)
            }
            is MigrationUi.Receive -> {
                val waiting = current.waiting
                stopSearchOnly = true
                pauseSearch()
                state.update { it.copy(migration = null) }
                if (waiting) toast(R.string.backup_migrate_stopped)
            }
            else -> abortConnected()
        }
    }

    private fun pauseSearch(cancelTimer: Boolean = true) {
        searchGeneration.incrementAndGet()
        if (cancelTimer) scanTimer?.cancel()
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
        scanTimer?.cancel()
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
        toast(R.string.backup_migrate_aborted)
        bluetooth.cancel()
        sessionJob?.cancel()
    }

    private fun callbacks(owner: MigrationRole): MigrationCallbacks {
        return object : MigrationCallbacks {
            override fun onConnected() {
                engaged = true
                searchGeneration.incrementAndGet()
                scanTimer?.cancel()
                state.update { it.copy(migration = MigrationUi.Connecting) }
            }

            override fun onCode(digits: String) {
                codeDeferred = CompletableDeferred()
                state.update {
                    it.copy(
                        migration = MigrationUi.Code(
                            digits = digits,
                            secondsLeft = confirmSeconds,
                            waitingForPeer = false,
                        ),
                    )
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
                    toast(R.string.backup_migrate_replace_prompt, long = true)
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
                toast(R.string.backup_migrate_transfer_started, long = true)
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
            for (left in confirmSeconds downTo 0) {
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
                val devices = scan.devices.map { peer -> BluetoothPeerUi(peer.address, peer.name) }
                state.update { current ->
                    val ui = current.migration as? MigrationUi.Scan ?: return@update current
                    // 倒计时结束后迟到的扫描结果不再把界面拉回“正在扫描”。
                    val scanning = scan.scanning && !ui.paused && (ui.scanning || ui.secondsLeft > 0)
                    current.copy(
                        migration = ui.copy(
                            devices = devices,
                            scanning = scanning,
                        ),
                    )
                }
                if (!toldFound && devices.isNotEmpty() && state.value.migration is MigrationUi.Scan) {
                    toldFound = true
                    toast(R.string.backup_migrate_found, long = true)
                }
            }
        }
    }

    private fun armScanTimer() {
        val generation = searchGeneration.get()
        scanTimer?.cancel()
        scanTimer = viewModelScope.launch {
            var left = searchSeconds
            while (left > 0) {
                if (generation != searchGeneration.get()) return@launch
                val scan = state.value.migration as? MigrationUi.Scan
                if (scan == null || scan.paused || !scan.scanning) return@launch
                state.update { current ->
                    val latest = current.migration as? MigrationUi.Scan ?: return@update current
                    if (!latest.scanning || latest.paused) return@update current
                    current.copy(migration = latest.copy(secondsLeft = left))
                }
                delay(1_000)
                left -= 1
            }
            if (generation != searchGeneration.get()) return@launch
            val scan = state.value.migration as? MigrationUi.Scan ?: return@launch
            if (scan.paused || !scan.scanning) return@launch
            bluetooth.stopScan()
            state.update { current ->
                if (generation != searchGeneration.get()) return@update current
                val latest = current.migration as? MigrationUi.Scan ?: return@update current
                if (latest.paused) return@update current
                current.copy(migration = latest.copy(scanning = false, secondsLeft = 0))
            }
            if (
                generation == searchGeneration.get() &&
                (state.value.migration as? MigrationUi.Scan)?.secondsLeft == 0
            ) {
                toast(R.string.backup_migrate_scan_done, long = true)
            }
        }
    }

    /** 接收端和扫描一样只等一轮。到时停下广播，页面上可以再点一次。 */
    private fun armReceiveTimer() {
        val generation = searchGeneration.get()
        scanTimer?.cancel()
        scanTimer = viewModelScope.launch {
            var left = searchSeconds
            while (left > 0) {
                if (generation != searchGeneration.get() || engaged) return@launch
                val receive = state.value.migration as? MigrationUi.Receive
                if (receive == null || !receive.waiting || receive.paused) return@launch
                state.update { current ->
                    val latest = current.migration as? MigrationUi.Receive ?: return@update current
                    if (!latest.waiting || latest.paused || engaged) return@update current
                    current.copy(migration = latest.copy(secondsLeft = left))
                }
                delay(1_000)
                left -= 1
            }
            if (generation != searchGeneration.get() || engaged) return@launch
            state.update { current ->
                if (generation != searchGeneration.get() || engaged) return@update current
                val latest = current.migration as? MigrationUi.Receive ?: return@update current
                if (!latest.waiting || latest.paused) return@update current
                current.copy(
                    migration = latest.copy(
                        waiting = false,
                        paused = false,
                        secondsLeft = 0,
                        timedOut = true,
                    ),
                )
            }
            val expired = (state.value.migration as? MigrationUi.Receive)?.timedOut == true
            if (!expired || generation != searchGeneration.get() || engaged) return@launch
            stopSearchOnly = true
            val job = sessionJob
            bluetooth.cancel()
            job?.cancel()
            if ((state.value.migration as? MigrationUi.Receive)?.timedOut == true) {
                toast(R.string.backup_migrate_wait_timeout, long = true)
            }
        }
    }

    private fun showFailure(error: Throwable, owner: MigrationRole) {
        failureShown = true
        searchGeneration.incrementAndGet()
        ticker?.cancel()
        scanTimer?.cancel()
        state.update {
            it.copy(
                migration = MigrationUi.Failed(
                    messageRes = messageRes(error),
                    diagnostic = diagnostic(error, owner),
                    role = owner,
                ),
            )
        }
        toast(messageRes(error))
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

    private fun toast(@StringRes messageRes: Int, long: Boolean = false) {
        emit(BackupSettingsEffect.Toast(messageRes, long))
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
        private val searchSeconds: Int = (MigrationProtocol.SCAN_MILLIS / 1_000L).toInt()
        private val confirmSeconds: Int = (MigrationProtocol.CONFIRM_MILLIS / 1_000L).toInt()

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
