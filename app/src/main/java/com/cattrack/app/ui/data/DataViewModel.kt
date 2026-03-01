package com.cattrack.app.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cattrack.app.data.model.ActivityData
import com.cattrack.app.data.model.Cat
import com.cattrack.app.data.model.HealthData
import com.cattrack.app.data.repository.CatRepository
import com.cattrack.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DataPeriod(val label: String) {
    DAY("日"), WEEK("周"), MONTH("月")
}

data class DataUiState(
    val selectedPeriod: DataPeriod = DataPeriod.DAY,
    val selectedCat: Cat? = null,
    val cats: List<Cat> = emptyList(),
    val activityDataList: List<ActivityData> = emptyList(),
    val healthDataList: List<HealthData> = emptyList(),
    val totalSteps: Int = 0,
    val totalActiveMinutes: Int = 0,
    val totalSleepMinutes: Int = 0,
    val avgHealthScore: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DataViewModel @Inject constructor(
    private val catRepository: CatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                catRepository.getAllCats()
                    .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                    .collect { cats ->
                        val selected = _uiState.value.selectedCat ?: cats.firstOrNull()
                        _uiState.update { it.copy(cats = cats, selectedCat = selected) }
                        selected?.let { loadData(it.id, _uiState.value.selectedPeriod) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectPeriod(period: DataPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        _uiState.value.selectedCat?.let { loadData(it.id, period) }
    }

    fun selectCat(cat: Cat) {
        _uiState.update { it.copy(selectedCat = cat) }
        loadData(cat.id, _uiState.value.selectedPeriod)
    }

    private fun loadData(catId: Long, period: DataPeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val (startDate, endDate) = when (period) {
                    DataPeriod.DAY -> {
                        val today = DateUtils.getTodayDateString()
                        Pair(today, today)
                    }
                    DataPeriod.WEEK -> DateUtils.getThisWeekRange()
                    DataPeriod.MONTH -> DateUtils.getThisMonthRange()
                }

                combine(
                    catRepository.getActivityDataRange(catId, startDate, endDate),
                    catRepository.getHealthDataRange(catId, startDate, endDate)
                ) { activities, healthData ->
                    Pair(activities, healthData)
                }.catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }.collect { (activities, healthData) ->
                    val totalSteps = activities.sumOf { it.steps }
                    val totalActive = activities.sumOf { it.activeMinutes }
                    val totalSleep = activities.sumOf { it.sleepMinutes }
                    val avgScore = if (healthData.isEmpty()) 0f
                    else healthData.map { it.healthScore }.average().toFloat()

                    _uiState.update {
                        it.copy(
                            activityDataList = activities,
                            healthDataList = healthData,
                            totalSteps = totalSteps,
                            totalActiveMinutes = totalActive,
                            totalSleepMinutes = totalSleep,
                            avgHealthScore = avgScore,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
