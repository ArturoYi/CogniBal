package com.example.cognibal.data.backup.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.cognibal.data.backup.VaultTransfer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

enum class BluetoothReadiness {
    Unavailable,
    PermissionRequired,
    Disabled,
    Ready,
}

data class BluetoothPeer(
    val address: String,
    val name: String,
)

data class BluetoothScan(
    val devices: List<BluetoothPeer> = emptyList(),
    val scanning: Boolean = false,
)

/**
 * 备份页上的蓝牙迁移。
 *
 * 接收端按服务 UUID 广播并等待。发送端只扫描带这个 UUID 的设备，连上后读身份特征。
 * 扫描约 60 秒由界面计时；这里负责开始和停下。页面离开前台时，扫描中只停查找，连上之后要 [cancel]。
 */
@SuppressLint("MissingPermission")
@Singleton
class BackupBluetooth @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vault: VaultTransfer,
) {
    private val discovered = LinkedHashMap<String, BluetoothPeer>()
    private val scanState = MutableStateFlow(BluetoothScan())
    private val handler: Handler by lazy { BleLink.handler() }
    private var scanCallback: ScanCallback? = null
    private var active: BleLink? = null

    val scan: StateFlow<BluetoothScan> = scanState

    fun requiredRuntimePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    suspend fun hasEntriesInUse(): Boolean = vault.hasEntriesInUse()

    fun readiness(): BluetoothReadiness {
        val adapter = bluetoothAdapter() ?: return BluetoothReadiness.Unavailable
        if (missingPermission()) return BluetoothReadiness.PermissionRequired
        return try {
            if (adapter.isEnabled) BluetoothReadiness.Ready else BluetoothReadiness.Disabled
        } catch (_: SecurityException) {
            BluetoothReadiness.PermissionRequired
        }
    }

    fun startDiscovery() {
        stopScan()
        ensureReady()
        discovered.clear()
        publish(scanning = true)
        val scanner = bluetoothAdapter()?.bluetoothLeScanner
            ?: throw MigrationException(MigrationException.Kind.Unavailable, "scan", "这台设备不能扫描蓝牙")
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(MigrationProtocol.SERVICE_UUID))
                .build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (scanCallback !== this) return
                val device = result.device ?: return
                val address = try {
                    device.address
                } catch (_: SecurityException) {
                    return
                } ?: return
                val label = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
                    ?: try {
                        device.name?.takeIf { it.isNotBlank() }
                    } catch (_: SecurityException) {
                        null
                    }
                    ?: address
                if (scanCallback !== this) return
                discovered[address] = BluetoothPeer(address, label)
                publish(scanning = true)
            }

            override fun onScanFailed(errorCode: Int) {
                if (scanCallback !== this) return
                publish(scanning = false)
            }
        }
        try {
            scanner.startScan(filters, settings, callback)
            scanCallback = callback
        } catch (exception: SecurityException) {
            publish(scanning = false)
            throw MigrationException(MigrationException.Kind.Permission, "scan", "没有蓝牙权限", exception)
        }
    }

    /** 停止查找，但留下已经扫到的设备，方便超时后点选或重扫。 */
    fun stopScan() {
        val callback = scanCallback
        scanCallback = null
        if (callback != null) {
            try {
                bluetoothAdapter()?.bluetoothLeScanner?.stopScan(callback)
            } catch (_: SecurityException) {
            }
        }
        publish(scanning = false)
    }

    fun stopDiscovery() {
        stopScan()
        discovered.clear()
        publish(scanning = false)
    }

    fun cancel() {
        active?.close()
        active = null
    }

    fun stop() {
        stopDiscovery()
        cancel()
    }

    suspend fun send(address: String, callbacks: MigrationCallbacks) = withContext(Dispatchers.IO) {
        runLink(callbacks) { identity ->
            val adapter = bluetoothAdapter()
                ?: throw MigrationException(MigrationException.Kind.Unavailable, "connect", "这台设备没有蓝牙")
            if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                throw MigrationException(MigrationException.Kind.NotFound, "connect", "找不到这台设备")
            }
            stopScan()
            val device = try {
                adapter.getRemoteDevice(address)
            } catch (_: IllegalArgumentException) {
                throw MigrationException(MigrationException.Kind.NotFound, "connect", "找不到这台设备")
            }
            val link = BleLink.connect(context, handler, identity, device)
            track(link)
            try {
                session(callbacks, identity).runSender(link)
            } finally {
                release(link)
            }
        }
    }

    suspend fun receive(callbacks: MigrationCallbacks) = withContext(Dispatchers.IO) {
        runLink(callbacks) { identity ->
            val link = BleLink.accept(context, handler, identity)
            track(link)
            try {
                session(callbacks, identity).runReceiver(link)
            } finally {
                release(link)
            }
        }
    }

    private suspend fun runLink(callbacks: MigrationCallbacks, block: suspend (ByteArray) -> Unit) {
        ensureReady()
        try {
            block(localIdentity())
        } catch (exception: MigrationException) {
            throw exception
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SecurityException) {
            throw MigrationException(MigrationException.Kind.Permission, "connect", "没有蓝牙权限", exception)
        } catch (exception: Exception) {
            throw MigrationException(MigrationException.Kind.Connection, "connect", "蓝牙没有连上", exception)
        }
    }

    private fun session(callbacks: MigrationCallbacks, identity: ByteArray): MigrationSession {
        val directory = File(context.cacheDir, "migration")
        if (!directory.exists()) directory.mkdirs()
        return MigrationSession(
            vault = vault,
            localIdentity = identity,
            gate = callbacks.asGate(),
            workDirectory = directory,
        )
    }

    private fun MigrationCallbacks.asGate(): MigrationGate {
        return object : MigrationGate {
            override fun onConnected() = this@asGate.onConnected()

            override fun onCode(digits: String) = this@asGate.onCode(digits)

            override suspend fun confirmCode(): Boolean = this@asGate.confirmCode()

            override fun onWaitReplace() = this@asGate.onWaitReplace()

            override suspend fun confirmReplace(): Boolean = this@asGate.confirmReplace()

            override fun onTransfer() = this@asGate.onTransfer()

            override fun onProgress(sent: Long, total: Long) = this@asGate.onProgress(sent, total)
        }
    }

    private fun track(link: BleLink) {
        active?.close()
        active = link
    }

    private fun release(link: BleLink) {
        link.close()
        if (active === link) active = null
    }

    private fun localIdentity(): ByteArray {
        val file = File(context.noBackupFilesDir, "migration-device-id")
        val existing = if (file.isFile) file.readBytes() else ByteArray(0)
        val deviceId = if (existing.size == MigrationProtocol.DEVICE_ID_BYTES) {
            existing
        } else {
            MigrationCrypto.randomBytes(MigrationProtocol.DEVICE_ID_BYTES).also { created ->
                file.parentFile?.mkdirs()
                file.writeBytes(created)
            }
        }
        return MigrationProtocol.identity(deviceId)
    }

    private fun ensureReady() {
        when (readiness()) {
            BluetoothReadiness.Unavailable ->
                throw MigrationException(MigrationException.Kind.Unavailable, "connect", "这台设备没有蓝牙")
            BluetoothReadiness.PermissionRequired ->
                throw MigrationException(MigrationException.Kind.Permission, "connect", "没有蓝牙权限")
            BluetoothReadiness.Disabled ->
                throw MigrationException(MigrationException.Kind.Disabled, "connect", "蓝牙没有打开")
            BluetoothReadiness.Ready -> Unit
        }
    }

    private fun publish(scanning: Boolean) {
        scanState.value = BluetoothScan(
            devices = discovered.values.sortedBy { it.name.lowercase() },
            scanning = scanning,
        )
    }

    private fun bluetoothAdapter(): BluetoothAdapter? {
        return context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    private fun missingPermission(): Boolean {
        return requiredRuntimePermissions().any { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}

interface MigrationCallbacks {
    fun onConnected()

    fun onCode(digits: String)

    suspend fun confirmCode(): Boolean

    fun onWaitReplace()

    suspend fun confirmReplace(): Boolean

    fun onTransfer()

    fun onProgress(sent: Long, total: Long)
}
