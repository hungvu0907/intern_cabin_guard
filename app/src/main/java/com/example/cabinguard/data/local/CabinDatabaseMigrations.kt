package com.example.cabinguard.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * [SYNC-02] Nâng schema, giữ log Sprint 1.
 * v1 → v2: index timestamp. v2 → v3: cột is_synced (mặc định chưa sync).
 */
object CabinDatabaseMigrations {

    const val TABLE = "cabin_telemetry"
    const val COLUMN_IS_SYNCED = "is_synced"
    const val ADD_TIMESTAMP_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS `index_cabin_telemetry_timestamp` ON `$TABLE` (`timestamp`)"
    const val ADD_IS_SYNCED_SQL =
        "ALTER TABLE `$TABLE` ADD COLUMN `$COLUMN_IS_SYNCED` INTEGER NOT NULL DEFAULT 0"

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_TIMESTAMP_INDEX_SQL)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_IS_SYNCED_SQL)
        }
    }
}
