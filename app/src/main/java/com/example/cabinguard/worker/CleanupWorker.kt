package com.example.cabinguard.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// Worker dọn dẹp log định kỳ: xóa mọi bản ghi cũ hơn 24 giờ khỏi Room.
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: CabinTelemetryRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            repository.deleteLogsOlderThan(RETENTION_HOURS)
            Log.d(TAG, "Đã dọn log cũ hơn $RETENTION_HOURS giờ")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Dọn log thất bại: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "cabin_cleanup_work"
        private const val TAG = "CleanupWorker"
        private const val RETENTION_HOURS = 24L
    }
}
