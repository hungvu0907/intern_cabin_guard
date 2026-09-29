package com.example.cabinguard.domain.sync

import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.remote.TelemetryRemoteDataSource
import javax.inject.Inject

/**
 * Đẩy mọi bản ghi isSynced = false lên cloud theo từng batch.
 *
 * Chỉ đánh dấu isSynced sau khi upload thành công. Nếu app bị kill giữa
 * upsert và markSynced, lần chạy sau gửi lại đúng các document ID đó và
 * remote ghi đè, nên không có bản trùng (idempotent).
 */
class SyncTelemetryUseCase @Inject constructor(
    private val dao: CabinTelemetryDao,
    private val remote: TelemetryRemoteDataSource
) {

    /** Trả về số bản ghi đã đồng bộ; ném exception nếu upload lỗi. */
    suspend operator fun invoke(): Int {
        var synced = 0
        while (true) {
            val batch = dao.getUnsynced(BATCH_SIZE)
            if (batch.isEmpty()) return synced
            remote.upsert(batch)
            dao.markSynced(batch.map { it.id })
            synced += batch.size
        }
    }

    companion object {
        /** Giới hạn một batch write của Firestore. */
        const val BATCH_SIZE = 500
    }
}
