package com.example.cabinguard.ui.widget

import com.example.cabinguard.telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinWidgetUiTest {

    @Test
    fun `empty history shows placeholder and is not a warning`() {
        val ui = null.toCabinWidgetUi()

        assertEquals("--", ui.temperatureText)
        assertEquals("Chưa có dữ liệu", ui.statusText)
        assertFalse(ui.isWarning)
    }

    @Test
    fun `safe row shows temperature and safe status`() {
        val ui = telemetry(temperature = 36.5f, co2Level = 800f, isWarning = false)
            .toCabinWidgetUi()

        assertEquals("${String.format("%.1f", 36.5f)}°C", ui.temperatureText)
        assertEquals("An toàn", ui.statusText)
        assertFalse(ui.isWarning)
    }

    @Test
    fun `warning row shows warning status`() {
        val ui = telemetry(temperature = 41.2f, co2Level = 1200f, isWarning = true)
            .toCabinWidgetUi()

        assertEquals("${String.format("%.1f", 41.2f)}°C", ui.temperatureText)
        assertEquals("Cảnh báo", ui.statusText)
        assertTrue(ui.isWarning)
    }

    @Test
    fun `widget trusts stored isWarning instead of the default 38C threshold`() {
        val ui = telemetry(temperature = 40f, co2Level = 900f, isWarning = false)
            .toCabinWidgetUi()

        assertFalse(ui.isWarning)
        assertEquals("An toàn", ui.statusText)
    }
}
