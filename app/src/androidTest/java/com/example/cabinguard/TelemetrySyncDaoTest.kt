package com.example.cabinguard

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.model.CabinTelemetry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelemetrySyncDaoTest {
    @Test fun unsyncedBatchesAreBoundedOrderedByIdAndExcludeAlreadySyncedRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, CabinDatabase::class.java
        ).build()
        try {
            val dao = db.telemetryDao()
            for (id in listOf(5L, 2L, 4L, 1L, 3L)) {
                dao.insert(CabinTelemetry(id, 100L - id, 38.0, 1000.0, 1000, false, isSynced = id == 2L))
            }
            assertEquals(4, dao.observeUnsyncedCount().first())
            assertEquals(emptyList<CabinTelemetry>(), dao.getUnsynced(0))
            assertEquals(listOf(1L, 3L), dao.getUnsynced(2).map { it.id })

            // Unknown, duplicate, and already-synced IDs must not inflate the update count.
            assertEquals(2, dao.markSynced(listOf(1L, 1L, 2L, 3L, 999L)))
            assertEquals(0, dao.markSynced(listOf(1L, 2L, 3L, 999L)))
            assertEquals(2, dao.observeUnsyncedCount().first())
            assertEquals(listOf(4L, 5L), dao.getUnsynced(2).map { it.id })
            assertEquals(2, dao.markSynced(listOf(4L, 5L)))
            assertTrue(dao.getUnsynced(100).isEmpty())
            assertEquals(0, dao.observeUnsyncedCount().first())
            assertEquals(5, dao.getAllLogs().size)
            assertTrue(dao.getAllLogs().all { it.isSynced })
        } finally { db.close() }
    }
}
