package com.example.cabinguard.domain.model

import com.example.cabinguard.data.local.CabinTelemetry

/**
 * Ngưỡng cảnh báo đang áp dụng.
 * Mặc định = companion Entity — chỉ dùng khi DataStore chưa có giá trị.
 */
data class AlertThresholds(
    val tempThreshold: Float = CabinTelemetry.TEMP_WARNING_THRESHOLD,
    val co2Threshold: Float = CabinTelemetry.CO2_WARNING_THRESHOLD
) {
    fun isWarning(temperature: Float, co2Level: Float): Boolean =
        temperature > tempThreshold || co2Level > co2Threshold
}
