package com.example.cabinguard.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import com.example.cabinguard.ui.dashboard.*
import com.example.cabinguard.widget.WidgetRefresh
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
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
    val error: String? = null,
    val showValidation: Boolean = false,
    val dirty: Boolean = false
) {
    val temperatureError: String? get() {
        if (!showValidation) return null
        val number = temperature.trim().replace(',', '.').toDoubleOrNull()
        return when {
            number == null || !number.isFinite() -> "Nhập nhiệt độ hợp lệ"
            number !in CabinWarningThresholds.MIN_TEMPERATURE..CabinWarningThresholds.MAX_TEMPERATURE -> "Nhiệt độ phải từ 30 đến 60 °C"
            else -> null
        }
    }
    val co2Error: String? get() {
        if (!showValidation) return null
        val number = co2.trim().toIntOrNull()
        return when {
            number == null -> "Nhập CO₂ bằng số nguyên"
            number !in CabinWarningThresholds.MIN_CO2..CabinWarningThresholds.MAX_CO2 -> "CO₂ phải từ 500 đến 3000 ppm"
            else -> null
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: ThresholdSettingsRepository,
    private val savedState: SavedStateHandle,
    private val widgetUpdater: WidgetRefresh
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())
    private var storedLimits: CabinWarningThresholds? = null
    val uiState = state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val limits = settings.thresholds.first()
                storedLimits = limits
                state.value = SettingsUiState(
                    temperature = savedState["temperature"] ?: limits.temperatureCelsius.toString(),
                    co2 = savedState["co2"] ?: limits.co2Ppm.toString(),
                    loading = false,
                    dirty = savedState.contains("temperature") || savedState.contains("co2")
                )
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                state.update { it.copy(loading = false, error = "Không đọc được cài đặt. Bạn có thể nhập và lưu lại ngưỡng.") }
            }
        }
    }

    fun temperatureChanged(value: String) {
        if (state.value.loading || state.value.saving) return
        savedState["temperature"] = value
        state.update { it.copy(temperature = value, saved = false, error = null, dirty = isDirty(value, it.co2)) }
    }
    fun co2Changed(value: String) {
        if (state.value.loading || state.value.saving) return
        savedState["co2"] = value
        state.update { it.copy(co2 = value, saved = false, error = null, dirty = isDirty(it.temperature, value)) }
    }

    fun save() {
        if (state.value.loading || state.value.saving) return
        when (val parsed = parseWarningThresholdInput(state.value.temperature, state.value.co2)) {
            ThresholdInputResult.InvalidNumber -> state.update { it.copy(showValidation = true) }
            ThresholdInputResult.OutOfRange -> state.update { it.copy(showValidation = true) }
            is ThresholdInputResult.Valid -> {
                state.update { it.copy(saving = true, error = null) }
                viewModelScope.launch {
                    try {
                        settings.update(parsed.thresholds)
                        storedLimits = parsed.thresholds
                        state.update { it.copy(saving = false, saved = true, dirty = false) }
                        // Launcher failures do not turn a committed DataStore write into a failed save.
                        try { widgetUpdater.requestUpdate(force = true) }
                        catch (error: CancellationException) { throw error }
                        catch (_: Exception) { /* The next telemetry/widget update can retry. */ }
                    } catch (error: CancellationException) { throw error }
                    catch (error: Exception) { state.update { it.copy(saving = false, error = "Lưu thất bại. Vui lòng thử lại.") } }
                }
            }
        }
    }
    fun consumeSaved() { state.update { it.copy(saved = false) } }

    fun discardDraft() {
        if (state.value.saving) return
        savedState.remove<String>("temperature")
        savedState.remove<String>("co2")
        state.value = SettingsUiState()
        load()
    }

    private fun isDirty(temperature: String, co2: String): Boolean =
        (parseWarningThresholdInput(temperature, co2) as? ThresholdInputResult.Valid)?.thresholds != storedLimits
}
