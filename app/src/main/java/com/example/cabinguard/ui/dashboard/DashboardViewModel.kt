package com.example.cabinguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.local.CabinTelemetryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DashboardViewModel @Inject constructor(
    telemetryDao: CabinTelemetryDao
) : ViewModel() {

    private val isPaused = MutableStateFlow(false)

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

    // Pause chỉ đổi UI; Service vẫn ghi Room (PR A không stopService).
    fun pauseMonitoring() {
        isPaused.value = true
    }

    fun resumeMonitoring() {
        isPaused.value = false
    }
}
