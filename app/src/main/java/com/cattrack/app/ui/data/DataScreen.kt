package com.cattrack.app.ui.data

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cattrack.app.data.model.ActivityState
import com.cattrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val periods = remember { DataPeriod.values().toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("数据分析", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        TabRow(
            selectedTabIndex = uiState.selectedPeriod.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            periods.forEachIndexed { _, period ->
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
            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataSummaryCard("步数", "${uiState.totalSteps}", "步", CatOrange, Modifier.weight(1f))
                DataSummaryCard(
                    "活动",
                    "${uiState.totalActiveMinutes / 60}h${uiState.totalActiveMinutes % 60}m",
                    "", ActiveGreen, Modifier.weight(1f)
                )
                DataSummaryCard("睡眠", "${uiState.totalSleepMinutes / 60}h", "", SleepBlue, Modifier.weight(1f))
            }

            // 活动量趋势折线图（纯 Compose Canvas）
            val stepsList = remember(uiState.activityDataList) {
                uiState.activityDataList.map { it.steps.toFloat() }
            }
            ChartCard(title = "活动量趋势（步数）") {
                if (stepsList.isEmpty()) {
                    EmptyChartPlaceholder()
                } else {
                    SimpleLineChart(
                        data = stepsList,
                        lineColor = CatOrange,
                        fillColor = CatOrange.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // 睡眠柱状图（纯 Compose Canvas）
            val sleepList = remember(uiState.activityDataList) {
                uiState.activityDataList.map { it.sleepMinutes.toFloat() }
            }
            ChartCard(title = "睡眠时间分布（分钟）") {
                if (sleepList.isEmpty()) {
                    EmptyChartPlaceholder()
                } else {
                    SimpleBarChart(
                        data = sleepList,
                        barColor = SleepBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }

            // 行为分布饼图（纯 Compose Canvas）
            val sleepMin = remember(uiState.activityDataList) { uiState.activityDataList.sumOf { it.sleepMinutes } }
            val activeMin = remember(uiState.activityDataList) { uiState.activityDataList.sumOf { it.activeMinutes } }
            val restMin = remember(uiState.activityDataList) { uiState.activityDataList.sumOf { it.restMinutes } }

            ChartCard(title = "行为分布") {
                if (sleepMin + activeMin + restMin == 0) {
                    EmptyChartPlaceholder()
                } else {
                    SimplePieChart(
                        segments = listOf(
                            PieSegment("睡眠", sleepMin.toFloat(), SleepBlue),
                            PieSegment("活动", activeMin.toFloat(), ActiveGreen),
                            PieSegment("休息", restMin.toFloat(), RestYellow)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // 详细数据列表
            uiState.activityDataList.takeLast(6).reversed().forEach { data ->
                ActivityDetailCard(data = data)
            }
        }
    }
}

// -------- 纯 Compose Canvas 图表组件 --------

data class PieSegment(val label: String, val value: Float, val color: Color)

@Composable
private fun SimpleLineChart(
    data: List<Float>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        EmptyChartPlaceholder()
        return
    }
    val maxVal = remember(data) { data.max().coerceAtLeast(1f) }
    val minVal = remember(data) { data.min() }

    Canvas(modifier = modifier) {
        val padH = 12.dp.toPx()
        val padV = 12.dp.toPx()
        val chartW = size.width - padH * 2
        val chartH = size.height - padV * 2
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val xStep = if (data.size > 1) chartW / (data.size - 1) else chartW

        fun xOf(i: Int) = padH + i * xStep
        fun yOf(v: Float) = padV + chartH * (1f - (v - minVal) / range)

        // 网格线
        repeat(4) { i ->
            val y = padV + chartH * i / 3f
            drawLine(Color.LightGray.copy(alpha = 0.4f), Offset(padH, y), Offset(padH + chartW, y), 1f)
        }

        // 填充区域
        val fillPath = Path().apply {
            moveTo(xOf(0), size.height - padV)
            lineTo(xOf(0), yOf(data[0]))
            data.forEachIndexed { i, v ->
                if (i > 0) lineTo(xOf(i), yOf(v))
            }
            lineTo(xOf(data.lastIndex), size.height - padV)
            close()
        }
        drawPath(fillPath, fillColor)

        // 折线
        val linePath = Path().apply {
            moveTo(xOf(0), yOf(data[0]))
            data.forEachIndexed { i, v ->
                if (i > 0) lineTo(xOf(i), yOf(v))
            }
        }
        drawPath(linePath, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // 数据点
        data.forEachIndexed { i, v ->
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(xOf(i), yOf(v)))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(xOf(i), yOf(v)))
        }
    }
}

@Composable
private fun SimpleBarChart(
    data: List<Float>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }
    val maxVal = remember(data) { data.max().coerceAtLeast(1f) }

    Canvas(modifier = modifier) {
        val padH = 8.dp.toPx()
        val padV = 8.dp.toPx()
        val chartW = size.width - padH * 2
        val chartH = size.height - padV * 2
        val totalBars = data.size
        val barW = (chartW / totalBars) * 0.6f
        val gap = (chartW / totalBars) * 0.4f / 2

        data.forEachIndexed { i, v ->
            val x = padH + i * (chartW / totalBars) + gap
            val barH = (v / maxVal) * chartH
            val y = padV + chartH - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

@Composable
private fun SimplePieChart(
    segments: List<PieSegment>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) {
        EmptyChartPlaceholder()
        return
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = minOf(size.width, size.height) / 2f * 0.75f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f

            segments.forEach { seg ->
                val sweep = (seg.value / total) * 360f
                drawArc(
                    color = seg.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweep
            }

            // 中心白圆
            drawCircle(Color.White, radius = radius * 0.5f, center = center)
        }

        // 图例
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                segments.forEach { seg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(seg.color, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${seg.label} ${seg.value.toInt()}min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// -------- 通用组件 --------

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
private fun ActivityDetailCard(data: com.cattrack.app.data.model.ActivityData) {
    val state = ActivityState.values().find { it.name == data.activityState } ?: ActivityState.UNKNOWN
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
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
