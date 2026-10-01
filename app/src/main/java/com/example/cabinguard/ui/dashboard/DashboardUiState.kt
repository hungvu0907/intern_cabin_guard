package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.sensor.CabinWarningThresholds

data class DashboardUiState(
    val latest: CabinTelemetry? = null,
    val history: List<CabinTelemetry> = emptyList(),
    val isPaused: Boolean = false,
    val warningThresholds: CabinWarningThresholds = CabinWarningThresholds(),
    val unsyncedCount: Int = 0
) {
    val isWarning: Boolean
        get() = latest?.let { warningThresholds.isWarning(it.temperature, it.co2Level) } == true
}
