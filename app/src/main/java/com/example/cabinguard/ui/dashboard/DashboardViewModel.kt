package com.example.cabinguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.domain.sensor.CabinSensorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    sensorEngine: CabinSensorEngine,
    private val telemetryDao: CabinTelemetryDao
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        telemetryDao.observeLatest(),
        telemetryDao.observeAll()
    ) { latest, history ->
        DashboardUiState(
            latest = latest,
            history = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            sensorEngine.observeTelemetry().collect { telemetry ->
                telemetryDao.insert(telemetry)
            }
        }
    }
}
