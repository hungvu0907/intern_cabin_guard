package com.example.cabinguard.data.local


@Database(
    entities = [CabinTelemetry::class],
    version = 1,
    exportSchema = false
)
abstract class CabinDatabase :
    RoomDatabase() {

    abstract fun telemetryDao():
        CabinTelemetryDao
}