package com.example.cabinguard

import com.example.cabinguard.domain.sensor.CabinSensorEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CabinSensorEngineTest {

    @Test
    fun testCabinSensorEngine() = runBlocking {
        println("=== BẮT ĐẦU TEST CABIN SENSOR ENGINE ===")
        val engine = CabinSensorEngine()

        // Thu thập 5 bản tin đầu tiên
        val job = launch {
            engine.observeTelemetry()
                .take(6)
                .collect { telemetry ->
                    println(
                        "-> [${telemetry.timestamp}] Temp: ${"%.1f".format(telemetry.temperature)}°C | " +
                                "CO2: ${telemetry.co2Level} ppm | " +
                                "Pressure: ${"%.1f".format(telemetry.pressure)} hPa | " +
                                "Warning: ${telemetry.isWarning}"
                    )
                }
        }

        // Sau 2.5s đổi sang chế độ pin yếu
        delay(2500L)
        println(">>> ĐỔI CHẾ ĐỘ: Battery LOW (chu kỳ quét chuyển thành 5000ms)...")
        engine.setBatteryLow(true)

        job.join()
        println("=== TEST HOÀN TẤT THÀNH CÔNG ===")
    }
}
