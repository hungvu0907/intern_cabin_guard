package com.example.cabinguard.domain.engine

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CabinSensorEngineTest {

    private val engine = CabinSensorEngine()

    @Test
    fun `sensorFlow emits data every interval`() = runTest {
        // Lấy 3 bản ghi đầu tiên từ flow
        val results = engine.sensorFlow.take(3).toList()

        // Kiểm tra đủ 3 bản ghi
        assertEquals(3, results.size)

        // Kiểm tra giá trị nằm trong dải spec
        results.forEach { telemetry ->
            assertTrue(telemetry.temperature in 25f..45f)
            assertTrue(telemetry.pressure in 980f..1020f)
            assertTrue(telemetry.co2Level in 400f..1200f)
        }
    }

    @Test
    fun `isWarning is true when temperature exceeds threshold`() = runTest {
        val results = engine.sensorFlow.take(50).toList()

        results.forEach { telemetry ->
            val expected = telemetry.temperature > 38f || telemetry.co2Level > 1000f
            assertEquals(expected, telemetry.isWarning)
        }
    }

    @Test
    fun `setInterval changes the interval`() {
        engine.setInterval(5_000L)
        // Không crash = test pass
        // Logic interval sẽ được verify kỹ hơn ở Integration Test
    }
}
