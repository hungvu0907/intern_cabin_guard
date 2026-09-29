package com.example.cabinguard.data.remote

import android.util.Log
import com.example.cabinguard.data.model.CabinTelemetry
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud giả lập khi chưa setup Firebase (assignment cho phép REST mock).
 * Lưu trong bộ nhớ theo document ID giống Firestore `set()`, nên số document
 * trong log cho thấy chạy sync lại không sinh bản trùng.
 */
@Singleton
class MockTelemetryRemote @Inject constructor() : TelemetryRemoteDataSource {

    private val documents = ConcurrentHashMap<String, CabinTelemetry>()

    override suspend fun upsert(records: List<CabinTelemetry>) {
        records.forEach { record ->
            documents[record.id.toString()] = record.copy(isSynced = true)
        }
        Log.d(TAG, "upserted ${records.size} records, cloud now has ${documents.size} documents")
    }

    private companion object {
        const val TAG = "MockTelemetryRemote"
    }
}
