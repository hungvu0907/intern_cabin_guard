package com.example.cabinguard.data.export

import java.io.File

/**
 * [EXP-02] Ghi CSV vào cache/export — không đụng external storage.
 * Ghi đè cùng một tên file để lần xuất sau không chất file cũ trong cache.
 */
object TelemetryCsvCache {

    const val DIRECTORY_NAME = "export"
    const val FILE_NAME = "cabinguard_history.csv"

    fun write(cacheDir: File, csv: String): File {
        val dir = File(cacheDir, DIRECTORY_NAME).apply { mkdirs() }
        return File(dir, FILE_NAME).apply {
            writeText(csv, Charsets.UTF_8)
        }
    }
}
