package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.sensor.CabinWarningThresholds

data class DashboardUiState(
    val latest: CabinTelemetry? = null,
    val history: List<CabinTelemetry> = emptyList(),
    val isPaused: Boolean = false,
    val warningThresholds: CabinWarningThresholds = CabinWarningThresholds(),
    val unsyncedCount: Int = 0,
    val loading: Boolean = false,
    val readError: String? = null
) {
    val isWarning: Boolean
        get() = latest?.let { warningThresholds.isWarning(it.temperature, it.co2Level) } == true

    val statusText: String get() = when {
        readError != null -> readError
        loading -> "Đang đọc dữ liệu..."
        latest == null -> "Đang chờ mẫu cảm biến"
        isPaused -> "Đã tạm dừng hiển thị · Service vẫn ghi log"
        isWarning -> "CẢNH BÁO: vượt ngưỡng nhiệt độ hoặc CO₂"
        else -> "Cabin an toàn"
    }
}
