package com.example.cabinguard.data.remote

import android.util.Log
import com.example.cabinguard.data.model.CabinTelemetry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud giả lập khi chưa setup Firebase (assignment cho phép REST mock).
 * Database riêng trên thiết bị; không phải backup cloud ngoài thiết bị.
 */
@Singleton
class MockTelemetryRemote @Inject constructor(private val database: MockCloudDatabase) : TelemetryRemoteDataSource {

    override suspend fun upsert(records: List<CabinTelemetry>) {
        require(records.all { it.id > 0 }) { "Only persisted local records can be synced" }
        database.documents().upsert(records.map { it.copy(isSynced = true) })
        Log.d(TAG, "persisted ${records.size} records in local mock cloud")
    }

    private companion object {
        const val TAG = "MockTelemetryRemote"
    }
}
