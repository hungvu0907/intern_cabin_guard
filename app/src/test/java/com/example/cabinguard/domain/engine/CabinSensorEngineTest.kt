package com.example.cabinguard.domain.engine

import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CabinSensorEngineTest {

    @Test
    fun `sensorFlow emits values within spec ranges`() = runTest {
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
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
    }

    @Test
    fun `isWarning matches temperature or CO2 threshold`() = runTest {
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
        val results = mutableListOf<CabinTelemetry>()
        backgroundScope.launch {
            engine.sensorFlow.collect { results.add(it) }
        }
        advanceTimeBy(49_000)
        runCurrent()

        assertTrue(results.size >= 50)
        results.forEach { telemetry ->
            val expected = telemetry.temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD
                || telemetry.co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD
            assertEquals(expected, telemetry.isWarning)
        }
        assertTrue(results.any { it.isWarning })
        assertTrue(results.any { !it.isWarning })
    }

    @Test
    fun `setInterval 5s delays the next emission`() = runTest {
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
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
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
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
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
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
}
