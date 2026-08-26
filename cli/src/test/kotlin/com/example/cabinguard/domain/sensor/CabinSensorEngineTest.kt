package com.example.cabinguard.domain.sensor

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CabinSensorEngineTest {
    private val engine = CabinSensorEngine()

    @Test
    fun `emits telemetry inside the configured sensor ranges`() = runBlocking {
        val telemetry = engine.observeTelemetry().first()

        assertTrue(telemetry.temperature in 18.0..<50.0)
        assertTrue(telemetry.pressure in 100.0..<2000.0)
        assertTrue(telemetry.co2Level in 400..<2000)
        assertTrue(telemetry.timestamp > 0)
    }

    @Test
    fun `warning flag matches the temperature and co2 thresholds`() = runBlocking {
        repeat(20) {
            val telemetry = engine.observeTelemetry().first()
            val expectedWarning = telemetry.co2Level > 1000 || telemetry.temperature > 38.0

            if (expectedWarning) {
                assertTrue(telemetry.isWarning)
            } else {
                assertFalse(telemetry.isWarning)
            }
        }
    }

    @Test
    fun `uses a five second scan interval while battery is low`() {
        assertEquals(1_000L, engine.scanIntervalMillis)

        engine.setBatteryLow(true)

        assertEquals(5_000L, engine.scanIntervalMillis)
    }

    @Test
    fun `restores the normal scan interval when battery recovers`() {
        engine.setBatteryLow(true)

        engine.setBatteryLow(false)

        assertEquals(1_000L, engine.scanIntervalMillis)
    }
}
