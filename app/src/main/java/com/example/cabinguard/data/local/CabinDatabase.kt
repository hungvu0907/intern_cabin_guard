package com.example.cabinguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CabinTelemetry::class],   // Danh sách bảng trong DB
    version = 1,                           // Phiên bản schema hiện tại
    exportSchema = false                   // Tắt export JSON schema (bật khi production)
)
abstract class CabinDatabase : RoomDatabase() {

    /* * Chỉ gọi qua Hilt injection, không gọi trực tiếp từ nơi khác.*/
    abstract fun cabinTelemetryDao(): CabinTelemetryDao

    companion object {
        /** Tên file database được tạo trên thiết bị: /data/data/<package>/databases/cabin_guard_db */
        const val DATABASE_NAME = "cabin_guard_db"
    }
}
