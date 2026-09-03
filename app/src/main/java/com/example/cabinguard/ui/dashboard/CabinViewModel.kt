package com.example.cabinguard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import com.example.cabinguard.domain.engine.CabinSensorEngine
import com.example.cabinguard.domain.model.CabinUiState
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CabinViewModel @Inject constructor(
    private val sensorEngine: CabinSensorEngine,
    private val repository: CabinTelemetryRepository
) : ViewModel() {

    /** UI State hiện tại — mặc định Loading khi chưa có data */
    private val _uiState = MutableStateFlow<CabinUiState>(CabinUiState.Loading)
    val uiState: StateFlow<CabinUiState> = _uiState.asStateFlow()

    /** Lịch sử log từ Room DB — tự cập nhật khi có bản ghi mới */
    val historyLogs: StateFlow<List<CabinTelemetry>> = repository.getAllLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    ) 

    init {
        startCollectingSensorData()
    }
    
    

    private fun startCollectingSensorData(){
        viewModelScope.launch {
            sensorEngine.sensorFlow.collect{telemetry ->
                // Debug: In dữ liệu ra Logcat để verify
                Log.d("CabinVM", "🌡 Temp=${telemetry.temperature}°C | 💨 CO2=${telemetry.co2Level}ppm | ⚠ Warning=${telemetry.isWarning}")

                //1. Calculate UI State (Normal/Warning)
                _uiState.value = if(telemetry.isWarning){
                    CabinUiState.Warning(telemetry)
                }else{
                    CabinUiState.Normal(telemetry)
                }
                //2. Save to Room DB
                repository.saveTelemetry(telemetry)
                
            }
        }
    }
}
