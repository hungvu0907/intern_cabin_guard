package com.example.cabinguard.data.repository

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.local.CabinTelemetryDao
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Luồng dữ liệu:
 *   SensorEngine → Repository.saveTelemetry() → DAO.insert() → SQLite
 *   ViewModel    ← Repository.getAllLogs()     ← DAO.getAllLogs() ← SQLite
 */
@Singleton
class CabinTelemetryRepository @Inject constructor(
    private val dao: CabinTelemetryDao
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
}
