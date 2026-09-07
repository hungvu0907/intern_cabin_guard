package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.data.model.CabinTelemetry

data class DashboardUiState(
    val latest: CabinTelemetry? = null,
    val history: List<CabinTelemetry> = emptyList(),
    val isPaused: Boolean = false
) {
    val isWarning: Boolean
        get() = latest?.isWarning == true
}
