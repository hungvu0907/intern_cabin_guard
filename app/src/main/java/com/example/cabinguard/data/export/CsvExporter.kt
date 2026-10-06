package com.example.cabinguard.data.export

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.domain.export.TelemetryCsvWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
        val upperId = dao.maxId() ?: return@withContext null

        val dir = File(context.cacheDir, EXPORT_DIR)
        var count = 0
        val file = ExportFiles.writeSuspending(dir, System.currentTimeMillis()) { writer ->
            count = TelemetryCsvWriter.writeBatches(writer) { last ->
                dao.exportBatch(upperId, last?.timestamp ?: Long.MIN_VALUE, last?.id ?: Long.MIN_VALUE, 1000)
            }
        }
        if (count == 0) {
            file.delete()
            return@withContext null
        }
        Log.d(TAG, "exported $count rows to ${file.name}")

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private companion object {
        const val TAG = "CsvExporter"

        /** Phải khớp path trong res/xml/file_paths.xml. */
        const val EXPORT_DIR = "exports"
    }
}
