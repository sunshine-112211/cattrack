package com.cattrack.app.data.local

import androidx.room.*
import com.cattrack.app.data.model.ActivityData
import com.cattrack.app.data.model.HealthData
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDataDao {

    // ========== HealthData ==========
    @Query("SELECT * FROM health_data WHERE catId = :catId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHealthData(catId: Long): HealthData?

    @Query("SELECT * FROM health_data WHERE catId = :catId AND date = :date LIMIT 1")
    suspend fun getHealthDataByDate(catId: Long, date: String): HealthData?

    @Query("SELECT * FROM health_data WHERE catId = :catId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getHealthDataRange(catId: Long, startDate: String, endDate: String): Flow<List<HealthData>>

    @Query("SELECT * FROM health_data WHERE catId = :catId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHealthData(catId: Long, limit: Int): List<HealthData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthData(healthData: HealthData): Long

    @Update
    suspend fun updateHealthData(healthData: HealthData)

    @Query("SELECT AVG(healthScore) FROM health_data WHERE catId = :catId AND date BETWEEN :startDate AND :endDate")
    suspend fun getAvgHealthScore(catId: Long, startDate: String, endDate: String): Float?

    // ========== ActivityData ==========
    @Query("SELECT * FROM activity_data WHERE catId = :catId AND date = :date ORDER BY hour ASC")
    fun getActivityDataByDate(catId: Long, date: String): Flow<List<ActivityData>>

    @Query("SELECT * FROM activity_data WHERE catId = :catId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, hour ASC")
    fun getActivityDataRange(catId: Long, startDate: String, endDate: String): Flow<List<ActivityData>>

    @Query("SELECT * FROM activity_data WHERE catId = :catId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestActivityData(catId: Long): Flow<ActivityData?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityData(activityData: ActivityData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityDataList(dataList: List<ActivityData>)

    @Query("SELECT SUM(steps) FROM activity_data WHERE catId = :catId AND date = :date")
    suspend fun getTotalStepsByDate(catId: Long, date: String): Int?

    @Query("SELECT SUM(activeMinutes) FROM activity_data WHERE catId = :catId AND date = :date")
    suspend fun getTotalActiveMinutesByDate(catId: Long, date: String): Int?

    @Query("SELECT SUM(sleepMinutes) FROM activity_data WHERE catId = :catId AND date = :date")
    suspend fun getTotalSleepMinutesByDate(catId: Long, date: String): Int?

    @Query("DELETE FROM activity_data WHERE catId = :catId AND timestamp < :beforeTimestamp")
    suspend fun deleteOldActivityData(catId: Long, beforeTimestamp: Long)
}
