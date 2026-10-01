package com.example.cabinguard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.sensor.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorFlowTest {
    @Test fun waitsForSettingsAndReevaluatesWithoutAnotherSample() = runTest {
        val settings = MutableSharedFlow<CabinWarningThresholds>()
        val samples = MutableSharedFlow<CabinTelemetry>()
        val engine = CabinSensorEngine()
        val emissions = mutableListOf<EvaluatedSample>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            engine.observeSamples(settings, samples).toList(emissions)
        }
        samples.emit(CabinTelemetry(timestamp = 123, temperature = 40.0, pressure = 1000.0, co2Level = 1100, isWarning = false))
        runCurrent()
        assertTrue(emissions.isEmpty())
        settings.emit(CabinWarningThresholds(50.0, 1500))
        runCurrent()
        assertFalse(emissions.single().telemetry.isWarning)
        settings.emit(CabinWarningThresholds())
        runCurrent()
        assertTrue(emissions.last().telemetry.isWarning)
        assertEquals(emissions.first().sequence, emissions.last().sequence)
        samples.emit(emissions.first().telemetry)
        runCurrent()
        assertNotEquals(emissions.first().sequence, emissions.last().sequence)
    }

    @Test fun batteryChangesSamplingDelayWithVirtualTime() = runTest {
        val engine = CabinSensorEngine()
        val times = mutableListOf<Long>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            engine.sensorFlow(now = { testScheduler.currentTime }, dispatcher = UnconfinedTestDispatcher(testScheduler))
                .collect { times += it.timestamp }
        }
        assertEquals(listOf(0L), times)
        advanceTimeBy(1000); runCurrent()
        engine.setBatteryLow(true)
        advanceTimeBy(1000); runCurrent()
        advanceTimeBy(4999); runCurrent()
        assertEquals(listOf(0L, 1000L, 2000L), times)
        advanceTimeBy(1); runCurrent()
        assertEquals(7000L, times.last())
        engine.setBatteryLow(false)
        advanceTimeBy(5000); runCurrent()
        advanceTimeBy(1000); runCurrent()
        assertEquals(13000L, times.last())
        job.cancel()
    }
}
