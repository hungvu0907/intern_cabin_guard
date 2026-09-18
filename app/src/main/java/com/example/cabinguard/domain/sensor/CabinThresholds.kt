package com.example.cabinguard.domain.sensor

/**
 * Ngưỡng cảnh báo và dải đo của cảm biến cabin.
 * Engine, UI và test đều đọc từ đây để không hard-code lặp lại mỗi nơi một số.
 */
object CabinThresholds {

    /** Trên ngưỡng này coi là quá nhiệt. */
    const val TEMPERATURE_WARNING_CELSIUS = 38.0

    /** Trên ngưỡng này coi là khí CO2 vượt mức an toàn. */
    const val CO2_WARNING_PPM = 1000

    const val TEMPERATURE_MIN_CELSIUS = 25.0
    const val TEMPERATURE_MAX_CELSIUS = 45.0

    const val PRESSURE_MIN_HPA = 980.0
    const val PRESSURE_MAX_HPA = 1020.0

    const val CO2_MIN_PPM = 400
    const val CO2_MAX_PPM = 1200

    /** Chu kỳ quét bình thường. */
    const val SCAN_INTERVAL_NORMAL_MS = 1_000L

    /** Chu kỳ quét khi pin yếu. */
    const val SCAN_INTERVAL_BATTERY_LOW_MS = 5_000L

    fun isWarning(temperature: Double, co2Level: Int): Boolean =
        temperature > TEMPERATURE_WARNING_CELSIUS || co2Level > CO2_WARNING_PPM
}
