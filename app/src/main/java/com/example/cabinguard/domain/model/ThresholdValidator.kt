package com.example.cabinguard.domain.model

/** Khoảng hợp lệ trước khi lưu Settings: nhiệt độ 30–60°C, CO2 500–3000 ppm. */
object ThresholdValidator {
    const val TEMP_MIN = 30f
    const val TEMP_MAX = 60f
    const val CO2_MIN = 500f
    const val CO2_MAX = 3000f

    fun isTempValid(value: Float): Boolean = value in TEMP_MIN..TEMP_MAX

    fun isCo2Valid(value: Float): Boolean = value in CO2_MIN..CO2_MAX

    fun validate(tempThreshold: Float, co2Threshold: Float): String? {
        if (!isTempValid(tempThreshold)) {
            return "Nhiệt độ phải từ ${TEMP_MIN.toInt()}–${TEMP_MAX.toInt()}°C"
        }
        if (!isCo2Valid(co2Threshold)) {
            return "CO2 phải từ ${CO2_MIN.toInt()}–${CO2_MAX.toInt()} ppm"
        }
        return null
    }
}
