package com.example.cabinguard.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelemetryCsvCacheTest {

    @Test
    fun `returns stable csv path under cache export`() {
        val cacheDir = File("build/tmp/csv-cache-test").apply {
            deleteRecursively()
            mkdirs()
        }
        val first = TelemetryCsvCache.targetFile(cacheDir)
        val second = TelemetryCsvCache.targetFile(cacheDir)

        assertEquals(first, second)
        assertTrue(first.path.replace('\\', '/').endsWith("export/cabinguard_history.csv"))
        assertTrue(first.parentFile?.isDirectory == true)
    }
}
