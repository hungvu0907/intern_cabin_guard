package com.example.cabinguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.domain.sensor.CabinSensorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorEngine: CabinSensorEngine,
    private val telemetryDao: CabinTelemetryDao
) : ViewModel() {

    private val isPaused = MutableStateFlow(false)
    private var collectJob: Job? = null

    val uiState: StateFlow<DashboardUiState> = combine(
        telemetryDao.observeLatest(),
        telemetryDao.observeAll(),
        isPaused
    ) { latest, history, paused ->
        DashboardUiState(
            latest = latest,
            history = history,
            isPaused = paused
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    init {
        startCollecting()
    }

    fun pauseMonitoring() {
        if (isPaused.value) return
        collectJob?.cancel()
        collectJob = null
        isPaused.value = true
    }

    fun resumeMonitoring() {
        if (!isPaused.value && collectJob?.isActive == true) return
        isPaused.value = false
        startCollecting()
    }

    private fun startCollecting() {
        if (collectJob?.isActive == true) return
        collectJob = viewModelScope.launch {
            sensorEngine.observeTelemetry().collect { telemetry ->
                telemetryDao.insert(telemetry)
            }
        }
    }
}
