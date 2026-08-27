package com.example.cabinguard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CabinTelemetryDao {
    /** Lưu 1 bản ghi dữ liệu sensor vào DB */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(telemetry: CabinTelemetry)

    /** Lấy toàn bộ history và sắp xếp theo thời gian mới nhất */
    @Query("SELECT * FROM cabin_telemetry ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CabinTelemetry>>

    /** Lấy số lượng log giới hạn */
    @Query("SELECT * FROM cabin_telemetry ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<CabinTelemetry>>

    /** Xóa các log cũ hơn thời gian hiện tại */
    @Query("DELETE FROM cabin_telemetry WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /** Đếm tổng số log trong DB */
    @Query("SELECT COUNT(*) FROM cabin_telemetry")
    fun getTotalCount(): Flow<Int>

    /** Xóa toàn bộ dữ liệu trong DB (sử dụng khi cần reset app hoặc test) */
    @Query("DELETE FROM cabin_telemetry")
    suspend fun clearAll()
}
