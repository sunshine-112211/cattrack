package com.cattrack.app.data.repository

import com.cattrack.app.bluetooth.BleManager
import com.cattrack.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val bleManager: BleManager
) {
    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val scannedDevices: StateFlow<List<ScannedDevice>> = bleManager.scannedDevices
    val batteryLevel: StateFlow<Int> = bleManager.batteryLevel
    val currentDevice: StateFlow<DeviceInfo?> = bleManager.currentDevice
    val rawDataFlow: Flow<ByteArray> = bleManager.rawDataFlow

    fun startScan() = bleManager.startScan()

    fun stopScan() = bleManager.stopScan()

    suspend fun connectDevice(address: String) = bleManager.connect(address)

    fun disconnect() = bleManager.disconnect()

    suspend fun readBattery(): Int = bleManager.readBatteryLevel()

    fun isConnected(): Boolean = bleManager.isConnected()

    fun isBleEnabled(): Boolean = bleManager.isBleEnabled()
}
