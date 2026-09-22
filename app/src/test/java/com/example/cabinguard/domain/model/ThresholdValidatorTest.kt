package com.example.cabinguard.domain.model

import com.example.cabinguard.data.local.CabinTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThresholdValidatorTest {

    @Test
    fun `default thresholds match companion constants`() {
        val defaults = AlertThresholds()
        assertEquals(CabinTelemetry.TEMP_WARNING_THRESHOLD, defaults.tempThreshold)
        assertEquals(CabinTelemetry.CO2_WARNING_THRESHOLD, defaults.co2Threshold)
    }

    @Test
    fun `accepts values inside allowed range`() {
        assertTrue(ThresholdValidator.isTempValid(30f))
        assertTrue(ThresholdValidator.isTempValid(60f))
        assertTrue(ThresholdValidator.isCo2Valid(500f))
        assertTrue(ThresholdValidator.isCo2Valid(3000f))
        assertNull(ThresholdValidator.validate(38f, 1000f))
    }

    @Test
    fun `rejects temperature outside 30 to 60`() {
        assertFalse(ThresholdValidator.isTempValid(29.9f))
        assertFalse(ThresholdValidator.isTempValid(60.1f))
        assertNotNull(ThresholdValidator.validate(20f, 1000f))
    }

    @Test
    fun `rejects CO2 outside 500 to 3000`() {
        assertFalse(ThresholdValidator.isCo2Valid(499f))
        assertFalse(ThresholdValidator.isCo2Valid(3001f))
        assertNotNull(ThresholdValidator.validate(38f, 400f))
    }

    @Test
    fun `same reading can flip warning when threshold changes`() {
        val readingTemp = 36f
        val readingCo2 = 800f
        assertFalse(AlertThresholds(tempThreshold = 38f, co2Threshold = 1000f).isWarning(readingTemp, readingCo2))
        assertTrue(AlertThresholds(tempThreshold = 30f, co2Threshold = 700f).isWarning(readingTemp, readingCo2))
    }
}
