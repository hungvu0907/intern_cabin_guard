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

    @Query(
        """
        SELECT *
        FROM cabin_telemetry
        ORDER BY timestamp DESC
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
        WHERE id IN (:ids)
        """
    )
    suspend fun markSynced(
        ids: List<Long>
    )

    // Chỉ xóa bản ghi đã lên cloud; mất mạng lâu hơn thời gian giữ log
    // cũng không được làm rớt dữ liệu chưa sync (US-06).
    @Query(
        """
        DELETE FROM cabin_telemetry
        WHERE timestamp < :cutoff
        AND isSynced = 1
        """
    )
    suspend fun deleteSyncedOlderThan(
        cutoff: Long
    ): Int
}