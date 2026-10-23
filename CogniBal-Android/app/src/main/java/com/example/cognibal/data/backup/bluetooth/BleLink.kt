package com.example.cognibal.data.backup.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 一条已经连上的 BLE 链路。接收端广播并当外围设备，发送端扫描后作为中心设备连接。
 * 控制帧和文件字节走不同特征，避免确认和数据缠在一起。
 */
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
internal class BleLink private constructor(
    private val context: Context,
    private val handler: Handler,
    private val identity: ByteArray,
    private val central: Boolean,
) {
    private val operationLock = Mutex()
    private val closed = AtomicBoolean(false)
    private val controlChannel = Channel<MigrationFrame>(Channel.BUFFERED)
    private val dataChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val connected = CompletableDeferred<Unit>()
    private val frameBuffer = FrameBuffer { body ->
        try {
            controlChannel.trySend(MigrationProtocol.decode(body))
        } catch (exception: MigrationException) {
            controlChannel.close(exception)
        }
    }
    private var pending: CompletableDeferred<Any?>? = null
    private var notifyWait: CompletableDeferred<Unit>? = null
    private var gatt: BluetoothGatt? = null
    private var server: BluetoothGattServer? = null
    private var remote: BluetoothDevice? = null
    private var control: BluetoothGattCharacteristic? = null
    private var data: BluetoothGattCharacteristic? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var mtu: Int = 23

    suspend fun remoteIdentity(): ByteArray {
        val characteristic = controlServiceCharacteristic(MigrationProtocol.IDENTITY_UUID)
        val value = awaitOp { gatt?.readCharacteristic(characteristic) == true }
        val bytes = value as? ByteArray ?: throw connection("没有读到身份特征")
        return bytes
    }

    suspend fun send(frame: MigrationFrame) {
        throwIfClosed()
        val bytes = MigrationProtocol.encode(frame)
        if (central) {
            val characteristic = control ?: throw connection("控制通道还没准备好")
            writeChunks(characteristic, bytes)
        } else {
            notifyChunks(bytes)
        }
    }

    suspend fun receive(): MigrationFrame {
        throwIfClosed()
        return try {
            controlChannel.receive()
        } catch (exception: MigrationException) {
            throw exception
        } catch (exception: Exception) {
            val cause = exception.cause
            if (cause is MigrationException) throw cause
            throwIfClosed()
            throw connection("控制通道已断开", exception)
        }
    }

    suspend fun writeData(bytes: ByteArray, length: Int) {
        val characteristic = data ?: throw connection("数据通道还没准备好")
        writeChunks(characteristic, bytes.copyOf(length))
    }

    suspend fun receiveData(file: File, size: Long, onWindow: suspend (Long) -> Unit) {
        FileOutputStream(file).use { output ->
            var got = 0L
            var sinceAck = 0L
            while (got < size) {
                val chunk = try {
                    dataChannel.receive()
                } catch (exception: Exception) {
                    throwIfClosed()
                    throw connection("数据通道已断开", exception)
                }
                val room = (size - got).toInt()
                val take = minOf(chunk.size, room)
                if (take > 0) {
                    output.write(chunk, 0, take)
                    got += take
                    sinceAck += take
                }
                if (sinceAck >= MigrationProtocol.WINDOW_BYTES || got == size) {
                    output.flush()
                    onWindow(got)
                    sinceAck = 0L
                }
            }
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        controlChannel.close()
        dataChannel.close()
        if (!connected.isCompleted) {
            connected.completeExceptionally(connection("连接已关闭"))
        }
        pending?.completeExceptionally(connection("连接已关闭"))
        notifyWait?.completeExceptionally(connection("连接已关闭"))
        stopAdvertising()
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }
        try {
            server?.close()
        } catch (_: Exception) {
        }
        gatt = null
        server = null
    }

    private suspend fun connect(device: BluetoothDevice) {
        val callback = clientCallback()
        val socket = device.connectGatt(
            context,
            false,
            callback,
            BluetoothDevice.TRANSPORT_LE,
            BluetoothDevice.PHY_LE_1M_MASK,
            handler,
        )
            ?: throw connection("无法连接这台设备")
        gatt = socket
        try {
            withTimeout(CONNECT_TIMEOUT) { awaitConnected() }
        } catch (exception: TimeoutCancellationException) {
            throw connection("连接超时", exception)
        }
        requestHighThroughput(socket)
        awaitOp { socket.discoverServices() }
        val service = socket.getService(MigrationProtocol.SERVICE_UUID)
            ?: throw MigrationException(MigrationException.Kind.Protocol, "identity", "对方不是本应用")
        control = service.getCharacteristic(MigrationProtocol.CONTROL_UUID)
        data = service.getCharacteristic(MigrationProtocol.DATA_UUID)
        if (control == null || data == null || service.getCharacteristic(MigrationProtocol.IDENTITY_UUID) == null) {
            throw MigrationException(MigrationException.Kind.Protocol, "identity", "对方缺少迁移特征")
        }
        try {
            awaitOp { socket.requestMtu(517) }
        } catch (_: Exception) {
            // MTU 维持默认时，后面的写入会按 20 字节拆开。
        }
        enableNotifications(socket, requireNotNull(control))
    }

    private fun appendControl(value: ByteArray) {
        try {
            frameBuffer.append(value)
        } catch (exception: MigrationException) {
            failLink(exception)
        }
    }

    private suspend fun serve() {
        val manager = bluetoothManager()
        val opened = manager.openGattServer(context, serverCallback())
            ?: throw MigrationException(MigrationException.Kind.Unavailable, "connect", "这台设备不能接收蓝牙连接")
        server = opened
        val service = BluetoothGattService(
            MigrationProtocol.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        val identityCharacteristic = BluetoothGattCharacteristic(
            MigrationProtocol.IDENTITY_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        val controlCharacteristic = BluetoothGattCharacteristic(
            MigrationProtocol.CONTROL_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        controlCharacteristic.addDescriptor(cccd())
        val dataCharacteristic = BluetoothGattCharacteristic(
            MigrationProtocol.DATA_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE,
        )
        service.addCharacteristic(identityCharacteristic)
        service.addCharacteristic(controlCharacteristic)
        service.addCharacteristic(dataCharacteristic)
        control = controlCharacteristic
        data = dataCharacteristic
        awaitOp { opened.addService(service) }
        startAdvertising()
        try {
            awaitConnected()
        } finally {
            stopAdvertising()
        }
    }

    private suspend fun awaitConnected() {
        suspendCancellableCoroutine { continuation ->
            connected.invokeOnCompletion { error ->
                if (!continuation.isActive) return@invokeOnCompletion
                if (error == null) continuation.resume(Unit) else continuation.resumeWithException(error)
            }
            continuation.invokeOnCancellation { close() }
        }
    }

    private suspend fun enableNotifications(socket: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (!socket.setCharacteristicNotification(characteristic, true)) {
            throw connection("无法订阅迁移通知")
        }
        val descriptor = characteristic.getDescriptor(cccdUuid()) ?: throw connection("缺少通知描述符")
        val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        awaitOp {
            writeDescriptor(socket, descriptor, value)
        }
    }

    private suspend fun writeChunks(characteristic: BluetoothGattCharacteristic, bytes: ByteArray) {
        for (chunk in slices(bytes)) {
            awaitOp { writeCharacteristic(characteristic, chunk) }
        }
    }

    private suspend fun notifyChunks(bytes: ByteArray) {
        val device = remote ?: throw connection("对方已经断开")
        val characteristic = control ?: throw connection("控制通道还没准备好")
        val opened = server ?: throw connection("接收端已经关闭")
        for (chunk in slices(bytes)) {
            val waiter = CompletableDeferred<Unit>()
            notifyWait = waiter
            val started = notify(opened, device, characteristic, chunk)
            if (!started) throw connection("无法把确认发给对方")
            try {
                withTimeout(OPERATION_TIMEOUT) { waiter.await() }
            } finally {
                if (notifyWait === waiter) notifyWait = null
            }
        }
    }

    private suspend fun awaitOp(start: () -> Boolean): Any? {
        throwIfClosed()
        return operationLock.withLock {
            throwIfClosed()
            val waiter = CompletableDeferred<Any?>()
            pending = waiter
            try {
                if (!start()) throw connection("无法发起蓝牙操作")
                withTimeout(OPERATION_TIMEOUT) { waiter.await() }
            } catch (exception: TimeoutCancellationException) {
                throw connection("蓝牙操作超时", exception)
            } finally {
                if (pending === waiter) pending = null
            }
        }
    }

    private fun clientCallback(): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    if (!connected.isCompleted) connected.complete(Unit)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    failLink(connection("蓝牙连接已断开（$status）"))
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) pending?.complete(Unit) else failPending(status)
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this@BleLink.mtu = mtu.coerceAtLeast(23)
                }
                pending?.complete(Unit)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    pending?.complete(value.copyOf())
                } else {
                    failPending(status)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    pending?.complete(characteristic.value?.copyOf())
                } else {
                    failPending(status)
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) pending?.complete(Unit) else failPending(status)
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) pending?.complete(Unit) else failPending(status)
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
            ) {
                if (characteristic.uuid == MigrationProtocol.CONTROL_UUID) appendControl(value)
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
                if (characteristic.uuid == MigrationProtocol.CONTROL_UUID) {
                    appendControl(characteristic.value ?: return)
                }
            }
        }
    }

    private fun serverCallback(): BluetoothGattServerCallback {
        return object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                handler.post {
                    if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                        if (remote == null) {
                            remote = device
                            if (!connected.isCompleted) connected.complete(Unit)
                        } else if (device.address != remote?.address) {
                            server?.cancelConnection(device)
                        }
                    } else if (device.address == remote?.address && newState == BluetoothProfile.STATE_DISCONNECTED) {
                        failLink(connection("蓝牙连接已断开（$status）"))
                    }
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                handler.post {
                    if (status == BluetoothGatt.GATT_SUCCESS) pending?.complete(Unit) else failPending(status)
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic,
            ) {
                val value = if (characteristic.uuid == MigrationProtocol.IDENTITY_UUID) identity else ByteArray(0)
                if (offset < 0 || offset > value.size) {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
                    return
                }
                server?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value.copyOfRange(offset, value.size),
                )
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (responseNeeded) {
                    val status = if (offset == 0) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_INVALID_OFFSET
                    server?.sendResponse(device, requestId, status, offset, null)
                }
                if (offset != 0) return
                handler.post {
                    when (characteristic.uuid) {
                        MigrationProtocol.CONTROL_UUID -> appendControl(value)
                        MigrationProtocol.DATA_UUID -> dataChannel.trySend(value.copyOf())
                    }
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                if (responseNeeded) {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                handler.post {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyWait?.complete(Unit)
                    } else {
                        notifyWait?.completeExceptionally(connection("通知没有送达（$status）"))
                    }
                }
            }

            override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
                handler.post { this@BleLink.mtu = mtu.coerceAtLeast(23) }
            }
        }
    }

    private fun startAdvertising() {
        val advertiser = bluetoothManager().adapter?.bluetoothLeAdvertiser
            ?: throw MigrationException(MigrationException.Kind.Unavailable, "connect", "这台设备不能被发现")
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(android.os.ParcelUuid(MigrationProtocol.SERVICE_UUID))
            .build()
        val started = CompletableDeferred<Unit>()
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                if (!started.isCompleted) started.complete(Unit)
            }

            override fun onStartFailure(errorCode: Int) {
                if (!started.isCompleted) {
                    started.completeExceptionally(connection("无法开始广播（$errorCode）"))
                }
            }
        }
        advertiseCallback = callback
        try {
            advertiser.startAdvertising(settings, data, callback)
        } catch (exception: SecurityException) {
            throw MigrationException(MigrationException.Kind.Permission, "connect", "没有蓝牙权限", exception)
        }
        // 广播是否成功由后面的连接超时兜底；这里不等回调，避免和 GATT 队列互相等待。
        started.invokeOnCompletion { error ->
            if (error != null && !closed.get()) failLink(error as? MigrationException ?: connection("无法开始广播"))
        }
    }

    private fun stopAdvertising() {
        val callback = advertiseCallback ?: return
        advertiseCallback = null
        try {
            bluetoothManager().adapter?.bluetoothLeAdvertiser?.stopAdvertising(callback)
        } catch (_: Exception) {
        }
    }

    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        val socket = gatt ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            socket.writeCharacteristic(
                characteristic,
                value,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = value
            socket.writeCharacteristic(characteristic)
        }
    }

    private fun writeDescriptor(socket: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            socket.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = value
            socket.writeDescriptor(descriptor)
        }
    }

    private fun notify(
        opened: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            opened.notifyCharacteristicChanged(device, characteristic, false, value) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = value
            opened.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private fun controlServiceCharacteristic(uuid: UUID): BluetoothGattCharacteristic {
        return gatt?.getService(MigrationProtocol.SERVICE_UUID)?.getCharacteristic(uuid)
            ?: throw MigrationException(MigrationException.Kind.Protocol, "identity", "对方不是本应用")
    }

    private fun slices(bytes: ByteArray): List<ByteArray> {
        if (bytes.isEmpty()) return emptyList()
        val size = (mtu - 3).coerceIn(20, 509)
        val parts = ArrayList<ByteArray>()
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + size, bytes.size)
            parts.add(bytes.copyOfRange(offset, end))
            offset = end
        }
        return parts
    }

    private fun failPending(status: Int) {
        pending?.completeExceptionally(connection("蓝牙操作失败（$status）"))
    }

    private fun failLink(error: MigrationException) {
        if (!connected.isCompleted) connected.completeExceptionally(error)
        pending?.completeExceptionally(error)
        notifyWait?.completeExceptionally(error)
        controlChannel.close(error)
        dataChannel.close(error)
    }

    private fun throwIfClosed() {
        if (closed.get()) throw connection("连接已关闭")
    }

    private fun requestHighThroughput(socket: BluetoothGatt) {
        try {
            socket.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
            socket.setPreferredPhy(
                BluetoothDevice.PHY_LE_2M_MASK,
                BluetoothDevice.PHY_LE_2M_MASK,
                BluetoothDevice.PHY_OPTION_NO_PREFERRED,
            )
        } catch (_: Exception) {
        }
    }

    private fun bluetoothManager(): BluetoothManager {
        return context.getSystemService(BluetoothManager::class.java)
            ?: throw MigrationException(MigrationException.Kind.Unavailable, "connect", "这台设备没有蓝牙")
    }

    private fun connection(message: String, cause: Throwable? = null): MigrationException {
        return MigrationException(MigrationException.Kind.Connection, "connect", message, cause)
    }

    companion object {
        private const val CONNECT_TIMEOUT: Long = 20_000L
        private const val OPERATION_TIMEOUT: Long = 20_000L

        suspend fun connect(
            context: Context,
            handler: Handler,
            identity: ByteArray,
            device: BluetoothDevice,
        ): BleLink {
            val link = BleLink(context, handler, identity, central = true)
            try {
                link.connect(device)
            } catch (exception: Exception) {
                link.close()
                throw exception
            }
            return link
        }

        suspend fun accept(context: Context, handler: Handler, identity: ByteArray): BleLink {
            val link = BleLink(context, handler, identity, central = false)
            try {
                link.serve()
            } catch (exception: Exception) {
                link.close()
                throw exception
            }
            return link
        }

        fun handler(): Handler {
            val thread = HandlerThread("cognibal-migrate")
            thread.start()
            return Handler(thread.looper)
        }
    }
}

