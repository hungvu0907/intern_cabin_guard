package com.example.cabinguard.data.export

import android.content.Context
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import com.example.cabinguard.domain.export.TelemetryCsvFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryCsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: CabinTelemetryRepository,
) {
    suspend fun export(): File? = withContext(Dispatchers.IO) {
        var offset = 0
        var page = repository.getLogsPage(PAGE_SIZE, offset)
        if (page.isEmpty()) return@withContext null

        val target = TelemetryCsvCache.targetFile(context.cacheDir)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        try {
            temporary.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.appendLine(TelemetryCsvFormatter.HEADER)
                while (page.isNotEmpty()) {
                    page.forEach { log -> writer.appendLine(TelemetryCsvFormatter.formatRow(log)) }
                    offset += page.size
                    page = repository.getLogsPage(PAGE_SIZE, offset)
                }
            }
            replaceAtomically(temporary, target)
            target
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    private fun replaceAtomically(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), REPLACE_EXISTING)
        } catch (error: SecurityException) {
            throw IOException("Không thể thay thế file CSV", error)
        }
    }

    private companion object {
        const val PAGE_SIZE = 500
    }
}
