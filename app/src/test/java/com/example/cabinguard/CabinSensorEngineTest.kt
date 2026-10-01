package com.example.cabinguard

import com.example.cabinguard.domain.sensor.CabinSensorEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CabinSensorEngineTest {
    @Test fun generatedSamplesStayInSensorRanges() = runBlocking {
        val sample = CabinSensorEngine().observeTelemetry().first()
        assertTrue(sample.temperature >= 25 && sample.temperature < 45)
        assertTrue(sample.pressure >= 980 && sample.pressure < 1020)
        assertTrue(sample.co2Level in 400..1200)
        assertEquals(sample.temperature > 38 || sample.co2Level > 1000, sample.isWarning)
    }
}