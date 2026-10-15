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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Mock-only storage: one JSON per record, retained up to a fixed file count (not a durable cloud). */
@Singleton
class MockRestTelemetryCloudApi internal constructor(
    private val directory: File,
    private val deviceId: String,
    private val maxDocuments: Int = MAX_DOCUMENTS,
    private val maxCorruptBackups: Int = MAX_CORRUPT_BACKUPS,
) : TelemetryCloudApi {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        directory = File(context.filesDir, RELATIVE_DIRECTORY),
        deviceId = getOrCreateDeviceId(context),
    )

    private val lock = Any()
    private val safeDeviceId = deviceId.replace(Regex("[^A-Za-z0-9._-]"), "_")

    init {
        require(maxDocuments > 0)
        require(maxCorruptBackups >= 0)
    }

    override suspend fun upsertAll(records: List<CabinTelemetry>) {
        if (records.isEmpty()) return
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                ensureDirectory()
                records.chunked(WRITE_CHUNK_SIZE).forEach { chunk ->
                    chunk.forEach(::writeDocument)
                }
                pruneFiles()
                Log.i(TAG, "Upsert ${records.size} bản ghi → ${directory.absolutePath}")
            }
        }
    }

    internal fun documentFile(id: Long): File = File(directory, "${safeDeviceId}_$id.json")

    private fun writeDocument(record: CabinTelemetry) {
        val target = documentFile(record.id)
        recoverCorruptDocument(target)
        val json = JSONObject()
            .put("deviceId", deviceId)
            .put("id", record.id)
            .put("timestamp", record.timestamp)
            .put("temperature", record.temperature.toDouble())
            .put("pressure", record.pressure.toDouble())
            .put("co2Level", record.co2Level.toDouble())
            .put("isWarning", record.isWarning)
            .put("isTempWarning", record.isTempWarning ?: JSONObject.NULL)
            .put("isCo2Warning", record.isCo2Warning ?: JSONObject.NULL)
        val temporary = File(directory, "${target.name}.tmp")
        try {
            temporary.writeText(json.toString(2), Charsets.UTF_8)
            replaceAtomically(temporary, target)
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    /** Preserve a corrupt payload, then let the current upsert replace it cleanly. */
    private fun recoverCorruptDocument(target: File) {
        if (!target.exists() || target.length() == 0L) return
        try {
            JSONObject(target.readText(Charsets.UTF_8))
        } catch (error: JSONException) {
            val backup = File(directory, "${target.name}.corrupt-${System.currentTimeMillis()}")
            Files.move(target.toPath(), backup.toPath(), REPLACE_EXISTING)
            Log.e(TAG, "Đã cách ly JSON hỏng: ${backup.name}", error)
        }
    }

    private fun replaceAtomically(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), REPLACE_EXISTING)
        }
    }

    private fun ensureDirectory() {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Không tạo được ${directory.absolutePath}")
        }
    }

    /** Only mock-generated files are eligible; unrelated files in the directory are untouched. */
    private fun pruneFiles() {
        val files = directory.listFiles() ?: throw IOException("Không đọc được ${directory.absolutePath}")
        pruneOldest(files.filter { it.isFile && DOCUMENT_NAME.matches(it.name) }, maxDocuments)
        pruneOldest(files.filter { it.isFile && CORRUPT_NAME.matches(it.name) }, maxCorruptBackups)
    }

    private fun pruneOldest(files: List<File>, limit: Int) {
        if (files.size <= limit) return
        files.sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
            .drop(limit)
            .forEach { file ->
                if (!file.delete()) Log.w(TAG, "Không xóa được mock file cũ: ${file.name}")
            }
    }

    companion object {
        const val RELATIVE_DIRECTORY = "mock_cloud/telemetry"
        // Deliberate mock retention: old synced documents and corrupt backups may be discarded.
        internal const val MAX_DOCUMENTS = 10_000
        internal const val MAX_CORRUPT_BACKUPS = 20
        private const val PREFERENCES_NAME = "mock_cloud_identity"
        private const val DEVICE_ID_KEY = "device_id"
        private const val WRITE_CHUNK_SIZE = 50
        private const val TAG = "CabinSync"
        private val DOCUMENT_NAME = Regex("[A-Za-z0-9._-]+_[0-9]+\\.json")
        private val CORRUPT_NAME = Regex("[A-Za-z0-9._-]+_[0-9]+\\.json\\.corrupt-[0-9]+")

        private fun getOrCreateDeviceId(context: Context): String {
            val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            preferences.getString(DEVICE_ID_KEY, null)?.let { return it }
            return UUID.randomUUID().toString().also { generated ->
                preferences.edit().putString(DEVICE_ID_KEY, generated).apply()
            }
        }
    }
}
