package com.example.cabinguard.data.repository

import com.example.cabinguard.data.cloud.CloudSyncOutcome
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.testutil.FakeCabinTelemetryDao
import com.example.cabinguard.testutil.FakeTelemetryCloudApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CabinTelemetryRepositorySyncTest {

    @Test
    fun `successful sync marks rows synced and stores one document per id`() = runTest {
        val dao = FakeCabinTelemetryDao(
            listOf(
                sample(id = 1, temperature = 30f),
                sample(id = 2, temperature = 40f, isSynced = true),
            )
        )
        val cloud = FakeTelemetryCloudApi()
        val repository = CabinTelemetryRepository(dao, cloud)

        val outcome = repository.syncPendingToCloud()

        assertTrue(outcome is CloudSyncOutcome.Success)
        assertEquals(1, (outcome as CloudSyncOutcome.Success).count)
        assertEquals(1, cloud.stored.size)
        assertEquals(30f, cloud.stored.getValue(1).temperature)
        assertTrue(dao.snapshot().single { it.id == 1L }.isSynced)
        assertTrue(dao.getUnsynced().isEmpty())
    }

    @Test
    fun `cloud failure keeps isSynced false`() = runTest {
        val dao = FakeCabinTelemetryDao(listOf(sample(id = 4)))
        val cloud = FakeTelemetryCloudApi().apply { failWith = IOException("timeout") }
        val repository = CabinTelemetryRepository(dao, cloud)

        val outcome = repository.syncPendingToCloud()

        assertTrue(outcome is CloudSyncOutcome.Failed)
        assertFalse(dao.snapshot().single().isSynced)
        assertTrue(cloud.stored.isEmpty())
    }

    @Test
    fun `syncing the same id twice does not create a second remote document`() = runTest {
        val dao = FakeCabinTelemetryDao(listOf(sample(id = 7, temperature = 30f)))
        val cloud = FakeTelemetryCloudApi()
        val repository = CabinTelemetryRepository(dao, cloud)

        repository.syncPendingToCloud()
        dao.replace(sample(id = 7, temperature = 41f, isSynced = false))
        repository.syncPendingToCloud()

        assertEquals(2, cloud.upsertCalls)
        assertEquals(1, cloud.stored.size)
        assertEquals(41f, cloud.stored.getValue(7).temperature)
        assertTrue(dao.snapshot().single().isSynced)
    }

    @Test
    fun `nothing pending does not call cloud`() = runTest {
        val dao = FakeCabinTelemetryDao(listOf(sample(id = 3, isSynced = true)))
        val cloud = FakeTelemetryCloudApi()
        val repository = CabinTelemetryRepository(dao, cloud)

        val outcome = repository.syncPendingToCloud()

        assertTrue(outcome is CloudSyncOutcome.NothingToSync)
        assertEquals(0, cloud.upsertCalls)
    }

    private fun sample(
        id: Long,
        temperature: Float = 36f,
        isSynced: Boolean = false,
    ) = CabinTelemetry(
        id = id,
        timestamp = 1_700_000_000_000L + id,
        temperature = temperature,
        pressure = 1000f,
        co2Level = 800f,
        isWarning = false,
        isSynced = isSynced,
    )
}
