package com.example.cabinguard.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CabinDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CabinDatabase::class.java,
    )

    @Test
    fun migrate2To3PreservesRowsAndDefaultsIsSyncedToFalse() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO cabin_telemetry
                    (id, timestamp, temperature, pressure, co2_level, is_warning)
                VALUES (7, 1700000000000, 39.5, 1000.0, 1100.0, 1)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            CabinDatabaseMigrations.MIGRATION_2_3,
        ).use { database ->
            database.query("SELECT * FROM cabin_telemetry WHERE id = 7").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1_700_000_000_000L, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
                assertEquals(39.5f, cursor.getFloat(cursor.getColumnIndexOrThrow("temperature")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_synced")))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
