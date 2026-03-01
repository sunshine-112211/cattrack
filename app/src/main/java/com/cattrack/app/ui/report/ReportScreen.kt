package com.cattrack.app.ui.report

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cattrack.app.data.model.*
import com.cattrack.app.ui.theme.*
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("健康报告", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        )

        // Period Selector
        TabRow(
            selectedTabIndex = if (uiState.selectedPeriod == ReportPeriod.WEEKLY) 0 else 1
        ) {
            Tab(
                selected = uiState.selectedPeriod == ReportPeriod.WEEKLY,
                onClick = { viewModel.selectPeriod(ReportPeriod.WEEKLY) },
                text = { Text("本周报告") }
            )
            Tab(
                selected = uiState.selectedPeriod == ReportPeriod.MONTHLY,
                onClick = { viewModel.selectPeriod(ReportPeriod.MONTHLY) },
                text = { Text("本月报告") }
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val report = if (uiState.selectedPeriod == ReportPeriod.WEEKLY)
            uiState.weeklyReport else uiState.monthlyReport

        if (report == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Text("暂无报告数据", style = MaterialTheme.typography.bodyLarge)
                    Text("收集更多数据后生成报告", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            // Health Score Ring
            HealthScoreCard(score = report.healthScore, changePercent = report.comparedToPreviousPeriod)

            // Summary Stats
            ReportStatsRow(report = report)

            // Trend Analysis
            TrendCard(analysis = report.trendAnalysis)

            // Anomaly Alerts
            if (report.anomalies.isNotEmpty()) {
                AnomalySection(anomalies = report.anomalies)
            }

            // Suggestions
            SuggestionsCard(suggestions = report.suggestions)

            // Feeding Advice
            FeedingAdviceCard(advice = report.feedingAdvice)
        }
    }
}

@Composable
private fun HealthScoreCard(
    score: Int,
    changePercent: Float,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 90 -> ScoreExcellent
        score >= 75 -> ScoreGood
        score >= 60 -> ScoreMedium
        score >= 40 -> ScorePoor
        else -> ScoreBad
    }
    val scoreLabel = when {
        score >= 90 -> "非常健康"
        score >= 75 -> "状态良好"
        score >= 60 -> "一般"
        score >= 40 -> "需要关注"
        else -> "健康堪忧"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "综合健康评分",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Score Ring (Canvas)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                    // Background ring
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = -220f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )

                    // Score arc
                    val sweepAngle = (score / 100f) * 260f
                    drawArc(
                        color = scoreColor,
                        startAngle = -220f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Change Indicator
            if (changePercent != 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (changePercent > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (changePercent > 0) ActiveGreen else ScoreBad,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (changePercent > 0) "+" else ""}${"%.1f".format(changePercent)}% 较上期",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (changePercent > 0) ActiveGreen else ScoreBad
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportStatsRow(report: HealthReport, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportStatCard(
            label = "均日活动",
            value = "${report.avgActiveMinutes.toInt()}",
            unit = "min",
            color = CatOrange,
            modifier = Modifier.weight(1f)
        )
        ReportStatCard(
            label = "均日睡眠",
            value = "${report.avgSleepMinutes.toInt() / 60}",
            unit = "h",
            color = SleepBlue,
            modifier = Modifier.weight(1f)
        )
        ReportStatCard(
            label = "均日步数",
            value = "${report.avgSteps.toInt()}",
            unit = "步",
            color = ActiveGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReportStatCard(
    label: String,
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun TrendCard(analysis: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text("📈", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("趋势分析", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(analysis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AnomalySection(anomalies: List<AnomalyEvent>) {
    Column {
        Text(
            text = "⚠️ 异常提醒",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        anomalies.forEach { anomaly ->
            AnomalyCard(anomaly = anomaly)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AnomalyCard(anomaly: AnomalyEvent) {
    val severityColor = Color(anomaly.severity.color)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = anomaly.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = severityColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = anomaly.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = anomaly.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SuggestionsCard(suggestions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "健康建议",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            suggestions.filterNotNull().forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FeedingAdviceCard(advice: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CatOrange.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text("🍽️", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "喂养建议",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CatOrange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
