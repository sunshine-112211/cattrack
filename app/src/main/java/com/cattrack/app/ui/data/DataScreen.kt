package com.cattrack.app.ui.data

import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cattrack.app.data.model.ActivityState
import com.cattrack.app.ui.theme.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 把 DataPeriod.entries 转成 list，避免 values() 弃用问题
    val periods = remember { DataPeriod.entries.toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text("数据分析", fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Period Tabs
        TabRow(
            selectedTabIndex = uiState.selectedPeriod.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            periods.forEachIndexed { index, period ->
                Tab(
                    selected = uiState.selectedPeriod == period,
                    onClick = { viewModel.selectPeriod(period) },
                    text = { Text(period.label) }
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataSummaryCard("步数", "${uiState.totalSteps}", "步", CatOrange, Modifier.weight(1f))
                DataSummaryCard("活动", "${uiState.totalActiveMinutes / 60}h${uiState.totalActiveMinutes % 60}m", "", ActiveGreen, Modifier.weight(1f))
                DataSummaryCard("睡眠", "${uiState.totalSleepMinutes / 60}h", "", SleepBlue, Modifier.weight(1f))
            }

            // Line Chart - Activity
            ChartCard(title = "活动量趋势") {
                ActivityLineChart(
                    dataList = uiState.activityDataList.map { it.steps.toFloat() },
                    labels = uiState.activityDataList.map { "${it.hour}:00" }
                )
            }

            // Bar Chart - Sleep
            ChartCard(title = "睡眠时间分布") {
                SleepBarChart(
                    dataList = uiState.activityDataList.map { it.sleepMinutes.toFloat() },
                    labels = uiState.activityDataList.map { "${it.hour}:00" }
                )
            }

            // Pie Chart - Activity Distribution
            ChartCard(title = "行为分布") {
                val sleepMin = uiState.activityDataList.sumOf { it.sleepMinutes }
                val activeMin = uiState.activityDataList.sumOf { it.activeMinutes }
                val restMin = uiState.activityDataList.sumOf { it.restMinutes }

                ActivityPieChart(
                    sleepMinutes = sleepMin,
                    activeMinutes = activeMin,
                    restMinutes = restMin
                )
            }

            // Detail Data Cards
            uiState.activityDataList.takeLast(6).reversed().forEach { data ->
                ActivityDetailCard(data = data)
            }
        }
    }
}

@Composable
private fun DataSummaryCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = color)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (unit.isNotEmpty()) {
                Text(text = unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun ActivityLineChart(
    dataList: List<Float>,
    labels: List<String>
) {
    if (dataList.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    600
                )
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setDrawGridBackground(false)
                axisRight.isEnabled = false
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
            }
        },
        update = { chart ->
            val entries = dataList.mapIndexed { index, value ->
                Entry(index.toFloat(), value)
            }
            val dataset = LineDataSet(entries, "步数").apply {
                color = CatOrange.toArgb()
                valueTextColor = android.graphics.Color.GRAY
                lineWidth = 2.5f
                circleRadius = 4f
                setCircleColor(CatOrange.toArgb())
                setDrawFilled(true)
                fillColor = CatOrange.copy(alpha = 0.2f).toArgb()
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
            }
            chart.data = LineData(dataset)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun SleepBarChart(
    dataList: List<Float>,
    labels: List<String>
) {
    if (dataList.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    500
                )
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                axisRight.isEnabled = false
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
            }
        },
        update = { chart ->
            val entries = dataList.mapIndexed { index, value ->
                BarEntry(index.toFloat(), value)
            }
            val dataset = BarDataSet(entries, "睡眠(min)").apply {
                color = SleepBlue.toArgb()
                valueTextColor = android.graphics.Color.GRAY
                setDrawValues(false)
            }
            chart.data = BarData(dataset)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    )
}

@Composable
private fun ActivityPieChart(
    sleepMinutes: Int,
    activeMinutes: Int,
    restMinutes: Int
) {
    val total = sleepMinutes + activeMinutes + restMinutes
    if (total == 0) {
        EmptyChartPlaceholder()
        return
    }
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    600
                )
                description.isEnabled = false
                isDrawHoleEnabled = true
                holeRadius = 50f
                setEntryLabelColor(android.graphics.Color.WHITE)
                setEntryLabelTextSize(12f)
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val entries = listOf(
                PieEntry(sleepMinutes.toFloat(), "睡眠"),
                PieEntry(activeMinutes.toFloat(), "活动"),
                PieEntry(restMinutes.toFloat(), "休息")
            )
            val dataset = PieDataSet(entries, "").apply {
                colors = listOf(
                    SleepBlue.toArgb(),
                    ActiveGreen.toArgb(),
                    RestYellow.toArgb()
                )
                valueTextColor = android.graphics.Color.WHITE
                valueTextSize = 12f
                sliceSpace = 2f
            }
            chart.data = PieData(dataset)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

@Composable
private fun ActivityDetailCard(data: com.cattrack.app.data.model.ActivityData) {
    // 用 entries 替代 values()
    val state = ActivityState.entries.find { it.name == data.activityState }
        ?: ActivityState.UNKNOWN

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = state.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${data.hour}:00 ${state.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "步数: ${data.steps} · 活动: ${data.activeMinutes}min · 睡眠: ${data.sleepMinutes}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
