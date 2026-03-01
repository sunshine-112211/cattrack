@file:OptIn(ExperimentalMaterial3Api::class)
package com.cattrack.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cattrack.app.data.model.*
import com.cattrack.app.ui.components.CatAvatar
import com.cattrack.app.ui.components.StatusCard
import com.cattrack.app.ui.theme.*
import com.cattrack.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToData: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDevice: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Top Header
        item {
            HomeHeader(
                uiState = uiState,
                onDeviceClick = onNavigateToDevice
            )
        }

        // Cat Status Card
        item {
            uiState.currentStatus?.let { status ->
                CatStatusCard(
                    status = status,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } ?: EmptyCatCard(
                onAddCat = onNavigateToProfile,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Multi-Cat Selector
        if (uiState.cats.size > 1) {
            item {
                CatSelector(
                    cats = uiState.cats,
                    selectedCat = uiState.selectedCat,
                    onCatSelected = { viewModel.selectCat(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Today Summary
        item {
            TodaySummarySection(
                steps = uiState.currentStatus?.todaySteps ?: 0,
                activeMinutes = uiState.currentStatus?.todayActiveMinutes ?: 0,
                sleepMinutes = uiState.currentStatus?.todaySleepMinutes ?: 0,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Quick Actions
        item {
            QuickActions(
                onDataClick = onNavigateToData,
                onReportClick = onNavigateToReport,
                onProfileClick = onNavigateToProfile,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Recent Activity Timeline
        item {
            Text(
                text = "最近活动",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        if (uiState.todayActivities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无活动数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(uiState.todayActivities.takeLast(5).reversed()) { activity ->
                ActivityTimelineItem(
                    activity = activity,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    uiState: HomeUiState,
    onDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CatOrange, CatOrangeLight)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CatTrack",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "猫咪健康智能管家",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Battery
                    if (uiState.batteryLevel > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BatteryFull,
                                contentDescription = "电量",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${uiState.batteryLevel}%",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    // Connection Status
                    IconButton(onClick = onDeviceClick) {
                        Icon(
                            imageVector = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> Icons.Default.Bluetooth
                                ConnectionState.SCANNING -> Icons.Default.BluetoothSearching
                                else -> Icons.Default.BluetoothDisabled
                            },
                            contentDescription = "设备连接",
                            tint = when (uiState.connectionState) {
                                ConnectionState.CONNECTED -> Color.White
                                else -> Color.White.copy(alpha = 0.5f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatStatusCard(
    status: CatStatus,
    modifier: Modifier = Modifier
) {
    val activityState = status.currentActivity

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatAvatar(
                    avatarUri = status.cat.avatarUri,
                    size = 72.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.cat.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activityState.emoji,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activityState.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (status.lastSyncTime > 0) {
                        Text(
                            text = "上次同步：${DateUtils.getRelativeTime(status.lastSyncTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Connection indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (status.isDeviceConnected) ActiveGreen else Color.Gray)
                )
            }
        }
    }
}

@Composable
private fun EmptyCatCard(
    onAddCat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onAddCat
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🐱", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("还没有猫咪档案", style = MaterialTheme.typography.titleMedium)
            Text("点击添加你的猫咪", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CatSelector(
    cats: List<Cat>,
    selectedCat: Cat?,
    onCatSelected: (Cat) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(cats) { cat ->
            FilterChip(
                selected = cat.id == selectedCat?.id,
                onClick = { onCatSelected(cat) },
                label = { Text(cat.name) },
                leadingIcon = {
                    Text(text = "🐱", fontSize = 14.sp)
                }
            )
        }
    }
}

@Composable
private fun TodaySummarySection(
    steps: Int,
    activeMinutes: Int,
    sleepMinutes: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "今日概览",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusCard(
                title = "步数",
                value = steps.toString(),
                unit = "步",
                icon = "👟",
                color = CatOrange,
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                title = "活动",
                value = (activeMinutes / 60).toString() + "h${activeMinutes % 60}",
                unit = "min",
                icon = "🏃",
                color = ActiveGreen,
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                title = "睡眠",
                value = (sleepMinutes / 60).toString(),
                unit = "小时",
                icon = "😴",
                color = SleepBlue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActions(
    onDataClick: () -> Unit,
    onReportClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard(
                title = "数据分析",
                icon = "📊",
                color = CatPurple,
                onClick = onDataClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "健康报告",
                icon = "📋",
                color = CatMint,
                onClick = onReportClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "猫咪档案",
                icon = "🐾",
                color = RestYellow,
                onClick = onProfileClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActivityTimelineItem(
    activity: ActivityData,
    modifier: Modifier = Modifier
) {
    val state = ActivityState.values().find { it.name == activity.activityState } ?: ActivityState.UNKNOWN

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = state.emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${activity.hour}:00 · 步数 ${activity.steps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${activity.activeMinutes}min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Divider(modifier = Modifier.padding(start = 32.dp), thickness = 0.5.dp)
}
