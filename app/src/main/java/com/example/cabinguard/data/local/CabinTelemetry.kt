package com.example.cabinguard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** khai báo entity cho bảng cabin_telemetry */
@Entity(tableName = "cabin_telemetry")
data class CabinTelemetry(

    /** khai báo khóa chính autoGenerate = true */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** khai báo timestamp mặc định là thời gian hiện tại */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    /** khai báo các cột còn lại */
    @ColumnInfo(name = "temperature")
    val temperature: Float = 0f,

    @ColumnInfo(name = "pressure")
    val pressure: Float = 0f,

    @ColumnInfo(name = "co2_level")
    val co2Level: Float = 0f,

    @ColumnInfo(name = "is_warning")
    val isWarning: Boolean = false
) {
    /** khai báo các giá trị ngưỡng cho các chỉ số */
    companion object {
        const val TEMP_WARNING_THRESHOLD  = 38f    
        const val CO2_WARNING_THRESHOLD   = 1000f  
    }
}
