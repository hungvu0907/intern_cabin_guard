package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.domain.sensor.CabinWarningThresholds

sealed interface ThresholdInputResult {
    data class Valid(val thresholds: CabinWarningThresholds) : ThresholdInputResult
    data object InvalidNumber : ThresholdInputResult
    data object OutOfRange : ThresholdInputResult
}

fun parseWarningThresholdInput(
    temperatureText: String,
    co2Text: String
): ThresholdInputResult {
    val temperature = temperatureText.trim().replace(',', '.').toDoubleOrNull()
        ?: return ThresholdInputResult.InvalidNumber
    val co2 = co2Text.trim().toIntOrNull()
        ?: return ThresholdInputResult.InvalidNumber

    return runCatching {
        CabinWarningThresholds(
            temperatureCelsius = temperature,
            co2Ppm = co2
        )
    }.fold(
        onSuccess = ThresholdInputResult::Valid,
        onFailure = { ThresholdInputResult.OutOfRange }
    )
}
