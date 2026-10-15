package com.example.cabinguard

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.data.remote.TelemetryRemoteDataSource
import com.example.cabinguard.domain.sync.SyncTelemetryUseCase
import com.example.cabinguard.work.SyncWorker
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("RestrictedApi")
@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: CabinDatabase
    private var testWorkManagerInitialized = false

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, CabinDatabase::class.java).build()
    }

    @After fun tearDown() {
        if (testWorkManagerInitialized) {
            WorkManager.getInstance(context).cancelAllWork().result.get(10, TimeUnit.SECONDS)
            WorkManagerTestInitHelper.closeWorkDatabase()
            WorkManagerImpl.setDelegate(null)
        }
        db.close()
    }

    private fun factory(remote: TelemetryRemoteDataSource) = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
            if (workerClassName == SyncWorker::class.java.name) {
                SyncWorker(appContext, workerParameters, SyncTelemetryUseCase(db.telemetryDao(), remote))
            } else null
    }

    private fun worker(remote: TelemetryRemoteDataSource) = TestListenableWorkerBuilder<SyncWorker>(context)
        .setWorkerFactory(factory(remote))
        .build()

    private suspend fun insertRows(count: Int) {
        for (id in 1..count) {
            db.telemetryDao().insert(CabinTelemetry(id.toLong(), id.toLong(), 38.0, 1000.0, 1000, false))
        }
    }

    @Test fun successUploadsEveryBatchAndMarksOnlyAcknowledgedRowsSynced() = runBlocking {
        insertRows(503)
        val uploaded = mutableListOf<List<CabinTelemetry>>()
        val remote = object : TelemetryRemoteDataSource {
            override suspend fun upsert(records: List<CabinTelemetry>) { uploaded += records }
        }
        assertEquals(ListenableWorker.Result.success(), worker(remote).doWork())
        assertEquals(listOf(500, 3), uploaded.map { it.size })
        assertEquals((1L..503L).toList(), uploaded.flatten().map { it.id })
        assertTrue(db.telemetryDao().getUnsynced(1000).isEmpty())
        assertTrue(db.telemetryDao().getAllLogs().all { it.isSynced })
    }

    @Test fun retryAfterPartialUploadKeepsFailedBatchAndCompletesWithoutDuplicateDocuments() = runBlocking {
        insertRows(501)
        var uploads = 0
        var failSecondBatch = true
        val documents = mutableMapOf<Long, CabinTelemetry>()
        val remote = object : TelemetryRemoteDataSource {
            override suspend fun upsert(records: List<CabinTelemetry>) {
                uploads++
                if (failSecondBatch && uploads == 2) throw IOException("Simulated upload failure")
                records.forEach { documents[it.id] = it }
            }
        }
        assertEquals(ListenableWorker.Result.retry(), worker(remote).doWork())
        assertEquals(listOf(501L), db.telemetryDao().getUnsynced(1000).map { it.id })
        assertEquals(500, documents.size)
        assertEquals(500, db.telemetryDao().getAllLogs().count { it.isSynced })

        failSecondBatch = false
        assertEquals(ListenableWorker.Result.success(), worker(remote).doWork())
        assertEquals((1L..501L).toSet(), documents.keys)
        assertTrue(db.telemetryDao().getUnsynced(1000).isEmpty())
    }

    @Test fun cancellationPropagatesAndLeavesTheUnacknowledgedBatchUnsynced() = runBlocking {
        insertRows(1)
        val cancelled = CancellationException("Work stopped")
        val remote = object : TelemetryRemoteDataSource {
            override suspend fun upsert(records: List<CabinTelemetry>) { throw cancelled }
        }
        try {
            worker(remote).doWork()
            fail("Worker must propagate cancellation instead of returning retry")
        } catch (error: CancellationException) {
            assertSame(cancelled, error)
        }
        assertEquals(listOf(1L), db.telemetryDao().getUnsynced(500).map { it.id })
    }

    @Test fun periodicRequestRequiresConnectedNetworkAndKeepsExistingUniqueSchedule() = runBlocking {
        val request = SyncWorker.createRequest()
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(TimeUnit.MINUTES.toMillis(15), request.workSpec.intervalDuration)
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals(TimeUnit.SECONDS.toMillis(30), request.workSpec.backoffDelayDuration)

        insertRows(1)
        val remote = object : TelemetryRemoteDataSource {
            override suspend fun upsert(records: List<CabinTelemetry>) {
                fail("Test driver has not satisfied the required network constraint")
            }
        }
        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setTaskExecutor(SynchronousExecutor())
            .setWorkerFactory(factory(remote))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        testWorkManagerInitialized = true
        val manager = WorkManager.getInstance(context)

        SyncWorker.enqueue(context)
        val first = manager.getWorkInfosForUniqueWork(SyncWorker.UNIQUE_WORK_NAME).get(10, TimeUnit.SECONDS).single()
        assertEquals(WorkInfo.State.ENQUEUED, first.state)
        assertEquals(NetworkType.CONNECTED, first.constraints.requiredNetworkType)

        SyncWorker.enqueue(context)
        val afterSecondEnqueue = manager.getWorkInfosForUniqueWork(SyncWorker.UNIQUE_WORK_NAME).get(10, TimeUnit.SECONDS)
        assertEquals(listOf(first.id), afterSecondEnqueue.map { it.id })
        assertEquals(listOf(1L), db.telemetryDao().getUnsynced(500).map { it.id })
    }
}
