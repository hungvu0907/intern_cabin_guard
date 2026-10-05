package com.example.cabinguard.ui.widget

import com.example.cabinguard.data.local.CabinTelemetry
import java.util.Locale

/**
 * Nội dung widget Home. Tôn trọng [CabinTelemetry.isWarning] đã lưu,
 * không tự so lại ngưỡng 38°C / 1000 ppm.
 */
data class CabinWidgetUi(
    val temperatureText: String,
    val statusText: String,
    val isWarning: Boolean,
)

fun CabinTelemetry?.toCabinWidgetUi(): CabinWidgetUi {
    if (this == null) {
        return CabinWidgetUi(
            temperatureText = "--",
            statusText = "Chưa có dữ liệu",
            isWarning = false,
        )
    }
    return CabinWidgetUi(
        temperatureText = "${String.format(Locale.US, "%.1f", temperature)}°C",
        statusText = if (isWarning) "Cảnh báo" else "An toàn",
        isWarning = isWarning,
    )
}
