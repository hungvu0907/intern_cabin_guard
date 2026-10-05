package com.example.cabinguard.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.cabinguard.CabinGuardApplication
import com.example.cabinguard.data.cloud.TelemetryCloudApi
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    @Test
    fun syncWorkerUploadsAndMarksPendingRows() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, CabinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = database.cabinTelemetryDao()
            dao.insert(sample(id = 11))
            val cloud = RecordingCloudApi()
            val repository = CabinTelemetryRepository(dao, cloud)
            val worker = TestListenableWorkerBuilder<SyncWorker>(context)
                .setWorkerFactory(SyncWorkerFactory(repository))
                .build()

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(listOf(11L), cloud.uploaded.map { it.id })
            assertTrue(dao.getUnsynced(10).isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun periodicSyncRequiresConnectedNetwork() {
        val request = CabinGuardApplication.buildSyncWorkRequest()
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    private class SyncWorkerFactory(
        private val repository: CabinTelemetryRepository,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? = if (workerClassName == SyncWorker::class.java.name) {
            SyncWorker(appContext, workerParameters, repository)
        } else {
            null
        }
    }

    private class RecordingCloudApi : TelemetryCloudApi {
        val uploaded = mutableListOf<CabinTelemetry>()
        override suspend fun upsertAll(records: List<CabinTelemetry>) {
            uploaded += records
        }
    }

    private fun sample(id: Long) = CabinTelemetry(
        id = id,
        timestamp = 1_700_000_000_000L + id,
        temperature = 36f,
        pressure = 1_000f,
        co2Level = 800f,
        isWarning = false,
    )
}
