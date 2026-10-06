package com.example.cabinguard.data.remote

import androidx.room.*
import com.example.cabinguard.data.model.CabinTelemetry
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Dao
interface MockCloudDao {
    @Upsert
    suspend fun upsert(records: List<CabinTelemetry>)

    @Query("SELECT * FROM cabin_telemetry ORDER BY id")
    suspend fun records(): List<CabinTelemetry>
}

/** Durable local simulation, not an off-device backup. */
@Database(entities = [CabinTelemetry::class], version = 2, exportSchema = true)
abstract class MockCloudDatabase : RoomDatabase() {
    abstract fun documents(): MockCloudDao
}

val MOCK_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cabin_telemetry_timestamp_id ON cabin_telemetry(timestamp, id)")
    }
}