private fun cccd(): BluetoothGattDescriptor {
    return BluetoothGattDescriptor(
        cccdUuid(),
        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
    )
}

private fun cccdUuid(): UUID = UUID.fromString(MigrationProtocol.CCCD_UUID)

private class FrameBuffer(
    private val onFrame: (ByteArray) -> Unit,
) {
    private val pending = ByteArrayOutputStream()
    private var expected = -1

    fun append(value: ByteArray) {
        if (value.isEmpty()) return
        pending.write(value)
        drain()
    }

    private fun drain() {
        while (true) {
            val bytes = pending.toByteArray()
            if (expected < 0) {
                if (bytes.size < 4) return
                val length = java.nio.ByteBuffer.wrap(bytes, 0, 4).int
                if (length <= 0 || length > MigrationProtocol.MAX_FRAME) {
                    throw MigrationException(MigrationException.Kind.Protocol, "transfer", "控制帧过长")
                }
                expected = length
            }
            if (bytes.size < 4 + expected) return
            val body = bytes.copyOfRange(4, 4 + expected)
            val rest = bytes.copyOfRange(4 + expected, bytes.size)
            pending.reset()
            if (rest.isNotEmpty()) pending.write(rest)
            val length = expected
            expected = -1
            onFrame(body)
            if (length == 0) return
        }
    }
}
