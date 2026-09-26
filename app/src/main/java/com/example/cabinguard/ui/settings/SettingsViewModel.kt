package com.example.cabinguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.SettingsRepository
import com.example.cabinguard.domain.model.ThresholdValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val tempThreshold: Float = CabinTelemetry.TEMP_WARNING_THRESHOLD,
    val co2Threshold: Float = CabinTelemetry.CO2_WARNING_THRESHOLD,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.thresholds.collect { thresholds ->
                _uiState.update {
                    it.copy(
                        tempThreshold = thresholds.tempThreshold,
                        co2Threshold = thresholds.co2Threshold,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onTempChanged(value: Float) {
        _uiState.update {
            it.copy(tempThreshold = value, errorMessage = null, isSaved = false)
        }
    }

    fun onCo2Changed(value: Float) {
        _uiState.update {
            it.copy(co2Threshold = value, errorMessage = null, isSaved = false)
        }
    }

    /** Validate khoảng hợp lệ rồi mới ghi repository. */
    fun save() {
        viewModelScope.launch {
            val current = _uiState.value
            val error = ThresholdValidator.validate(current.tempThreshold, current.co2Threshold)
            if (error != null) {
                _uiState.update { it.copy(errorMessage = error, isSaved = false) }
                return@launch
            }
            settingsRepository.saveThresholds(current.tempThreshold, current.co2Threshold)
                .onSuccess {
                    _uiState.update { it.copy(isSaved = true, errorMessage = null) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.message, isSaved = false)
                    }
                }
        }
    }
}
