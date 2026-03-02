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
import com.cattrack.app.ui.theme.*

// Demo 数据：确保 UI 在没有真实数据时也能正常渲染
private val DEMO_STEPS = listOf(120f, 340f, 210f, 480f, 390f, 150f, 520f)
private val DEMO_SLEEP = listOf(60f, 0f, 120f, 90f, 30f, 150f, 80f)
private val DEMO_SLEEP_MIN = 420
private val DEMO_ACTIVE_MIN = 180
private val DEMO_REST_MIN = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    viewModel: DataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val periods = DataPeriod.values().toList()

    // ★ 所有 remember 必须在任何 return/条件分支之前定义
    val stepsList = remember(uiState.activityDataList) {
        if (uiState.activityDataList.isEmpty()) DEMO_STEPS
        else uiState.activityDataList.map { it.steps.toFloat() }
    }
    val sleepList = remember(uiState.activityDataList) {
        if (uiState.activityDataList.isEmpty()) DEMO_SLEEP
        else uiState.activityDataList.map { it.sleepMinutes.toFloat() }
    }
    val sleepMin = remember(uiState.activityDataList) {
        if (uiState.activityDataList.isEmpty()) DEMO_SLEEP_MIN
        else uiState.activityDataList.sumOf { it.sleepMinutes }
    }
    val activeMin = remember(uiState.activityDataList) {
        if (uiState.activityDataList.isEmpty()) DEMO_ACTIVE_MIN
        else uiState.activityDataList.sumOf { it.activeMinutes }
    }
    val restMin = remember(uiState.activityDataList) {
        if (uiState.activityDataList.isEmpty()) DEMO_REST_MIN
        else uiState.activityDataList.sumOf { it.restMinutes }
    }

    // 统计数据（demo 兜底）
    val totalSteps = if (uiState.totalSteps == 0 && uiState.activityDataList.isEmpty()) 1890 else uiState.totalSteps
    val totalActiveMin = if (uiState.totalActiveMinutes == 0 && uiState.activityDataList.isEmpty()) DEMO_ACTIVE_MIN else uiState.totalActiveMinutes
    val totalSleepMin = if (uiState.totalSleepMinutes == 0 && uiState.activityDataList.isEmpty()) DEMO_SLEEP_MIN else uiState.totalSleepMinutes

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
                    onClick = { try { viewModel.selectPeriod(period) } catch (e: Exception) { } },
                    text = { Text(period.label) }
                )
            }
        }

        // loading 时显示进度条，但不 return，继续渲染下面的内容
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                DataSummaryCard("步数", "$totalSteps", "步", CatOrange, Modifier.weight(1f))
                DataSummaryCard(
                    "活动",
                    "${totalActiveMin / 60}h${totalActiveMin % 60}m",
                    "", ActiveGreen, Modifier.weight(1f)
                )
                DataSummaryCard("睡眠", "${totalSleepMin / 60}h", "", SleepBlue, Modifier.weight(1f))
            }

            // 活动量趋势
            ChartCard(title = "活动量趋势（步数）") {
                SimpleLineChart(
                    data = stepsList,
                    lineColor = CatOrange,
                    fillColor = CatOrange.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }

            // 睡眠柱状图
            ChartCard(title = "睡眠时间分布（分钟）") {
                SimpleBarChart(
                    data = sleepList,
                    barColor = SleepBlue,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            }

            // 行为分布
            ChartCard(title = "行为分布") {
                SimplePieChart(
                    segments = listOf(
                        PieSegment("睡眠", sleepMin.toFloat(), SleepBlue),
                        PieSegment("活动", activeMin.toFloat(), ActiveGreen),
                        PieSegment("休息", restMin.toFloat(), RestYellow)
                    ),
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }

            // 详细数据
            if (uiState.activityDataList.isNotEmpty()) {
                uiState.activityDataList.takeLast(6).reversed().forEach { data ->
                    ActivityDetailCard(data = data)
                }
            } else {
                // Demo 提示卡
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CatOrange.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📡", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "连接设备后查看真实数据",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "当前显示演示数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------- 图表组件（纯 Compose Canvas，无第三方库） --------

data class PieSegment(val label: String, val value: Float, val color: Color)

@Composable
private fun SimpleLineChart(
    data: List<Float>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val safeData = if (data.size >= 2) data else listOf(0f, 0f)

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val padH = 12.dp.toPx()
        val padV = 12.dp.toPx()
        val chartW = (size.width - padH * 2).coerceAtLeast(1f)
        val chartH = (size.height - padV * 2).coerceAtLeast(1f)
        val maxVal = safeData.max().coerceAtLeast(1f)
        val minVal = safeData.min()
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val n = safeData.size
        val xStep = chartW / (n - 1).coerceAtLeast(1)

        fun xOf(i: Int) = padH + i * xStep
        fun yOf(v: Float) = padV + chartH * (1f - (v - minVal) / range)

        repeat(4) { i ->
            val y = padV + chartH * i / 3f
            drawLine(Color.LightGray.copy(alpha = 0.4f), Offset(padH, y), Offset(padH + chartW, y), 1f)
        }

        val fillPath = Path().apply {
            moveTo(xOf(0), padV + chartH)
            lineTo(xOf(0), yOf(safeData[0]))
            safeData.forEachIndexed { i, v -> if (i > 0) lineTo(xOf(i), yOf(v)) }
            lineTo(xOf(n - 1), padV + chartH)
            close()
        }
        drawPath(fillPath, fillColor)

        val linePath = Path().apply {
            moveTo(xOf(0), yOf(safeData[0]))
            safeData.forEachIndexed { i, v -> if (i > 0) lineTo(xOf(i), yOf(v)) }
        }
        drawPath(linePath, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        safeData.forEachIndexed { i, v ->
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
    val safeData = data.ifEmpty { listOf(0f) }
    val maxVal = safeData.max().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val padH = 8.dp.toPx()
        val padV = 8.dp.toPx()
        val chartW = (size.width - padH * 2).coerceAtLeast(1f)
        val chartH = (size.height - padV * 2).coerceAtLeast(1f)
        val slotW = chartW / safeData.size
        val barW = slotW * 0.6f
        val gapX = slotW * 0.2f

        safeData.forEachIndexed { i, v ->
            val x = padH + i * slotW + gapX
            val barH = (v / maxVal) * chartH
            val y = padV + chartH - barH
            if (barH > 0.5f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun SimplePieChart(
    segments: List<PieSegment>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val radius = minOf(size.width, size.height) / 2f * 0.65f
            val cx = size.width / 2f
            val cy = size.height / 2f * 0.82f
            var startAngle = -90f

            segments.forEach { seg ->
                if (seg.value > 0f) {
                    val sweep = (seg.value / total) * 360f
                    drawArc(
                        color = seg.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    startAngle += sweep
                }
            }
            drawCircle(Color.White, radius = radius * 0.5f, center = Offset(cx, cy))
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { seg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(10.dp)
                            .background(seg.color, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "${seg.label} ${seg.value.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DataSummaryCard(
    title: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier
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
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            if (unit.isNotEmpty()) Text(text = unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun ActivityDetailCard(data: com.cattrack.app.data.model.ActivityData) {
    val stateName = try { data.activityState } catch (e: Exception) { "UNKNOWN" }
    val stateEmoji = when (stateName) {
        "SLEEPING" -> "😴"; "WALKING" -> "🐾"; "RUNNING" -> "🏃"
        "PLAYING" -> "🎾"; "EATING" -> "🍽️"; "RESTING" -> "🐱"
        else -> "❓"
    }
    val stateDisplay = when (stateName) {
        "SLEEPING" -> "睡觉中"; "WALKING" -> "散步中"; "RUNNING" -> "奔跑中"
        "PLAYING" -> "玩耍中"; "EATING" -> "进食中"; "RESTING" -> "休息中"
        else -> "未知"
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stateEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${data.hour}:00 $stateDisplay",
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
