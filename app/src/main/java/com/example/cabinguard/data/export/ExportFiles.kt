package com.example.cabinguard.data.export

import java.io.File
import java.io.Writer

/** File lifecycle is separate from Android sharing so it can be tested on JVM. */
object ExportFiles {
    const val RETENTION_MILLIS = 24 * 60 * 60 * 1000L

    fun write(directory: File, now: Long, content: (Writer) -> Unit): File {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create export directory" }
        directory.listFiles()?.filter { it.isFile && it.extension == "csv" && it.lastModified() < now - RETENTION_MILLIS }
            ?.forEach { it.delete() }
        val file = File.createTempFile("cabin_log_${now}_", ".csv", directory)
        try {
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(0xFEFF)
                content(writer)
            }
            return file
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }
}
