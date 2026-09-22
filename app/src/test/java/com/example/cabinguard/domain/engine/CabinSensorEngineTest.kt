package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.domain.model.AlertThresholds
import com.example.cabinguard.testutil.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinSensorEngineTest {

    @Test
    fun `sensorFlow emits data every interval`() = runTest {
        val engine = CabinSensorEngine(FakeSettingsRepository())
        val results = engine.sensorFlow.take(3).toList()

        assertEquals(3, results.size)
        results.forEach { telemetry ->
            assertTrue(telemetry.temperature in 25f..45f)
            assertTrue(telemetry.pressure in 980f..1020f)
            assertTrue(telemetry.co2Level in 400f..1200f)
        }
    }

    @Test
    fun `isWarning uses default companion thresholds`() = runTest {
        val defaults = AlertThresholds()
        val engine = CabinSensorEngine(FakeSettingsRepository(defaults))
        val results = engine.sensorFlow.take(20).toList()

        results.forEach { telemetry ->
            val expected = defaults.isWarning(telemetry.temperature, telemetry.co2Level)
            assertEquals(expected, telemetry.isWarning)
            val hardcoded = telemetry.temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD ||
                telemetry.co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD
            assertEquals(hardcoded, telemetry.isWarning)
        }
    }

    @Test
    fun `low custom threshold marks typical reading as warning`() = runTest {
        val fake = FakeSettingsRepository(AlertThresholds(tempThreshold = 20f, co2Threshold = 100f))
        val engine = CabinSensorEngine(fake)
        assertTrue(engine.sensorFlow.first().isWarning)
    }

    @Test
    fun `high custom threshold keeps typical reading normal`() = runTest {
        val fake = FakeSettingsRepository(AlertThresholds(tempThreshold = 50f, co2Threshold = 2000f))
        val engine = CabinSensorEngine(fake)
        assertFalse(engine.sensorFlow.first().isWarning)
    }

    @Test
    fun `changing threshold updates isWarning on next emit`() = runTest {
        val fake = FakeSettingsRepository(AlertThresholds(tempThreshold = 50f, co2Threshold = 2000f))
        val engine = CabinSensorEngine(fake)
        assertFalse(engine.sensorFlow.first().isWarning)

        fake.emit(AlertThresholds(tempThreshold = 20f, co2Threshold = 100f))
        assertTrue(engine.sensorFlow.first().isWarning)
    }

    @Test
    fun `setInterval changes the interval`() {
        val engine = CabinSensorEngine(FakeSettingsRepository())
        engine.setInterval(5_000L)
    }
}
