package com.example.cabinguard

import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import com.example.cabinguard.ui.dashboard.*
import org.junit.Assert.*
import org.junit.Test

class WarningThresholdTest {
    @Test fun assignmentBoundaries() {
        for (temperature in listOf(30.0, 60.0)) CabinWarningThresholds(temperature, 1000)
        for (co2 in listOf(500, 3000)) CabinWarningThresholds(38.0, co2)
        for (temperature in listOf(29.0, 61.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { CabinWarningThresholds(temperature, 1000) }
        }
        for (co2 in listOf(499, 3001)) {
            assertThrows(IllegalArgumentException::class.java) { CabinWarningThresholds(38.0, co2) }
        }
    }
    @Test fun parsingAndStrictWarningComparison() {
        assertEquals(ThresholdInputResult.Valid(CabinWarningThresholds(38.5, 1500)), parseWarningThresholdInput(" 38,5 ", "1500"))
        for (input in listOf("", "abc", "NaN", "Infinity")) {
            assertFalse(parseWarningThresholdInput(input, "1000") is ThresholdInputResult.Valid)
        }
        for (input in listOf("", "abc", "NaN", "Infinity", "1000.5", "499", "3001")) {
            assertFalse(parseWarningThresholdInput("38", input) is ThresholdInputResult.Valid)
        }
        assertFalse(CabinWarningThresholds().isWarning(38.0, 1000))
        assertTrue(CabinWarningThresholds().isWarning(38.1, 1000))
    }
}
