package com.example.cabinguard.data.export

import java.io.File

/**
 * Đường dẫn cache dùng chung cho exporter và FileProvider.
 */
object TelemetryCsvCache {

    const val DIRECTORY_NAME = "export"
    const val FILE_NAME = "cabinguard_history.csv"

    fun targetFile(cacheDir: File): File {
        val dir = File(cacheDir, DIRECTORY_NAME).apply { mkdirs() }
        return File(dir, FILE_NAME)
    }
}
