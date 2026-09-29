package com.example.cabinguard.data.remote

import com.example.cabinguard.data.model.CabinTelemetry

/**
 * Nơi lưu log trên cloud. SyncWorker chỉ biết interface này nên đổi từ mock
 * sang Firestore không phải sửa logic đồng bộ.
 */
interface TelemetryRemoteDataSource {

    /**
     * Ghi đè theo document ID = id local, nên gọi lại nhiều lần với cùng bản
     * ghi vẫn không tạo bản trùng. Ném exception nếu upload thất bại.
     */
    suspend fun upsert(records: List<CabinTelemetry>)
}
