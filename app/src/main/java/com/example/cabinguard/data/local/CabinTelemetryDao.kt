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
        DELETE FROM cabin_telemetry
        WHERE timestamp < :cutoff
        """
    )
    suspend fun deleteOlderThan(
        cutoff: Long
    ): Int
}