package com.example.cabinguard.data.repository

import com.example.cabinguard.data.cloud.CloudSyncOutcome
import com.example.cabinguard.data.cloud.TelemetryCloudApi
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.local.CabinTelemetryDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Luồng dữ liệu:
 *   SensorEngine → Repository.saveTelemetry() → DAO.insert() → SQLite
 *   ViewModel    ← Repository.getAllLogs()     ← DAO.getAllLogs() ← SQLite
 *   SyncWorker   → syncPendingToCloud()        → cloud upsert theo id → markSynced
 */
@Singleton
class CabinTelemetryRepository @Inject constructor(
    private val dao: CabinTelemetryDao,
    private val cloudApi: TelemetryCloudApi,
) {
    suspend fun saveTelemetry(telemetry: CabinTelemetry) {
        dao.insert(telemetry)
    }

    /**
     * Xóa toàn bộ log cũ hơn [hours] giờ.
     * Được gọi bởi: CleanupWorker (WorkManager) định kỳ mỗi 24h.
     */
    suspend fun deleteLogsOlderThan(hours: Long = 24) {
        val cutoffTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours)
        dao.deleteOlderThan(cutoffTimestamp)
    }

    fun getAllLogs(): Flow<List<CabinTelemetry>> = dao.getAllLogs()

    fun getRecentLogs(limit: Int = 50): Flow<List<CabinTelemetry>> = dao.getRecentLogs(limit)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    /**
     * Đẩy mọi bản ghi chưa sync. Upsert theo id rồi mới đánh dấu Room.
     * Cloud lỗi hoặc ghi Room lỗi → [CloudSyncOutcome.Failed], isSynced giữ false
     * với các dòng chưa mark (lần sau ghi đè cùng id).
     */
    suspend fun syncPendingToCloud(): CloudSyncOutcome {
        val pending = dao.getUnsynced()
        if (pending.isEmpty()) return CloudSyncOutcome.NothingToSync
        return try {
            cloudApi.upsertAll(pending)
            pending.map { it.id }
                .chunked(MARK_SYNCED_CHUNK)
                .forEach { chunk -> dao.markSynced(chunk) }
            CloudSyncOutcome.Success(pending.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CloudSyncOutcome.Failed(e)
        }
    }

    private companion object {
        const val MARK_SYNCED_CHUNK = 500
    }
}
