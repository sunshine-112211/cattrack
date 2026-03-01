package com.cattrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cattrack.app.data.model.*
import com.cattrack.app.data.repository.CatRepository
import com.cattrack.app.data.repository.DeviceRepository
import com.cattrack.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val cats: List<Cat> = emptyList(),
    val selectedCat: Cat? = null,
    val currentStatus: CatStatus? = null,
    val todayActivities: List<ActivityData> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val batteryLevel: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catRepository: CatRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeCats()
        observeConnectionState()
        observeBattery()
    }

    private fun observeCats() {
        viewModelScope.launch {
            catRepository.getAllCats().collect { cats ->
                val selected = _uiState.value.selectedCat ?: cats.firstOrNull()
                _uiState.update { it.copy(cats = cats, selectedCat = selected, isLoading = false) }
                selected?.let { cat ->
                    loadCatData(cat.id)
                    observeLatestActivity(cat.id)
                }
            }
        }
    }

    private fun loadCatData(catId: Long) {
        viewModelScope.launch {
            try {
                val (steps, activeMin, sleepMin) = catRepository.getTodaySummary(catId)
                val healthData = catRepository.getTodayHealthData(catId)
                val cat = catRepository.getCatById(catId)

                _uiState.update { state ->
                    state.copy(
                        currentStatus = CatStatus(
                            cat = cat ?: state.selectedCat ?: return@update state,
                            todaySteps = steps,
                            todayActiveMinutes = activeMin,
                            todaySleepMinutes = sleepMin,
                            batteryLevel = state.batteryLevel,
                            isDeviceConnected = deviceRepository.isConnected(),
                            lastSyncTime = healthData?.timestamp ?: 0L
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun observeLatestActivity(catId: Long) {
        viewModelScope.launch {
            catRepository.getTodayActivityData(catId).collect { activities ->
                _uiState.update { it.copy(todayActivities = activities) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            deviceRepository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    private fun observeBattery() {
        viewModelScope.launch {
            deviceRepository.batteryLevel.collect { level ->
                _uiState.update { it.copy(batteryLevel = level) }
            }
        }
    }

    fun selectCat(cat: Cat) {
        _uiState.update { it.copy(selectedCat = cat) }
        loadCatData(cat.id)
        observeLatestActivity(cat.id)
    }

    fun refresh() {
        _uiState.value.selectedCat?.let { loadCatData(it.id) }
    }
}
