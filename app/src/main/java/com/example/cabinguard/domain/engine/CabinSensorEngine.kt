package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CabinSensorEngine(
    private val settingsRepository: SettingsRepository,
    dispatcher: CoroutineDispatcher
) {

    // Hilt inject Settings; unit test truyền TestDispatcher vào constructor kia.
    @Inject
    constructor(settingsRepository: SettingsRepository) : this(settingsRepository, Dispatchers.IO)

    // @Volatile: thread đọc flow (IO) thấy ngay giá trị mới do receiver (main) đổi.
    @Volatile
    private var intervalMs: Long = 1_000L

    private val sharingScope = CoroutineScope(SupervisorJob() + dispatcher)

    // Cảm biến thô — isWarning tính sau khi combine với ngưỡng DataStore.
    private val rawReadings = flow {
        while (true) {
            emit(
                CabinTelemetry(
                    timestamp = System.currentTimeMillis(),
                    temperature = randomIn(25f, 45f),
                    pressure = randomIn(980f, 1020f),
                    co2Level = randomIn(400f, 1200f)
                )
            )
            delay(intervalMs)
        }
    }.flowOn(dispatcher)

    /**
     * Hot SharedFlow: ViewModel + Service collect chung một nguồn.
     * combine ngưỡng Settings — đổi ngưỡng áp dụng ngay, không cần restart app/Service.
     * WhileSubscribed(0): dừng emit khi không còn collector.
     * replay = 1: collector vào sau (bật Service) nhận ngay bản mới nhất.
     */
    val sensorFlow: SharedFlow<CabinTelemetry> = combine(
        rawReadings,
        settingsRepository.thresholds
    ) { reading, thresholds ->
        reading.copy(isWarning = thresholds.isWarning(reading.temperature, reading.co2Level))
    }.shareIn(
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
