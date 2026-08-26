package com.example.cabinguard.ui.dashboard

import androidx.lifecycle.ViewModel
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import com.example.cabinguard.domain.engine.CabinSensorEngine
import com.example.cabinguard.domain.model.CabinUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * [DOMAIN-04] ViewModel — Quản lý UI State và điều phối giữa Domain và UI Layer.
 *
 * TODO: Implement trong Task DOMAIN-04
 *   - Collect từ CabinSensorEngine.sensorFlow trong viewModelScope
 *   - Cập nhật _uiState dựa trên isWarning của telemetry
 *   - Lưu từng telemetry vào Room qua repository.saveTelemetry()
 *   - Expose historyLogs từ repository.getAllLogs()
 */
@HiltViewModel
class CabinViewModel @Inject constructor(
    private val sensorEngine: CabinSensorEngine,
    private val repository: CabinTelemetryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CabinUiState>(CabinUiState.Loading)
    val uiState: StateFlow<CabinUiState> = _uiState.asStateFlow()
}
