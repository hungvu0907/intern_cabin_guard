package com.example.cabinguard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.export.TelemetryCsvWriter
import com.example.cabinguard.data.export.ExportFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.nio.file.Files

class PagedCsvTest {
    @Test fun manyBatchesHaveOneHeaderAndNoDuplicateRowsAtEqualTimestamps() = runTest {
        val rows = (1..503).map { CabinTelemetry(it.toLong(), (it / 3).toLong(), 38.5, 1000.0, 1000, false) }
        val out = StringBuilder()
        var largest = 0
        val count = TelemetryCsvWriter.writeBatches(out, ZoneId.of("UTC")) { last ->
            rows.filter { last == null || it.timestamp > last.timestamp || (it.timestamp == last.timestamp && it.id > last.id) }
                .take(50).also { largest = maxOf(largest, it.size) }
        }
        assertEquals(503, count)
        assertEquals(50, largest)
        assertEquals(1, out.lines().count { it == TelemetryCsvWriter.HEADER })
        assertEquals(rows.map { it.id.toString() }, out.lines().drop(1).filter { it.isNotEmpty() }.map { it.substringBefore(',') })
    }
    @Test fun cancellationRemovesThePartialExport() = runTest {
        val dir = Files.createTempDirectory("cancel-csv").toFile()
        try {
            try {
                ExportFiles.writeSuspending(dir, 1) {
                    it.append("partial")
                    throw CancellationException("screen closed")
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) { }
            assertTrue(dir.listFiles()!!.isEmpty())
        } finally { dir.deleteRecursively() }
    }
    @Test fun repeatedBatchFailsRatherThanLoopingForever() = runTest {
        val row = CabinTelemetry(1, 1, 38.0, 1000.0, 1000, false)
        try {
            TelemetryCsvWriter.writeBatches(StringBuilder()) { listOf(row) }
            fail("Expected no-progress failure")
        } catch (_: IllegalStateException) { }
    }
}
