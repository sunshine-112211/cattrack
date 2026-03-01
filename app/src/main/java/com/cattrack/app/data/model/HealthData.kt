package com.cattrack.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_data")
data class HealthData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val catId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String, // yyyy-MM-dd
    val healthScore: Int = 0, // 0-100
    val activeLevel: Float = 0f, // 活跃度 0-100
    val sleepQuality: Float = 0f, // 睡眠质量 0-100
    val regularityScore: Float = 0f, // 作息规律度 0-100
    val totalActiveMinutes: Int = 0,
    val totalSleepMinutes: Int = 0,
    val totalRestMinutes: Int = 0,
    val totalSteps: Int = 0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val caloriesBurned: Float = 0f,
    val hasAnomaly: Boolean = false,
    val anomalyDescription: String = "",
    val suggestions: String = ""
)

data class HealthReport(
    val catId: Long,
    val period: ReportPeriod,
    val startDate: String,
    val endDate: String,
    val healthScore: Int,
    val avgActiveMinutes: Float,
    val avgSleepMinutes: Float,
    val avgSteps: Float,
    val trendAnalysis: String,
    val anomalies: List<AnomalyEvent>,
    val suggestions: List<String>,
    val feedingAdvice: String,
    val comparedToPreviousPeriod: Float // 环比变化百分比
)

data class AnomalyEvent(
    val date: String,
    val type: AnomalyType,
    val description: String,
    val severity: AnomalySeverity
)

enum class AnomalyType(val displayName: String) {
    ACTIVITY_DROP("活动量骤降"),
    ACTIVITY_SURGE("活动量异常增加"),
    SLEEP_ABNORMAL("睡眠异常"),
    IRREGULAR_SCHEDULE("作息不规律"),
    LONG_INACTIVITY("长时间不动"),
    RAPID_MOVEMENT("异常快速移动")
}

enum class AnomalySeverity(val displayName: String, val color: Long) {
    LOW("轻微", 0xFFFFC107),
    MEDIUM("中度", 0xFFFF9800),
    HIGH("严重", 0xFFF44336)
}

enum class ReportPeriod {
    DAILY, WEEKLY, MONTHLY
}
