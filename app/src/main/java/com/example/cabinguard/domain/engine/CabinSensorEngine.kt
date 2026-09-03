package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CabinSensorEngine @Inject constructor() {

    private var intervalMs: Long = 1_000L
    val sensorFlow: Flow<CabinTelemetry> = flow{
        while (true) {
            val temperature = (25..45).random().toFloat()
            val pressure = (980..1020).random().toFloat()
            val co2Level = (400..1200).random().toFloat()
            val isWarning = temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD
                         || co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD
            emit(
                CabinTelemetry(
                    timestamp   = System.currentTimeMillis(),
                    temperature = temperature,
                    pressure    = pressure,
                    co2Level    = co2Level,
                    isWarning   = isWarning
                )
            )
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO) 

    fun setInterval(ms: Long) {
        intervalMs = ms
    }
}
