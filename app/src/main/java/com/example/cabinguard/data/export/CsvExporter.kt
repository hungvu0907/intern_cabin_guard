package com.example.cabinguard.data.export

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.domain.export.TelemetryCsvWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Xuất toàn bộ log ra file CSV trong cache và trả content:// URI để chia sẻ
 * qua FileProvider (US-07).
 */
@Singleton
class CsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: CabinTelemetryDao
) {

    /** Trả null khi chưa có log nào để xuất. */
    suspend fun export(): Uri? = withContext(Dispatchers.IO) {
        val rows = dao.getAllLogs()
        if (rows.isEmpty()) return@withContext null

        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        // Chỉ giữ file mới nhất để cache không phình sau nhiều lần xuất.
        dir.listFiles()?.forEach { it.delete() }

        val file = File(dir, "cabin_log_${LocalDateTime.now().format(FILE_TIME)}.csv")
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // BOM để Excel nhận đúng UTF-8.
            writer.write(UTF8_BOM)
            TelemetryCsvWriter.write(rows, writer)
        }
        Log.d(TAG, "exported ${rows.size} rows to ${file.name}")

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private companion object {
        const val TAG = "CsvExporter"
        const val UTF8_BOM = 0xFEFF

        /** Phải khớp path trong res/xml/file_paths.xml. */
        const val EXPORT_DIR = "exports"
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
