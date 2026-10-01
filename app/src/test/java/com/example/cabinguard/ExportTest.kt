package com.example.cabinguard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.export.TelemetryCsvWriter
import com.example.cabinguard.data.export.ExportFiles
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.time.ZoneId
import java.util.Locale

class ExportTest {
    @Test fun csvUsesStableColumnsAndLocaleAndZone() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val out = StringBuilder()
            TelemetryCsvWriter.write(listOf(CabinTelemetry(7, 0, 38.5, 1000.25, 1500, true, false)), out, ZoneId.of("Asia/Bangkok"))
            assertEquals(TelemetryCsvWriter.HEADER + "\n7,1970-01-01T07:00:00+07:00,38.50,1000.25,1500,true,false\n", out.toString())
            val empty = StringBuilder()
            TelemetryCsvWriter.write(emptyList(), empty)
            assertEquals(TelemetryCsvWriter.HEADER + "\n", empty.toString())
        } finally { Locale.setDefault(previous) }
    }

    @Test fun exportsKeepRecentFilesAndWriteBomAndUseUniqueNames() {
        val dir = Files.createTempDirectory("exports-test").toFile()
        try {
            val now = 100_000_000L
            val old = dir.resolve("old.csv").apply { writeText("old"); setLastModified(now - ExportFiles.RETENTION_MILLIS - 1) }
            val recent = dir.resolve("recent.csv").apply { writeText("shared"); setLastModified(now - 1) }
            val boundary = dir.resolve("boundary.csv").apply { writeText("keep"); setLastModified(now - ExportFiles.RETENTION_MILLIS) }
            val first = ExportFiles.write(dir, now) { it.append("csv") }
            val second = ExportFiles.write(dir, now) { it.append("csv") }
            assertFalse(old.exists())
            assertEquals("shared", recent.readText())
            assertTrue(boundary.exists())
            assertNotEquals(first.name, second.name)
            assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), first.readBytes().take(3).toByteArray())
            val filesBefore = dir.listFiles()!!.map { it.name }.toSet()
            assertThrows(java.io.IOException::class.java) {
                ExportFiles.write(dir, now) { it.append("partial"); throw java.io.IOException("disk full") }
            }
            assertEquals(filesBefore, dir.listFiles()!!.map { it.name }.toSet())
        } finally { dir.deleteRecursively() }
    }
}
