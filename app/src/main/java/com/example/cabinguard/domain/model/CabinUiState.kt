package com.example.cabinguard.domain.model

import com.example.cabinguard.data.local.CabinTelemetry

sealed class CabinUiState {

    /** Trạng thái chờ: Khi app mới mở, chưa có data → UI hiển thị loading spinner */
    data object Loading : CabinUiState()

    /** Trạng thái bình thường: Khi temp <= 38°C VÀ CO2 <= 1000 ppm → UI màu xanh (an toàn) */
    data class Normal(val data: CabinTelemetry) : CabinUiState()

    /** Trạng thái cảnh báo: Khi temp > 38°C HOẶC CO2 > 1000 ppm → UI màu đỏ (cảnh báo) */
    data class Warning(val data: CabinTelemetry) : CabinUiState()
}
