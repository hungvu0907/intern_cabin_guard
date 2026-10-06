package com.example.cabinguard.data.local

import androidx.room.*
import com.example.cabinguard.data.model.CabinTelemetry
import kotlinx.coroutines.flow.Flow

@Dao
interface CabinTelemetryDao {

    @Insert
    suspend fun insert(
        telemetry: CabinTelemetry
    )

    @Query(
        """
        SELECT *
        FROM cabin_telemetry
        ORDER BY timestamp DESC
        """
    )
    fun observeAll():
        Flow<List<CabinTelemetry>>

    @Query("SELECT * FROM cabin_telemetry ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CabinTelemetry>>

    @Query("SELECT MAX(id) FROM cabin_telemetry")
    suspend fun maxId(): Long?

    @Query("""
        SELECT * FROM cabin_telemetry
        WHERE id <= :maxId
        AND (timestamp > :afterTimestamp OR (timestamp = :afterTimestamp AND id > :afterId))
        ORDER BY timestamp ASC, id ASC LIMIT :limit
    """)
    suspend fun exportBatch(maxId: Long, afterTimestamp: Long, afterId: Long, limit: Int): List<CabinTelemetry>

    @Query(
        """
        SELECT *
        FROM cabin_telemetry
        ORDER BY timestamp DESC, id DESC
        LIMIT 1
        """
    )
    fun observeLatest():
        Flow<CabinTelemetry?>

    @Query(
        """
        SELECT COUNT(*)
        FROM cabin_telemetry
        """
    )
    fun observeCount():
        Flow<Int>

    @Query(
        """
        SELECT COUNT(*)
        FROM cabin_telemetry
        WHERE isSynced = 0
        """
    )
    fun observeUnsyncedCount():
        Flow<Int>

    @Query(
        """
        SELECT *
        FROM cabin_telemetry
        ORDER BY timestamp ASC
        """
    )
    suspend fun getAllLogs():
        List<CabinTelemetry>

    @Query(
        """
        SELECT *
        FROM cabin_telemetry
        WHERE isSynced = 0
        ORDER BY id ASC
        LIMIT :limit
        """
    )
    suspend fun getUnsynced(
        limit: Int
    ): List<CabinTelemetry>

    @Query(
        """
        UPDATE cabin_telemetry
        SET isSynced = 1
        WHERE id IN (:ids) AND isSynced = 0
        """
    )
    suspend fun markSynced(
        ids: List<Long>
    ): Int

    @Query("SELECT COUNT(*) FROM cabin_telemetry WHERE isSynced = 0 AND timestamp < :cutoff")
    suspend fun countUnsyncedOlderThan(cutoff: Long): Int

    // A hard age limit deliberately trades old offline logs for bounded retention.
    @Query(
        """
        DELETE FROM cabin_telemetry
        WHERE (timestamp < :syncedCutoff AND isSynced = 1)
        OR timestamp < :hardCutoff
        """
    )
    suspend fun deleteExpired(
        syncedCutoff: Long,
        hardCutoff: Long
    ): Int

    @Transaction
    suspend fun cleanup(syncedCutoff: Long, hardCutoff: Long): CleanupResult {
        val unsynced = countUnsyncedOlderThan(hardCutoff)
        return CleanupResult(deleteExpired(syncedCutoff, hardCutoff), unsynced)
    }
}

data class CleanupResult(val deleted: Int, val unsyncedDeleted: Int)
