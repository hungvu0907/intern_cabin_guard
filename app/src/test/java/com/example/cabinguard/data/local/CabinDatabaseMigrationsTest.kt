package com.example.cabinguard.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CabinDatabaseMigrationsTest {

    @Test
    fun `v2 to v3 adds is_synced with default unsynced`() {
        assertEquals(2, CabinDatabaseMigrations.MIGRATION_2_3.startVersion)
        assertEquals(3, CabinDatabaseMigrations.MIGRATION_2_3.endVersion)
        assertEquals(
            "ALTER TABLE `cabin_telemetry` ADD COLUMN `is_synced` INTEGER NOT NULL DEFAULT 0",
            CabinDatabaseMigrations.ADD_IS_SYNCED_SQL,
        )
    }

    @Test
    fun `sprint 1 rows without the column must be migrated`() {
        val sprint1 = listOf("id", "timestamp", "temperature", "pressure", "co2_level", "is_warning")
        assertTrue(CabinDatabaseMigrations.shouldAddIsSynced(sprint1))
    }

    @Test
    fun `does not add is_synced twice if the column already exists`() {
        val alreadyOnThisBranch = listOf(
            "id",
            "timestamp",
            "temperature",
            "pressure",
            "co2_level",
            "is_warning",
            "is_synced",
        )
        assertFalse(CabinDatabaseMigrations.shouldAddIsSynced(alreadyOnThisBranch))
    }

    @Test
    fun `v1 to v2 only creates the timestamp index`() {
        assertEquals(1, CabinDatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, CabinDatabaseMigrations.MIGRATION_1_2.endVersion)
        assertEquals(
            "CREATE INDEX IF NOT EXISTS `index_cabin_telemetry_timestamp` ON `cabin_telemetry` (`timestamp`)",
            CabinDatabaseMigrations.ADD_TIMESTAMP_INDEX_SQL,
        )
    }
}
