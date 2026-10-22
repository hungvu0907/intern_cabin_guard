package com.example.cabinguard.worker

import android.content.Context
import android.os.SystemClock
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
        val startedAt = SystemClock.elapsedRealtime()
        var syncedCount = 0
        while (true) {
            when (val outcome = repository.syncPendingToCloud()) {
                is CloudSyncOutcome.Success -> {
                    syncedCount += outcome.count
                    Log.i(TAG, "Đã sync $syncedCount bản ghi trong lượt chạy này")
                    // WorkManager giới hạn thời gian chạy; phần còn lại sẽ được xử lý ở kỳ tiếp theo.
                    if (SystemClock.elapsedRealtime() - startedAt >= MAX_RUN_DURATION_MS) {
                        Log.i(TAG, "Đạt giới hạn thời gian sync, tiếp tục ở kỳ sau")
                        return Result.success()
                    }
                }
                CloudSyncOutcome.NothingToSync -> {
                    Log.i(TAG, "Đã xử lý hết backlog ($syncedCount bản ghi)")
                    return Result.success()
                }
                is CloudSyncOutcome.Failed -> {
                    Log.w(TAG, "Sync lỗi sau $syncedCount bản ghi, giữ các dòng còn lại chưa sync", outcome.cause)
                    return Result.retry()
                }
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "cabin_cloud_sync"
        const val INTERVAL_MINUTES = 15L
        private const val MAX_RUN_DURATION_MS = 8L * 60L * 1_000L
        private const val TAG = "CabinSync"
    }
}
