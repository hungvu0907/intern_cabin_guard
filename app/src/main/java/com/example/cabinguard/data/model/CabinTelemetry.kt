package com.example.cabinguard.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cabin_telemetry")
data class CabinTelemetry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val temperature: Double,
    val pressure: Double,
    val co2Level: Int,
    val isWarning: Boolean
)
