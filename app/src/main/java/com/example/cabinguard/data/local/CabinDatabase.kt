package com.example.cabinguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cabinguard.data.model.CabinTelemetry


@Database(
    entities = [CabinTelemetry::class],
    version = 2,
    exportSchema = true
)
abstract class CabinDatabase :
    RoomDatabase() {

    abstract fun telemetryDao():
        CabinTelemetryDao
}