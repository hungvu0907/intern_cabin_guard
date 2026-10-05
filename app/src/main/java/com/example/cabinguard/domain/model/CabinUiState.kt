package com.example.cabinguard.domain.model

import com.example.cabinguard.data.local.CabinTelemetry

sealed class CabinUiState {

    /** Trạng thái chờ: Khi app mới mở, chưa có data → UI hiển thị loading spinner */
    data object Loading : CabinUiState()

    /** Trạng thái bình thường: temp và CO2 không vượt ngưỡng đang lưu trong Settings. */
    data class Normal(val data: CabinTelemetry) : CabinUiState()

    /** Trạng thái cảnh báo: temp hoặc CO2 vượt ngưỡng DataStore → UI màu đỏ. */
    data class Warning(val data: CabinTelemetry) : CabinUiState()
}
