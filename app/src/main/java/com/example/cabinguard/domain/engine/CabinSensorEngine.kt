package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CabinSensorEngine(
    dispatcher: CoroutineDispatcher
) {

    // Hilt inject constructor không-tham-số; unit test truyền TestDispatcher vào constructor kia.
    @Inject
    constructor() : this(Dispatchers.IO)

    // @Volatile: thread đọc flow (IO) thấy ngay giá trị mới do receiver (main) đổi.
    @Volatile
    private var intervalMs: Long = 1_000L

    private val sharingScope = CoroutineScope(SupervisorJob() + dispatcher)

    // Hot SharedFlow: ViewModel + Service collect chung một nguồn — cùng timestamp / isWarning.
    // WhileSubscribed(0): dừng emit khi không còn collector (test không treo; không tốn pin khi idle).
    // replay = 1: collector vào sau (bật Service) nhận ngay bản mới nhất.
    val sensorFlow: SharedFlow<CabinTelemetry> = flow {
        while (true) {
            val temperature = randomIn(25f, 45f)
            val pressure = randomIn(980f, 1020f)
            val co2Level = randomIn(400f, 1200f)
            val isWarning = temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD
                || co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD
            emit(
                CabinTelemetry(
                    timestamp = System.currentTimeMillis(),
                    temperature = temperature,
                    pressure = pressure,
                    co2Level = co2Level,
                    isWarning = isWarning
                )
            )
            delay(intervalMs)
        }
    }.flowOn(dispatcher)
        .shareIn(
            sharingScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
            replay = 1
        )

    fun setInterval(ms: Long) {
        intervalMs = ms
    }

    // nextFloat() ∈ [0, 1) → kết quả ∈ [min, max), có phần thập phân (không chỉ 25f, 26f, …).
    private fun randomIn(min: Float, max: Float): Float =
        min + Random.nextFloat() * (max - min)
}
