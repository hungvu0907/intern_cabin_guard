package com.example.cabinguard.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelemetryCsvCacheTest {

    @Test
    fun `writes csv under cache export and overwrites the same file`() {
        val cacheDir = File("build/tmp/csv-cache-test").apply {
            deleteRecursively()
            mkdirs()
        }
        val csv = "id,timestamp\n1,10"

        val first = TelemetryCsvCache.write(cacheDir, csv)
        val second = TelemetryCsvCache.write(cacheDir, "id,timestamp\n2,20")

        assertEquals(first, second)
        assertTrue(first.path.replace('\\', '/').endsWith("export/cabinguard_history.csv"))
        assertEquals("id,timestamp\n2,20", first.readText())
        assertEquals(
            listOf(TelemetryCsvCache.FILE_NAME),
            File(cacheDir, TelemetryCsvCache.DIRECTORY_NAME).list()?.toList(),
        )
    }
}
