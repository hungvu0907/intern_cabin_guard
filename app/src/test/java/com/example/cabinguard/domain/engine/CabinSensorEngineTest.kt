package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.domain.model.AlertThresholds
import com.example.cabinguard.testutil.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CabinSensorEngineTest {

    @Test
    fun `sensorFlow emits values within spec ranges`() = runTest {
        val engine = engine()
        val results = mutableListOf<CabinTelemetry>()
        backgroundScope.launch {
            engine.sensorFlow.collect { results.add(it) }
        }
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(results.size >= 3)
        results.forEach { telemetry ->
            assertTrue(telemetry.temperature in 25f..45f)
            assertTrue(telemetry.pressure in 980f..1020f)
            assertTrue(telemetry.co2Level in 400f..1200f)
        }
        assertTrue(
            "mô phỏng phải có phần thập phân, không chỉ số nguyên",
            results.any { it.temperature % 1f != 0f || it.pressure % 1f != 0f || it.co2Level % 1f != 0f }
        )
    }

    @Test
    fun `isWarning uses default companion thresholds`() = runTest {
        val defaults = AlertThresholds()
        val engine = engine(FakeSettingsRepository(defaults))
        val results = mutableListOf<CabinTelemetry>()
        backgroundScope.launch {
            engine.sensorFlow.collect { results.add(it) }
        }
        advanceTimeBy(49_000)
        runCurrent()

        assertTrue(results.size >= 50)
        results.forEach { telemetry ->
            val expected = defaults.isWarning(telemetry.temperature, telemetry.co2Level)
            assertEquals(expected, telemetry.isWarning)
        }
        assertTrue(results.any { it.isWarning })
        assertTrue(results.any { !it.isWarning })
    }

    @Test
    fun `low custom threshold marks typical reading as warning`() = runTest {
        val engine = engine(
            FakeSettingsRepository(AlertThresholds(tempThreshold = 20f, co2Threshold = 100f))
        )
        val collected = mutableListOf<CabinTelemetry>()
        backgroundScope.launch { engine.sensorFlow.collect { collected.add(it) } }
        runCurrent()

        assertTrue(collected.last().isWarning)
    }

    @Test
    fun `high custom threshold keeps typical reading normal`() = runTest {
        val engine = engine(
            FakeSettingsRepository(AlertThresholds(tempThreshold = 50f, co2Threshold = 2000f))
        )
        val collected = mutableListOf<CabinTelemetry>()
        backgroundScope.launch { engine.sensorFlow.collect { collected.add(it) } }
        runCurrent()

        assertFalse(collected.last().isWarning)
    }

    @Test
    fun `changing threshold updates isWarning on next emit`() = runTest {
        val fake = FakeSettingsRepository(AlertThresholds(tempThreshold = 50f, co2Threshold = 2000f))
        val engine = engine(fake)
        val collected = mutableListOf<CabinTelemetry>()
        backgroundScope.launch { engine.sensorFlow.collect { collected.add(it) } }
        runCurrent()
        assertFalse(collected.last().isWarning)

        fake.emit(AlertThresholds(tempThreshold = 20f, co2Threshold = 100f))
        runCurrent()
        assertTrue(collected.last().isWarning)
    }

    @Test
    fun `setInterval 5s delays the next emission`() = runTest {
        val engine = engine()
        engine.setInterval(5_000L)
        val collected = mutableListOf<CabinTelemetry>()
        backgroundScope.launch {
            engine.sensorFlow.collect { collected.add(it) }
        }

        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, collected.size)
    }

    @Test
    fun `default interval emits about every 1 second`() = runTest {
        val engine = engine()
        val collected = mutableListOf<CabinTelemetry>()
        backgroundScope.launch {
            engine.sensorFlow.collect { collected.add(it) }
        }

        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, collected.size)
    }

    @Test
    fun `two collectors receive the same shared emissions`() = runTest {
        val engine = engine()
        val first = mutableListOf<CabinTelemetry>()
        val second = mutableListOf<CabinTelemetry>()

        backgroundScope.launch { engine.sensorFlow.collect { first.add(it) } }
        backgroundScope.launch { engine.sensorFlow.collect { second.add(it) } }

        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(first.size >= 3)
        assertEquals(first.map { it.timestamp }, second.map { it.timestamp })
        assertEquals(first.map { it.temperature }, second.map { it.temperature })
        assertEquals(first.map { it.isWarning }, second.map { it.isWarning })
    }

    private fun TestScope.engine(
        settings: FakeSettingsRepository = FakeSettingsRepository()
    ): CabinSensorEngine = CabinSensorEngine(
        settings,
        UnconfinedTestDispatcher(testScheduler)
    )
}
