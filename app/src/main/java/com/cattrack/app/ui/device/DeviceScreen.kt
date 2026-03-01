package com.cattrack.app.ui.device

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cattrack.app.data.model.ConnectionState
import com.cattrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    onScanDevice: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("设备管理", fontWeight = FontWeight.Bold) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                connectionState = uiState.connectionState,
                device = uiState.currentDevice,
                batteryLevel = uiState.batteryLevel,
                onScanClick = onScanDevice,
                onDisconnectClick = { viewModel.disconnect() }
            )

            // Device Info Card
            uiState.currentDevice?.let { device ->
                DeviceInfoCard(device = device)

                // Sync Card
                ActionCard(
                    title = "数据同步",
                    description = "手动同步最新活动数据",
                    icon = "🔄",
                    buttonText = "立即同步",
                    onClick = { viewModel.syncData() }
                )

                // Firmware Update Card
                FirmwareCard(
                    firmwareVersion = device.firmwareVersion,
                    updateState = uiState.firmwareUpdateState,
                    onUpdateClick = { viewModel.startFirmwareUpdate() }
                )
            }

            // BLE not enabled warning
            if (!viewModel.isBleEnabled()) {
                BleDisabledWarning()
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    device: com.cattrack.app.data.model.DeviceInfo?,
    batteryLevel: Int,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val stateColor = when (connectionState) {
        ConnectionState.CONNECTED -> ActiveGreen
        ConnectionState.CONNECTING, ConnectionState.SCANNING -> CatOrange
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = stateColor.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, stateColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (connectionState) {
                        ConnectionState.CONNECTED -> Icons.Default.Bluetooth
                        ConnectionState.SCANNING -> Icons.Default.BluetoothSearching
                        else -> Icons.Default.BluetoothDisabled
                    },
                    contentDescription = "连接状态",
                    tint = stateColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connectionState.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = stateColor
                    )
                    device?.let {
                        Text(
                            text = it.deviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (connectionState == ConnectionState.CONNECTED && batteryLevel > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (batteryLevel > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                            contentDescription = "电量",
                            tint = if (batteryLevel > 20) ActiveGreen else ScoreBad
                        )
                        Text("$batteryLevel%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState != ConnectionState.CONNECTED
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (connectionState == ConnectionState.SCANNING) "扫描中..." else "扫描设备")
                }
                if (connectionState == ConnectionState.CONNECTED) {
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("断开连接")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(device: com.cattrack.app.data.model.DeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("设备信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            DeviceInfoRow("设备名称", device.deviceName)
            DeviceInfoRow("MAC地址", device.macAddress)
            DeviceInfoRow("固件版本", device.firmwareVersion)
            DeviceInfoRow("硬件版本", device.hardwareVersion)
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun FirmwareCard(
    firmwareVersion: String,
    updateState: com.cattrack.app.data.model.FirmwareUpdateState,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📡", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("固件升级", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("当前版本: $firmwareVersion", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!updateState.isUpdating) {
                    TextButton(onClick = onUpdateClick) {
                        Text("检查更新")
                    }
                }
            }
            if (updateState.isUpdating) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("升级中... ${updateState.progress}%", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { updateState.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BleDisabledWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScoreBad.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, ScoreBad.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = ScoreBad)
            Spacer(modifier = Modifier.width(12.dp))
            Text("蓝牙未开启，请在系统设置中开启蓝牙",
                style = MaterialTheme.typography.bodyMedium, color = ScoreBad)
        }
    }
}
