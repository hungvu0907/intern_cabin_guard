package com.example.cabinguard.domain.export

import com.example.cabinguard.data.local.CabinTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryCsvFormatterTest {

    @Test
    fun `header matches telemetry columns`() {
        val csv = TelemetryCsvFormatter.format(emptyList())
        assertEquals(TelemetryCsvFormatter.HEADER, csv)
        assertEquals(
            "id,timestamp,temperature,pressure,co2_level,is_warning,is_synced",
            csv,
        )
    }

    @Test
    fun `line count is one header plus each record`() {
        val csv = TelemetryCsvFormatter.format(listOf(sample(id = 1), sample(id = 2), sample(id = 3)))
        assertEquals(4, csv.lines().size)
    }

    @Test
    fun `sample values appear in the row`() {
        val log = sample(
            id = 7,
            timestamp = 1_700_000_000_000L,
            temperature = 39.5f,
            pressure = 1013.25f,
            co2Level = 1100f,
            isWarning = true,
            isSynced = false,
        )

        val csv = TelemetryCsvFormatter.format(listOf(log))
        val row = csv.lines()[1]

        assertTrue(row.contains("7"))
        assertTrue(row.contains("1700000000000"))
        assertTrue(row.contains("39.5"))
        assertTrue(row.contains("1013.25"))
        assertTrue(row.contains("1100"))
        assertTrue(row.contains("true"))
        assertTrue(row.contains("false"))
    }

    @Test
    fun `keeps caller order`() {
        val csv = TelemetryCsvFormatter.format(
            listOf(sample(id = 2, timestamp = 20L), sample(id = 1, timestamp = 10L)),
        )
        val ids = csv.lines().drop(1).map { it.substringBefore(',') }
        assertEquals(listOf("2", "1"), ids)
    }

    private fun sample(
        id: Long = 1,
        timestamp: Long = 1_700_000_000_000L,
        temperature: Float = 30f,
        pressure: Float = 1000f,
        co2Level: Float = 800f,
        isWarning: Boolean = false,
        isSynced: Boolean = false,
    ) = CabinTelemetry(
        id = id,
        timestamp = timestamp,
        temperature = temperature,
        pressure = pressure,
        co2Level = co2Level,
        isWarning = isWarning,
        isSynced = isSynced,
    )
}
