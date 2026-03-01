package com.cattrack.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cats")
data class Cat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val breed: String = "",
    val gender: String = "未知", // 公/母/未知
    val birthday: Long? = null, // timestamp
    val weight: Float = 0f, // kg
    val avatarUri: String = "",
    val deviceId: String = "", // 绑定设备ID
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class CatStatus(
    val cat: Cat,
    val currentActivity: ActivityState = ActivityState.REST,
    val todaySteps: Int = 0,
    val todayActiveMinutes: Int = 0,
    val todaySleepMinutes: Int = 0,
    val batteryLevel: Int = 0,
    val isDeviceConnected: Boolean = false,
    val lastSyncTime: Long = 0L
)

enum class ActivityState(val displayName: String, val emoji: String) {
    SLEEPING("睡觉中", "😴"),
    RESTING("休息中", "🐱"),
    WALKING("散步中", "🐾"),
    RUNNING("奔跑中", "🏃"),
    PLAYING("玩耍中", "🎾"),
    EATING("进食中", "🍽️"),
    UNKNOWN("未知", "❓")
}
