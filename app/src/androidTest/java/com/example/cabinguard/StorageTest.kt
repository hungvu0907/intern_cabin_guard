package com.example.cabinguard

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cabinguard.data.local.*
import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.data.remote.*
import com.example.cabinguard.domain.sensor.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val migration = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CabinDatabase::class.java)
    @get:Rule val mockMigration = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MockCloudDatabase::class.java)

    @Test fun migrationPreservesV1RowsAndDefaultsToUnsynced() {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        try {
            migration.createDatabase(name, 1).apply {
                execSQL("INSERT INTO cabin_telemetry (id,timestamp,temperature,pressure,co2Level,isWarning) VALUES (42,123,39.5,1001.0,1100,1)")
                close()
            }
            migration.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).apply {
                query("SELECT * FROM cabin_telemetry WHERE id=42").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(123L, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
                    assertEquals(39.5, cursor.getDouble(cursor.getColumnIndexOrThrow("temperature")), 0.0)
                    assertEquals(1001.0, cursor.getDouble(cursor.getColumnIndexOrThrow("pressure")), 0.0)
                    assertEquals(1100, cursor.getInt(cursor.getColumnIndexOrThrow("co2Level")))
                    assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isWarning")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isSynced")))
                    assertFalse(cursor.moveToNext())
                }
                close()
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun cleanupRespectsExactCutoffsAndReportsUnsyncedLoss() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CabinDatabase::class.java).build()
        try {
            val dao = db.telemetryDao()
            // synced cutoff=100, hard cutoff=10; strictly older rows only.
            for ((id, time, synced) in listOf(Triple(1, 99, true), Triple(2, 100, true), Triple(3, 99, false), Triple(4, 9, false), Triple(5, 10, false), Triple(6, 9, true))) {
                dao.insert(CabinTelemetry(id.toLong(), time.toLong(), 38.0, 1000.0, 1000, false, synced))
            }
            assertEquals(CleanupResult(3, 1), dao.cleanup(100, 10))
            assertEquals(listOf(5L, 3L, 2L), dao.getAllLogs().map { it.id })
            assertEquals(2, dao.markSynced(listOf(2, 3, 5)))
            assertEquals(0, dao.markSynced(listOf(2, 3, 5)))
        } finally { db.close() }
    }

    @Test fun durableMockSurvivesReopenAndUpsertsWithoutDuplicates() = runBlocking {
        val name = "mock-reopen-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, MockCloudDatabase::class.java, name).build()
        var db = open()
        try {
            val row = CabinTelemetry(42, 123, 39.0, 1000.0, 1100, true)
            MockTelemetryRemote(db).upsert(listOf(row))
            db.close()
            db = open()
            assertEquals(listOf(row.copy(isSynced = true)), db.documents().records())
            MockTelemetryRemote(db).upsert(listOf(row.copy(temperature = 40.0)))
            assertEquals(listOf(row.copy(temperature = 40.0, isSynced = true)), db.documents().records())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun thresholdReevaluationDoesNotInsertDuplicateAndIdenticalTimestampsAreDistinctSamples() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CabinDatabase::class.java).build()
        try {
            val recorder = TelemetryRecorder(db.telemetryDao())
            val row = CabinTelemetry(timestamp = 123, temperature = 40.0, pressure = 1000.0, co2Level = 1100, isWarning = false)
            recorder.record(EvaluatedSample(1, row))
            recorder.record(EvaluatedSample(1, row.copy(isWarning = true)))
            assertEquals(1, db.telemetryDao().getAllLogs().size)
            recorder.record(EvaluatedSample(2, row.copy(isWarning = true)))
            val rows = db.telemetryDao().getAllLogs()
            assertEquals(2, rows.size)
            assertNotEquals(rows[0].id, rows[1].id)
        } finally { db.close() }
    }

    @Test fun recentHistoryIsBoundedAndExportIsOrderedAndExcludesNewRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CabinDatabase::class.java).build()
        try {
            val dao = db.telemetryDao()
            for (id in 1..503) {
                dao.insert(CabinTelemetry(id.toLong(), (503 - id).toLong() / 3, 38.0, 1000.0, 1000, false))
            }
            val recent = dao.observeRecent(200).first()
            assertEquals(200, recent.size)
            assertEquals(2L, recent.first().id)
            val upper = dao.maxId()!!
            dao.insert(CabinTelemetry(504, 0, 38.0, 1000.0, 1000, false))
            val exported = mutableListOf<CabinTelemetry>()
            var last: CabinTelemetry? = null
            while (true) {
                val batch = dao.exportBatch(upper, last?.timestamp ?: Long.MIN_VALUE, last?.id ?: Long.MIN_VALUE, 50)
                if (batch.isEmpty()) break
                assertTrue(batch.size <= 50)
                exported += batch
                last = batch.last()
            }
            assertEquals(503, exported.size)
            assertEquals(503, exported.map { it.id }.toSet().size)
            assertTrue(exported.none { it.id == 504L })
            assertEquals(exported.sortedWith(compareBy<CabinTelemetry> { it.timestamp }.thenBy { it.id }), exported)
        } finally { db.close() }
    }

    @Test fun indexMigrationKeepsOldLogsFromBothMainVersions() {
        for (version in listOf(1, 2)) {
            val name = "index-migration-$version.db"
            context.deleteDatabase(name)
            try {
                migration.createDatabase(name, version).apply {
                    val columns = if (version == 1) "id,timestamp,temperature,pressure,co2Level,isWarning" else "id,timestamp,temperature,pressure,co2Level,isWarning,isSynced"
                    val values = if (version == 1) "42,123,39.5,1001.0,1100,1" else "42,123,39.5,1001.0,1100,1,1"
                    execSQL("INSERT INTO cabin_telemetry ($columns) VALUES ($values)")
                    close()
                }
                migration.runMigrationsAndValidate(name, 3, true, MIGRATION_1_2, MIGRATION_2_3).apply {
                    query("SELECT id,isSynced FROM cabin_telemetry").use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        assertEquals(42L, cursor.getLong(0))
                        assertEquals(if (version == 1) 0 else 1, cursor.getInt(1))
                    }
                    query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_cabin_telemetry_timestamp_id'").use {
                        assertTrue(it.moveToFirst())
                    }
                    close()
                }
            } finally { context.deleteDatabase(name) }
        }
    }

    @Test fun mockIndexMigrationKeepsSyncedDocuments() {
        val name = "mock-index-migration.db"
        context.deleteDatabase(name)
        try {
            mockMigration.createDatabase(name, 1).apply {
                execSQL("INSERT INTO cabin_telemetry (id,timestamp,temperature,pressure,co2Level,isWarning,isSynced) VALUES (42,123,39.5,1001.0,1100,1,1)")
                close()
            }
            mockMigration.runMigrationsAndValidate(name, 2, true, MOCK_MIGRATION_1_2).apply {
                query("SELECT id,isSynced FROM cabin_telemetry").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(42L, cursor.getLong(0))
                    assertEquals(1, cursor.getInt(1))
                }
                close()
            }
        } finally { context.deleteDatabase(name) }
    }
}
