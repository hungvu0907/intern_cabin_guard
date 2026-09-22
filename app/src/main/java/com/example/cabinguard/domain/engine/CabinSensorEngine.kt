package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CabinSensorEngine @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    private var intervalMs: Long = 1_000L

    /** Cảm biến thô — isWarning tính sau khi combine với ngưỡng DataStore. */
    private val rawReadings: Flow<CabinTelemetry> = flow {
        while (true) {
            emit(
                CabinTelemetry(
                    timestamp = System.currentTimeMillis(),
                    temperature = (25..45).random().toFloat(),
                    pressure = (980..1020).random().toFloat(),
                    co2Level = (400..1200).random().toFloat()
                )
            )
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Kết hợp cảm biến với ngưỡng Settings — đổi ngưỡng áp dụng ở mẫu kế tiếp,
     * không cần restart app/Service.
     */
    val sensorFlow: Flow<CabinTelemetry> = combine(
        rawReadings,
        settingsRepository.thresholds
    ) { reading, thresholds ->
        reading.copy(isWarning = thresholds.isWarning(reading.temperature, reading.co2Level))
    }

    fun setInterval(ms: Long) {
        intervalMs = ms
    }
}
