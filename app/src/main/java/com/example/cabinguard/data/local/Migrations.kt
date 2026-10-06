package com.example.cabinguard.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: thêm cột isSynced cho Cloud Sync (US-06).
 * Log cũ từ Sprint 1 mặc định là chưa sync để lần đồng bộ đầu đẩy lên hết.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE cabin_telemetry " +
                "ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/** Keep existing rows and speed up bounded history / timestamp-keyset export. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cabin_telemetry_timestamp_id ON cabin_telemetry(timestamp, id)")
    }
}
