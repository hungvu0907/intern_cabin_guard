package com.example.cabinguard.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cabinguard.data.cloud.CloudSyncOutcome
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Đẩy log isSynced=false lên cloud khi máy có mạng. Lỗi thì retry, không đánh dấu synced. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CabinTelemetryRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val outcome = repository.syncPendingToCloud()) {
            is CloudSyncOutcome.Success -> {
                Log.i(TAG, "Đã sync ${outcome.count} bản ghi")
                Result.success()
            }
            CloudSyncOutcome.NothingToSync -> {
                Log.i(TAG, "Không có bản ghi chưa sync")
                Result.success()
            }
            is CloudSyncOutcome.Failed -> {
                Log.w(TAG, "Sync lỗi, giữ isSynced=false", outcome.cause)
                Result.retry()
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "cabin_cloud_sync"
        const val INTERVAL_MINUTES = 15L
        private const val TAG = "CabinSync"
    }
}
