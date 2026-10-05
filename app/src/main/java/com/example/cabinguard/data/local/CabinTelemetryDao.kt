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

    /** Đọc lịch sử theo trang để export không nạp toàn bộ database vào RAM. */
    @Query("SELECT * FROM cabin_telemetry ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getLogsPage(limit: Int, offset: Int): List<CabinTelemetry>

    /** Lấy số lượng log giới hạn */
    @Query("SELECT * FROM cabin_telemetry ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<CabinTelemetry>>

    /** [WDG-03] Một bản ghi mới nhất — widget không gọi DAO trực tiếp. */
    @Query("SELECT * FROM cabin_telemetry ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<CabinTelemetry?>

    /** Dọn bình thường chỉ xóa dữ liệu đã được đồng bộ. */
    @Query("DELETE FROM cabin_telemetry WHERE is_synced = 1 AND timestamp < :timestamp")
    suspend fun deleteSyncedOlderThan(timestamp: Long)

    /** Trần cứng để database không tăng vô hạn khi cloud hỏng kéo dài. */
    @Query("DELETE FROM cabin_telemetry WHERE timestamp < :timestamp")
    suspend fun deleteOlderThanHardLimit(timestamp: Long)

    /** Đếm tổng số log trong DB */
    @Query("SELECT COUNT(*) FROM cabin_telemetry")
    fun getTotalCount(): Flow<Int>

    /** Xóa toàn bộ dữ liệu trong DB (sử dụng khi cần reset app hoặc test) */
    @Query("DELETE FROM cabin_telemetry")
    suspend fun clearAll()

    /** Bản ghi chưa có trên cloud, cũ trước để sync theo thứ tự ghi. */
    @Query("SELECT * FROM cabin_telemetry WHERE is_synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int): List<CabinTelemetry>

    /** Đánh dấu đã sync. Gọi với danh sách id không rỗng. */
    @Query("UPDATE cabin_telemetry SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
