package com.example.cabinguard.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CabinTelemetryDaoTest {
    private lateinit var database: CabinDatabase
    private lateinit var dao: CabinTelemetryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CabinDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.cabinTelemetryDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun getUnsyncedAndMarkSyncedUseRealRoomQueries() = runTest {
        dao.insert(sample(id = 1, timestamp = 20, isSynced = false))
        dao.insert(sample(id = 2, timestamp = 10, isSynced = false))
        dao.insert(sample(id = 3, timestamp = 30, isSynced = true))

        val pending = dao.getUnsynced(limit = 10)
        assertEquals(listOf(2L, 1L), pending.map { it.id })

        dao.markSynced(listOf(2L))
        val remaining = dao.getUnsynced(limit = 10)
        assertEquals(listOf(1L), remaining.map { it.id })
        assertTrue(pending.first { it.id == 2L }.isSynced.not())
        assertFalse(remaining.first().isSynced)
    }

    @Test
    fun normalCleanupKeepsUnsyncedRowsUntilHardLimit() = runTest {
        dao.insert(sample(id = 1, timestamp = 10, isSynced = false))
        dao.insert(sample(id = 2, timestamp = 10, isSynced = true))

        dao.deleteSyncedOlderThan(timestamp = 100)

        assertEquals(listOf(1L), dao.getLogsPage(limit = 10, offset = 0).map { it.id })

        dao.deleteOlderThanHardLimit(timestamp = 100)
        assertTrue(dao.getLogsPage(limit = 10, offset = 0).isEmpty())
    }

    private fun sample(id: Long, timestamp: Long, isSynced: Boolean) = CabinTelemetry(
        id = id,
        timestamp = timestamp,
        temperature = 35f,
        pressure = 1_000f,
        co2Level = 800f,
        isWarning = false,
        isSynced = isSynced,
    )
}
