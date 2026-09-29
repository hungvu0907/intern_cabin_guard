package com.example.cabinguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    telemetryDao: CabinTelemetryDao,
    private val thresholdSettings: ThresholdSettingsRepository
) : ViewModel() {

    private val isPaused = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        telemetryDao.observeLatest(),
        telemetryDao.observeAll(),
        isPaused,
        thresholdSettings.thresholds
    ) { latest, history, paused, thresholds ->
        DashboardUiState(
            latest = latest,
            history = history,
            isPaused = paused,
            warningThresholds = thresholds
        )
    }.scan(DashboardUiState()) { previous, current ->
        // Đang pause thì giữ nguyên số liệu đang hiển thị, Service vẫn ghi Room.
        if (current.isPaused) {
            previous.copy(
                isPaused = true,
                warningThresholds = current.warningThresholds
            )
        } else {
            current
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    // Pause chỉ đổi UI; Service vẫn ghi Room (PR A không stopService).
    fun pauseMonitoring() {
        isPaused.value = true
    }

    fun resumeMonitoring() {
        isPaused.value = false
    }

    fun updateWarningThresholds(thresholds: CabinWarningThresholds) {
        viewModelScope.launch {
            thresholdSettings.update(thresholds)
        }
    }
}
