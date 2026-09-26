package com.example.cabinguard.data.cloud

import android.content.Context
import android.util.Log
import com.example.cabinguard.data.local.CabinTelemetry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REST mock: PUT theo id vào file JSON (một object = một document).
 * Không gọi mạng thật — Worker vẫn chỉ chạy khi máy có mạng (constraint CONNECTED).
 */
@Singleton
class MockRestTelemetryCloudApi @Inject constructor(
    @ApplicationContext context: Context,
) : TelemetryCloudApi {

    private val file = File(context.filesDir, RELATIVE_PATH)
    private val lock = Any()

    override suspend fun upsertAll(records: List<CabinTelemetry>) {
        if (records.isEmpty()) return
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val documents = readDocuments()
                CloudDocumentStore.upsert(documents, records)
                writeDocuments(documents)
                Log.i(TAG, "Upsert ${records.size} bản ghi, tổng ${documents.size} → ${file.absolutePath}")
            }
        }
    }

    private fun readDocuments(): MutableMap<Long, CloudTelemetryDocument> {
        if (!file.exists() || file.length() == 0L) return mutableMapOf()
        val json = try {
            JSONObject(file.readText())
        } catch (e: JSONException) {
            throw IOException("File cloud mock hỏng: ${file.name}", e)
        }
        val documents = mutableMapOf<Long, CloudTelemetryDocument>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val obj = json.getJSONObject(keys.next())
            val id = obj.getLong("id")
            documents[id] = CloudTelemetryDocument(
                id = id,
                timestamp = obj.getLong("timestamp"),
                temperature = obj.getDouble("temperature").toFloat(),
                pressure = obj.getDouble("pressure").toFloat(),
                co2Level = obj.getDouble("co2Level").toFloat(),
                isWarning = obj.getBoolean("isWarning"),
            )
        }
        return documents
    }

    private fun writeDocuments(documents: Map<Long, CloudTelemetryDocument>) {
        val json = JSONObject()
        documents.values.sortedBy { it.id }.forEach { doc ->
            json.put(
                doc.id.toString(),
                JSONObject()
                    .put("id", doc.id)
                    .put("timestamp", doc.timestamp)
                    .put("temperature", doc.temperature.toDouble())
                    .put("pressure", doc.pressure.toDouble())
                    .put("co2Level", doc.co2Level.toDouble())
                    .put("isWarning", doc.isWarning),
            )
        }
        val dir = file.parentFile ?: throw IOException("Không có thư mục cloud mock")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Không tạo được ${dir.absolutePath}")
        }
        val tmp = File(dir, "${file.name}.tmp")
        tmp.writeText(json.toString(2))
        if (file.exists() && !file.delete()) {
            tmp.delete()
            throw IOException("Không ghi đè được ${file.name}")
        }
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IOException("Không đổi tên file cloud mock")
        }
    }

    companion object {
        const val RELATIVE_PATH = "mock_cloud/telemetry.json"
        private const val TAG = "CabinSync"
    }
}
