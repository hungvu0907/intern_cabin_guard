package com.example.cabinguard.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "cabin_telemetry", indices = [Index(value = ["timestamp", "id"])])
data class CabinTelemetry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val temperature: Double,
    val pressure: Double,
    val co2Level: Int,
    val isWarning: Boolean,
    // defaultValue phải khớp "DEFAULT 0" trong MIGRATION_1_2, nếu lệch Room sẽ
    // báo migration sai ngay khi mở DB trên máy đã có dữ liệu Sprint 1.
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false
)
