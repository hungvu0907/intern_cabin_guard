package com.example.cabinguard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CabinTelemetry::class],   // Danh sách bảng trong DB
    version = 2,                           // v2: thêm Index(timestamp)
    exportSchema = false                   // false = không ghi schema JSON ra thư mục; bật true khi cần versioning migration
)
abstract class CabinDatabase : RoomDatabase() {

    /* * Chỉ gọi qua Hilt injection, không gọi trực tiếp từ nơi khác.*/
    abstract fun cabinTelemetryDao(): CabinTelemetryDao

    companion object {
        /** Tên file database được tạo trên thiết bị: /data/data/<package>/databases/cabin_guard_db */
        const val DATABASE_NAME = "cabin_guard_db"
    }
}
