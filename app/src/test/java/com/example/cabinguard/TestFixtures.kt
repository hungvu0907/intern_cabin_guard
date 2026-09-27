package com.example.cabinguard

import com.example.cabinguard.data.local.CabinTelemetry

fun telemetry(
    temperature: Float,
    co2Level: Float,
    pressure: Float = 1000f,
    isWarning: Boolean = temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD
        || co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD
): CabinTelemetry = CabinTelemetry(
    timestamp = 1_700_000_000_000L,
    temperature = temperature,
    pressure = pressure,
    co2Level = co2Level,
    isWarning = isWarning
)
