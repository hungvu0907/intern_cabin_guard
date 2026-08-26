package com.example.cabinguard.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cabinguard.domain.model.CabinUiState

/**
 * [UI-01..04] Dashboard Screen — Màn hình chính hiển thị dữ liệu cảm biến realtime.
 *
 * TODO: Implement các Composable theo spec:
 *   - [UI-01] SensorGaugeRow: hiển thị Temperature, Pressure, CO2 cập nhật 1s/lần
 *   - [UI-02] WarningOverlay: đổi màu nền sang đỏ khi isWarning = true
 *   - [UI-03] LogHistoryList: LazyColumn hiển thị lịch sử từ Room DB
 *   - [UI-04] Kết nối ViewModel qua collectAsState()
 *
 * Ngưỡng cảnh báo:
 *   - Nhiệt độ > 38°C  → Warning
 *   - CO2 > 1000 ppm   → Warning
 */
@Composable
fun DashboardScreen(
    viewModel: CabinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is CabinUiState.Loading -> {
            // TODO: LoadingContent()
        }
        is CabinUiState.Normal -> {
            val data = (uiState as CabinUiState.Normal).data
            // TODO: NormalDashboard(data)
        }
        is CabinUiState.Warning -> {
            val data = (uiState as CabinUiState.Warning).data
            // TODO: WarningDashboard(data)
        }
    }
}
