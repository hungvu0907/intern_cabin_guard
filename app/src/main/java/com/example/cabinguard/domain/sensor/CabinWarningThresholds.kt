package com.example.cabinguard.domain.sensor

/**
 * User-configurable warning limits. Values stay inside the ranges emitted by
 * [CabinSensorEngine] so a saved threshold can always be reached and tested.
 */
data class CabinWarningThresholds(
    val temperatureCelsius: Double = CabinThresholds.TEMPERATURE_WARNING_CELSIUS,
    val co2Ppm: Int = CabinThresholds.CO2_WARNING_PPM
) {
    init {
        require(
            temperatureCelsius in
                CabinThresholds.TEMPERATURE_MIN_CELSIUS..CabinThresholds.TEMPERATURE_MAX_CELSIUS
        ) { "Temperature threshold is outside the sensor range" }
        require(co2Ppm in CabinThresholds.CO2_MIN_PPM..CabinThresholds.CO2_MAX_PPM) {
            "CO2 threshold is outside the sensor range"
        }
    }

    fun isWarning(temperature: Double, co2Level: Int): Boolean =
        temperature > temperatureCelsius || co2Level > co2Ppm
}
