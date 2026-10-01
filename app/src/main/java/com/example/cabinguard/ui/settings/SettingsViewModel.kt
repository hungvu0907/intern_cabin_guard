package com.example.cabinguard.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import com.example.cabinguard.ui.dashboard.*
import com.example.cabinguard.widget.WidgetRefresh
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val temperature: String = "",
    val co2: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: ThresholdSettingsRepository,
    private val savedState: SavedStateHandle,
    private val widgetUpdater: WidgetRefresh
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())
    val uiState = state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val limits = settings.thresholds.first()
                state.value = SettingsUiState(
                    temperature = savedState["temperature"] ?: limits.temperatureCelsius.toString(),
                    co2 = savedState["co2"] ?: limits.co2Ppm.toString(),
                    loading = false
                )
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                state.update { it.copy(loading = false, error = "Không đọc được cài đặt. Bạn có thể nhập và lưu lại ngưỡng.") }
            }
        }
    }

    fun temperatureChanged(value: String) {
        savedState["temperature"] = value
        state.update { it.copy(temperature = value, saved = false, error = null) }
    }
    fun co2Changed(value: String) {
        savedState["co2"] = value
        state.update { it.copy(co2 = value, saved = false, error = null) }
    }

    fun save() {
        if (state.value.loading || state.value.saving) return
        when (val parsed = parseWarningThresholdInput(state.value.temperature, state.value.co2)) {
            ThresholdInputResult.InvalidNumber -> state.update { it.copy(error = "Vui lòng nhập số hợp lệ") }
            ThresholdInputResult.OutOfRange -> state.update { it.copy(error = "Nhiệt độ 30–60 °C; CO₂ 500–3000 ppm") }
            is ThresholdInputResult.Valid -> {
                state.update { it.copy(saving = true, error = null) }
                viewModelScope.launch {
                    try {
                        settings.update(parsed.thresholds)
                        widgetUpdater.requestUpdate(force = true)
                        state.update { it.copy(saving = false, saved = true) }
                    } catch (error: CancellationException) { throw error }
                    catch (error: Exception) { state.update { it.copy(saving = false, error = "Lưu thất bại. Vui lòng thử lại.") } }
                }
            }
        }
    }
    fun consumeSaved() { state.update { it.copy(saved = false) } }
}
