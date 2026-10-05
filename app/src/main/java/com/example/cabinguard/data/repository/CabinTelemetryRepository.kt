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

    suspend fun cleanupLogs(
        syncedRetentionHours: Long = 24,
        hardLimitHours: Long = 24 * 7,
    ) {
        val now = System.currentTimeMillis()
        dao.deleteSyncedOlderThan(now - TimeUnit.HOURS.toMillis(syncedRetentionHours))
        dao.deleteOlderThanHardLimit(now - TimeUnit.HOURS.toMillis(hardLimitHours))
    }

    fun getAllLogs(): Flow<List<CabinTelemetry>> = dao.getAllLogs()

    suspend fun getLogsPage(limit: Int, offset: Int): List<CabinTelemetry> =
        dao.getLogsPage(limit, offset)

    fun getRecentLogs(limit: Int = 50): Flow<List<CabinTelemetry>> = dao.getRecentLogs(limit)

    /** [WDG-03] Bản ghi mới nhất cho widget Home. */
    fun observeLatest(): Flow<CabinTelemetry?> = dao.observeLatest()

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    /**
     * Đẩy mọi bản ghi chưa sync. Upsert theo id rồi mới đánh dấu Room.
     * Cloud lỗi hoặc ghi Room lỗi → [CloudSyncOutcome.Failed], isSynced giữ false
     * với các dòng chưa mark (lần sau ghi đè cùng id).
     */
    suspend fun syncPendingToCloud(): CloudSyncOutcome {
        val pending = dao.getUnsynced(SYNC_BATCH_SIZE)
        if (pending.isEmpty()) return CloudSyncOutcome.NothingToSync
        return try {
            cloudApi.upsertAll(pending)
            dao.markSynced(pending.map { it.id })
            CloudSyncOutcome.Success(pending.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CloudSyncOutcome.Failed(e)
        }
    }

    private companion object {
        const val SYNC_BATCH_SIZE = 200
    }
}
