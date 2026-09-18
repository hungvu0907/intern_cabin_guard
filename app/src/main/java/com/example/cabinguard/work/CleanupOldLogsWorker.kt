package com.example.cabinguard.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cabinguard.data.local.CabinTelemetryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanupOldLogsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: CabinTelemetryDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() -
            TimeUnit.HOURS.toMillis(RETENTION_HOURS)
        val deleted = dao.deleteOlderThan(cutoff)
        Log.d(TAG, "deleted $deleted rows older than ${RETENTION_HOURS}h")
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "cleanup_old_cabin_logs"
        const val TAG = "CleanupOldLogsWorker"
        private const val RETENTION_HOURS = 24L

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<CleanupOldLogsWorker>(
                RETENTION_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
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
