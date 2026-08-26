package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DOMAIN-01] Sensor Engine — Phát dữ liệu cảm biến giả lập liên tục qua Kotlin Flow.
 *
 * TODO: Implement trong Task DOMAIN-01
 *   - Phát dữ liệu giả lập mỗi 1 giây trên Dispatchers.IO:
 *       + Nhiệt độ: 25–45°C
 *       + Áp suất: 980–1020 hPa
 *       + CO2: 400–1200 ppm
 *   - [DOMAIN-02] Logic isWarning: temperature > 38f || co2Level > 1000f
 *   - [BR-02] setInterval(): điều chỉnh chu kỳ (1s ↔ 5s) khi pin yếu
 */
@Singleton
class CabinSensorEngine @Inject constructor() {

    val sensorFlow: Flow<CabinTelemetry> = emptyFlow()

    fun setInterval(ms: Long) {
        // TODO: Implement trong Task BR-02
    }
}
