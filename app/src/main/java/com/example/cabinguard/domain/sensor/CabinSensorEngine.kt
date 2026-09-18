package com.example.cabinguard.domain.sensor

import com.example.cabinguard.data.model.CabinTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

@Singleton
class CabinSensorEngine @Inject constructor() {

    private val scanInterval =
        MutableStateFlow(CabinThresholds.SCAN_INTERVAL_NORMAL_MS)

    val scanIntervalMillis: Long
        get() = scanInterval.value

    fun setBatteryLow(batteryLow: Boolean) {
        scanInterval.value = if (batteryLow) {
            CabinThresholds.SCAN_INTERVAL_BATTERY_LOW_MS
        } else {
            CabinThresholds.SCAN_INTERVAL_NORMAL_MS
        }
    }

    fun observeTelemetry(): Flow<CabinTelemetry> = flow {
        while (currentCoroutineContext().isActive) {
            val timestamp = System.currentTimeMillis()
            val temperature = Random.nextDouble(
                CabinThresholds.TEMPERATURE_MIN_CELSIUS,
                CabinThresholds.TEMPERATURE_MAX_CELSIUS
            )
            val pressure = Random.nextDouble(
                CabinThresholds.PRESSURE_MIN_HPA,
                CabinThresholds.PRESSURE_MAX_HPA
            )
            val co2Level = Random.nextInt(
                CabinThresholds.CO2_MIN_PPM,
                CabinThresholds.CO2_MAX_PPM + 1
            )

            val telemetry = CabinTelemetry(
                timestamp = timestamp,
                temperature = temperature,
                pressure = pressure,
                co2Level = co2Level,
                isWarning = CabinThresholds.isWarning(temperature, co2Level)
            )

            emit(telemetry)

            delay(scanIntervalMillis)
        }
    }.flowOn(Dispatchers.IO)
}
