package com.cattrack.app.data.repository

import com.cattrack.app.data.local.CatDao
import com.cattrack.app.data.local.HealthDataDao
import com.cattrack.app.data.model.*
import com.cattrack.app.util.DateUtils
import com.cattrack.app.util.HealthAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatRepository @Inject constructor(
    private val catDao: CatDao,
    private val healthDataDao: HealthDataDao
) {

    fun getAllCats(): Flow<List<Cat>> = catDao.getAllActiveCats()

    suspend fun getCatById(catId: Long): Cat? = catDao.getCatById(catId)

    fun getCatByIdFlow(catId: Long): Flow<Cat?> = catDao.getCatByIdFlow(catId)

    suspend fun addCat(cat: Cat): Long = catDao.insertCat(cat)

    suspend fun updateCat(cat: Cat) = catDao.updateCat(cat)

    suspend fun deleteCat(catId: Long) = catDao.softDeleteCat(catId)

    suspend fun bindDevice(catId: Long, deviceId: String) = catDao.bindDevice(catId, deviceId)

    suspend fun unbindDevice(catId: Long) = catDao.unbindDevice(catId)

    suspend fun updateWeight(catId: Long, weight: Float) = catDao.updateWeight(catId, weight)

    // Health Data
    suspend fun getLatestHealthData(catId: Long): HealthData? =
        healthDataDao.getLatestHealthData(catId)

    suspend fun getTodayHealthData(catId: Long): HealthData? {
        val today = DateUtils.getTodayDateString()
        return healthDataDao.getHealthDataByDate(catId, today)
    }

    fun getHealthDataRange(catId: Long, startDate: String, endDate: String): Flow<List<HealthData>> =
        healthDataDao.getHealthDataRange(catId, startDate, endDate)

    suspend fun saveHealthData(healthData: HealthData): Long =
        healthDataDao.insertHealthData(healthData)

    // Activity Data
    fun getTodayActivityData(catId: Long): Flow<List<ActivityData>> {
        val today = DateUtils.getTodayDateString()
        return healthDataDao.getActivityDataByDate(catId, today)
    }

    fun getActivityDataRange(catId: Long, startDate: String, endDate: String): Flow<List<ActivityData>> =
        healthDataDao.getActivityDataRange(catId, startDate, endDate)

    fun getLatestActivityData(catId: Long): Flow<ActivityData?> =
        healthDataDao.getLatestActivityData(catId)

    suspend fun saveActivityData(activityData: ActivityData): Long =
        healthDataDao.insertActivityData(activityData)

    suspend fun saveActivityDataBatch(dataList: List<ActivityData>) =
        healthDataDao.insertActivityDataList(dataList)

    // Reports
    suspend fun generateWeeklyReport(catId: Long): HealthReport {
        val (startDate, endDate) = DateUtils.getThisWeekRange()
        val (prevStart, prevEnd) = DateUtils.getLastWeekRange()

        val healthDataList = healthDataDao.getHealthDataRange(catId, startDate, endDate).first()
        val prevHealthDataList = healthDataDao.getHealthDataRange(catId, prevStart, prevEnd).first()

        return HealthAnalyzer.generateReport(catId, ReportPeriod.WEEKLY, startDate, endDate, healthDataList, prevHealthDataList)
    }

    suspend fun generateMonthlyReport(catId: Long): HealthReport {
        val (startDate, endDate) = DateUtils.getThisMonthRange()
        val (prevStart, prevEnd) = DateUtils.getLastMonthRange()

        val healthDataList = healthDataDao.getHealthDataRange(catId, startDate, endDate).first()
        val prevHealthDataList = healthDataDao.getHealthDataRange(catId, prevStart, prevEnd).first()

        return HealthAnalyzer.generateReport(catId, ReportPeriod.MONTHLY, startDate, endDate, healthDataList, prevHealthDataList)
    }

    suspend fun getTodaySummary(catId: Long): Triple<Int, Int, Int> {
        val today = DateUtils.getTodayDateString()
        val steps = healthDataDao.getTotalStepsByDate(catId, today) ?: 0
        val activeMinutes = healthDataDao.getTotalActiveMinutesByDate(catId, today) ?: 0
        val sleepMinutes = healthDataDao.getTotalSleepMinutesByDate(catId, today) ?: 0
        return Triple(steps, activeMinutes, sleepMinutes)
    }
}
