package com.example.cabinguard.data.remote

import androidx.room.*
import com.example.cabinguard.data.model.CabinTelemetry

@Dao
interface MockCloudDao {
    @Upsert
    suspend fun upsert(records: List<CabinTelemetry>)

    @Query("SELECT * FROM cabin_telemetry ORDER BY id")
    suspend fun records(): List<CabinTelemetry>
}

/** Durable local simulation, not an off-device backup. */
@Database(entities = [CabinTelemetry::class], version = 1, exportSchema = true)
abstract class MockCloudDatabase : RoomDatabase() {
    abstract fun documents(): MockCloudDao
}
