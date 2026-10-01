package com.example.cabinguard.domain.sensor

import com.example.cabinguard.data.model.CabinTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.random.Random
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** Sequence identifies a measurement across threshold reevaluations. */
data class EvaluatedSample(val sequence: Long, val telemetry: CabinTelemetry)

class CabinSensorEngine {

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

    fun sensorFlow(
        now: () -> Long = System::currentTimeMillis,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<CabinTelemetry> = flow {
        while (currentCoroutineContext().isActive) {
            val timestamp = now()
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
                isWarning = false
            )

            emit(telemetry)

            delay(scanIntervalMillis)
        }
    }.flowOn(dispatcher)

    fun observeSamples(
        thresholds: Flow<CabinWarningThresholds>,
        sensors: Flow<CabinTelemetry> = sensorFlow()
    ): Flow<EvaluatedSample> {
        val numbered = flow {
            var sequence = 0L
            sensors.collect { emit(EvaluatedSample(++sequence, it)) }
        }
        return combine(numbered, thresholds.distinctUntilChanged()) { sample, limits ->
            sample.copy(telemetry = sample.telemetry.copy(
                isWarning = limits.isWarning(sample.telemetry.temperature, sample.telemetry.co2Level)
            ))
        }
    }

    /** Android supplies the unseeded DataStore flow; CLI uses explicit defaults. */
    fun observeTelemetry(thresholds: Flow<CabinWarningThresholds> = flowOf(CabinWarningThresholds())):
        Flow<CabinTelemetry> = observeSamples(thresholds).map { it.telemetry }
}
