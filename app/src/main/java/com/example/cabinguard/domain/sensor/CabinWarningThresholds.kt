package com.example.cabinguard.domain.sensor

/**
 * User-configurable warning limits from the Sprint 2 assignment.
 */
data class CabinWarningThresholds(
    val temperatureCelsius: Double = CabinThresholds.TEMPERATURE_WARNING_CELSIUS,
    val co2Ppm: Int = CabinThresholds.CO2_WARNING_PPM
) {
    init {
        require(
            temperatureCelsius in
                MIN_TEMPERATURE..MAX_TEMPERATURE
        ) { "Temperature threshold must be 30–60 °C" }
        require(co2Ppm in MIN_CO2..MAX_CO2) {
            "CO2 threshold must be 500–3000 ppm"
        }
    }

    fun isWarning(temperature: Double, co2Level: Int): Boolean =
        temperature > temperatureCelsius || co2Level > co2Ppm

    companion object {
        const val MIN_TEMPERATURE = 30.0
        const val MAX_TEMPERATURE = 60.0
        const val MIN_CO2 = 500
        const val MAX_CO2 = 3000
    }
}
