package com.cattrack.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.cattrack.app.data.model.ConnectionState
import com.cattrack.app.data.model.DeviceInfo
import com.cattrack.app.data.model.ScannedDevice
import com.cattrack.app.util.BleUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager: BluetoothManager? = try {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    } catch (e: Exception) { null }

    private val bluetoothAdapter: BluetoothAdapter? = try {
        bluetoothManager?.adapter
    } catch (e: Exception) { null }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _currentDevice = MutableStateFlow<DeviceInfo?>(null)
    val currentDevice: StateFlow<DeviceInfo?> = _currentDevice.asStateFlow()

    private val _rawDataFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val rawDataFlow: SharedFlow<ByteArray> = _rawDataFlow.asSharedFlow()

    private val scannedDeviceMap = mutableMapOf<String, ScannedDevice>()
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = try { device.name ?: "CatTrack-${device.address.takeLast(5)}" } catch (e: Exception) { "Unknown" }
            val scannedDevice = ScannedDevice(
                name = name,
                address = device.address,
                rssi = result.rssi
            )
            scannedDeviceMap[device.address] = scannedDevice
            _scannedDevices.value = scannedDeviceMap.values.toList()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(0, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    try { gatt.discoverServices() } catch (e: Exception) { }
                    val device = gatt.device
                    _currentDevice.value = DeviceInfo(
                        deviceId = gatt.device.address,
                        deviceName = try { gatt.device.name ?: "CatTrack" } catch (e: Exception) { "CatTrack" },
                        macAddress = gatt.device.address
                    )
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _currentDevice.value = null
                    try { gatt.close() } catch (e: Exception) { }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try { enableNotifications(gatt) } catch (e: Exception) { }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == BleUuid.BATTERY_LEVEL_CHAR_UUID) {
                try { _batteryLevel.value = characteristic.value?.firstOrNull()?.toInt() ?: 0 } catch (e: Exception) { }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (characteristic.uuid == BleUuid.BATTERY_LEVEL_CHAR_UUID) {
                _batteryLevel.value = value.firstOrNull()?.toInt() ?: 0
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            try { _rawDataFlow.tryEmit(characteristic.value ?: return) } catch (e: Exception) { }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            _rawDataFlow.tryEmit(value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) { }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) { }
    }

    // ---- Public API ----

    fun startScan() {
        if (isScanning || !isBleEnabled()) return
        try {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            scannedDeviceMap.clear()
            _scannedDevices.value = emptyList()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BleUuid.CAT_TRACK_SERVICE_UUID))
                    .build()
            )

            bluetoothLeScanner?.startScan(filters, settings, scanCallback)
            isScanning = true
            _connectionState.value = ConnectionState.SCANNING
        } catch (e: Exception) {
            isScanning = false
        }
    }

    fun stopScan() {
        if (!isScanning) return
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) { }
        isScanning = false
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connect(address: String) {
        stopScan()
        try {
            val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
            _connectionState.value = ConnectionState.CONNECTING
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) { }
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    fun isBleEnabled(): Boolean = try { bluetoothAdapter?.isEnabled == true } catch (e: Exception) { false }

    suspend fun readBatteryLevel(): Int {
        return try {
            val gatt = bluetoothGatt ?: return 0
            val service = gatt.getService(BleUuid.BATTERY_SERVICE_UUID) ?: return 0
            val characteristic = service.getCharacteristic(BleUuid.BATTERY_LEVEL_CHAR_UUID) ?: return 0
            gatt.readCharacteristic(characteristic)
            _batteryLevel.value
        } catch (e: Exception) { 0 }
    }

    fun writeCommand(command: ByteArray) {
        try {
            val gatt = bluetoothGatt ?: return
            val service = gatt.getService(BleUuid.CAT_TRACK_SERVICE_UUID) ?: return
            val characteristic = service.getCharacteristic(BleUuid.COMMAND_WRITE_CHAR_UUID) ?: return
            @Suppress("DEPRECATION")
            characteristic.value = command
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) { }
    }

    // ---- Private Helpers ----

    private fun enableNotifications(gatt: BluetoothGatt) {
        listOf(
            BleUuid.CAT_TRACK_SERVICE_UUID to BleUuid.ACTIVITY_NOTIFY_CHAR_UUID,
            BleUuid.CAT_TRACK_SERVICE_UUID to BleUuid.REALTIME_DATA_CHAR_UUID
        ).forEach { (serviceUuid, charUuid) ->
            try {
                val service = gatt.getService(serviceUuid) ?: return@forEach
                val characteristic = service.getCharacteristic(charUuid) ?: return@forEach
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                ) ?: return@forEach
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            } catch (e: Exception) { }
        }
    }
}
