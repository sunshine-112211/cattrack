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
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
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

    // ---- BLE Scanner Callback ----
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Device"
            if (!name.contains("CatTrack", ignoreCase = true) &&
                !name.contains("CT-", ignoreCase = true)
            ) return

            val scanned = ScannedDevice(
                name = name,
                address = device.address,
                rssi = result.rssi,
                isConnectable = result.isConnectable
            )
            scannedDeviceMap[device.address] = scanned
            _scannedDevices.value = scannedDeviceMap.values.toList()
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = ConnectionState.ERROR
        }
    }

    // ---- GATT Callback ----
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt.discoverServices()
                    updateCurrentDevice(gatt.device)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    bluetoothGatt = null
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(gatt)
                readBatteryCharacteristic(gatt)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(characteristic.uuid, value)
            }
        }

        @Deprecated("Deprecated for API < 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicData(characteristic.uuid, value)
        }

        @Deprecated("Deprecated for API < 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicData(characteristic.uuid, characteristic.value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            // Descriptor write completed, notification enabled
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            // MTU updated
        }
    }

    // ---- Public API ----

    fun startScan() {
        if (isScanning || !isBleEnabled()) return
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
    }

    fun stopScan() {
        if (!isScanning) return
        bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connect(address: String) {
        stopScan()
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        _connectionState.value = ConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    fun isBleEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    suspend fun readBatteryLevel(): Int {
        val gatt = bluetoothGatt ?: return 0
        val service = gatt.getService(BleUuid.BATTERY_SERVICE_UUID) ?: return 0
        val characteristic = service.getCharacteristic(BleUuid.BATTERY_LEVEL_CHAR_UUID) ?: return 0
        gatt.readCharacteristic(characteristic)
        return _batteryLevel.value
    }

    fun writeCommand(command: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(BleUuid.CAT_TRACK_SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(BleUuid.COMMAND_WRITE_CHAR_UUID) ?: return
        characteristic.value = command
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gatt.writeCharacteristic(characteristic)
    }

    // ---- Private Helpers ----

    private fun enableNotifications(gatt: BluetoothGatt) {
        listOf(
            BleUuid.CAT_TRACK_SERVICE_UUID to BleUuid.ACTIVITY_NOTIFY_CHAR_UUID,
            BleUuid.CAT_TRACK_SERVICE_UUID to BleUuid.REALTIME_DATA_CHAR_UUID
        ).forEach { (serviceUuid, charUuid) ->
            val service = gatt.getService(serviceUuid) ?: return@forEach
            val characteristic = service.getCharacteristic(charUuid) ?: return@forEach
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(BleUuid.CLIENT_CHARACTERISTIC_CONFIG_UUID)
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }
    }

    private fun readBatteryCharacteristic(gatt: BluetoothGatt) {
        val service = gatt.getService(BleUuid.BATTERY_SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(BleUuid.BATTERY_LEVEL_CHAR_UUID) ?: return
        gatt.readCharacteristic(characteristic)
    }

    private fun handleCharacteristicData(uuid: UUID, data: ByteArray) {
        when (uuid) {
            BleUuid.BATTERY_LEVEL_CHAR_UUID -> {
                if (data.isNotEmpty()) {
                    _batteryLevel.value = data[0].toInt() and 0xFF
                }
            }
            BleUuid.ACTIVITY_NOTIFY_CHAR_UUID,
            BleUuid.REALTIME_DATA_CHAR_UUID -> {
                _rawDataFlow.tryEmit(data)
            }
        }
    }

    private fun updateCurrentDevice(device: BluetoothDevice) {
        _currentDevice.value = DeviceInfo(
            deviceId = device.address,
            deviceName = device.name ?: "CatTrack Device",
            macAddress = device.address,
            connectionState = ConnectionState.CONNECTED
        )
    }
}
