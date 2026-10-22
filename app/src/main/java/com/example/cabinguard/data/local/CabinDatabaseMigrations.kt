package com.example.cabinguard.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * [SYNC-02] Nâng schema, giữ log Sprint 1.
 * v1 → v2: index timestamp. v2 → v3: is_synced. v3 → v4: cờ cảnh báo từng chỉ số.
 */
object CabinDatabaseMigrations {

    const val TABLE = "cabin_telemetry"
    const val COLUMN_IS_SYNCED = "is_synced"
    const val ADD_TIMESTAMP_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS `index_cabin_telemetry_timestamp` ON `$TABLE` (`timestamp`)"
    const val ADD_IS_SYNCED_SQL =
        "ALTER TABLE `$TABLE` ADD COLUMN `$COLUMN_IS_SYNCED` INTEGER NOT NULL DEFAULT 0"
    const val ADD_IS_TEMP_WARNING_SQL =
        "ALTER TABLE `$TABLE` ADD COLUMN `is_temp_warning` INTEGER"
    const val ADD_IS_CO2_WARNING_SQL =
        "ALTER TABLE `$TABLE` ADD COLUMN `is_co2_warning` INTEGER"

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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(ADD_IS_TEMP_WARNING_SQL)
            db.execSQL(ADD_IS_CO2_WARNING_SQL)
        }
    }
}
