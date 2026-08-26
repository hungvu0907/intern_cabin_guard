package com.example.cabinguard.domain.model

import com.example.cabinguard.data.local.CabinTelemetry

/**
 * [DOMAIN-03] UI State — Sealed class đại diện cho các trạng thái có thể có của Dashboard UI.
 *
 * Sử dụng sealed class để đảm bảo exhaustive when-expression trong Compose,
 * buộc UI phải xử lý tất cả trạng thái có thể xảy ra.
 *
 * TODO: Implement logic chuyển đổi state trong CabinViewModel:
 *   - Normal: nhiệt độ <= 38°C VÀ CO2 <= 1000 ppm
 *   - Warning: nhiệt độ > 38°C HOẶC CO2 > 1000 ppm
 *   - Loading: khi chờ dữ liệu đầu tiên từ SensorEngine
 */
sealed class CabinUiState {

    /** Trạng thái chờ — hiển thị khi chưa có dữ liệu cảm biến */
    data object Loading : CabinUiState()

    /** Trạng thái bình thường — tất cả chỉ số trong ngưỡng an toàn */
    data class Normal(val data: CabinTelemetry) : CabinUiState()

    /** Trạng thái cảnh báo — nhiệt độ > 38°C hoặc CO2 > 1000 ppm */
    data class Warning(val data: CabinTelemetry) : CabinUiState()
}
