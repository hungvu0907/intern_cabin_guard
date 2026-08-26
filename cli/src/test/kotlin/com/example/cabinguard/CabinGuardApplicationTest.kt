package com.example.cabinguard

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CabinGuardApplicationTest {
    @Test
    fun `enables battery low mode from the terminal flag`() {
        assertTrue(isBatteryLowMode(arrayOf("--battery-low")))
    }

    @Test
    fun `keeps normal mode when battery low flag is absent`() {
        assertFalse(isBatteryLowMode(emptyArray()))
    }
}
