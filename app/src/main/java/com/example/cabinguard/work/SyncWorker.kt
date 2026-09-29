package com.example.cabinguard.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cabinguard.domain.sync.SyncTelemetryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncTelemetry: SyncTelemetryUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val synced = syncTelemetry()
            Log.d(TAG, "synced $synced rows")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Bản ghi lỗi vẫn isSynced = false, lần retry sau gửi lại.
            Log.w(TAG, "sync failed, will retry", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sync_cabin_telemetry"
        const val TAG = "SyncWorker"

        /** Chu kỳ tối thiểu WorkManager cho phép với PeriodicWorkRequest. */
        private const val SYNC_INTERVAL_MINUTES = 15L

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            Log.d(TAG, "enqueued unique periodic work")
        }
    }
}
