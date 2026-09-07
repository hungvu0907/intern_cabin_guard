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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class CabinSensorEngine @Inject constructor(){

    private val scanInterval = MutableStateFlow(1_000L)

    val scanIntervalMillis: Long
        get() = scanInterval.value

    fun setBatteryLow(batteryLow: Boolean) {
        scanInterval.value = if (batteryLow) 5_000L else 1_000L
    }

    fun observeTelemetry(): Flow<CabinTelemetry> = flow {
        while (currentCoroutineContext().isActive) {
            val timestamp = System.currentTimeMillis()
            val temperature = Random.nextDouble(25.0, 45.0)
            val pressure = Random.nextDouble(980.0, 1020.0)
            val co2Level = Random.nextInt(400, 1201)
            val isWarning = co2Level > 1000 || temperature > 38

            val telemetry = CabinTelemetry(
                timestamp = timestamp,
                temperature = temperature,
                pressure = pressure,
                co2Level = co2Level,
                isWarning = isWarning
            )

            emit(telemetry)

            delay(scanIntervalMillis)
        }
    }.flowOn(Dispatchers.IO)
}
