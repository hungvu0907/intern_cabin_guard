package com.example.cabinguard.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cabinguard.data.export.CsvExporter
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    telemetryDao: CabinTelemetryDao,
    private val csvExporter: CsvExporter,
    private val thresholdSettings: ThresholdSettingsRepository
) : ViewModel() {

    private val isPaused = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        telemetryDao.observeLatest(),
        telemetryDao.observeAll(),
        isPaused,
        thresholdSettings.thresholds,
        telemetryDao.observeUnsyncedCount()
    ) { latest, history, paused, thresholds, unsynced ->
        DashboardUiState(
            latest = latest,
            history = history,
            isPaused = paused,
            warningThresholds = thresholds,
            unsyncedCount = unsynced
        )
    }.scan(DashboardUiState()) { previous, current ->
        // Đang pause thì giữ nguyên số liệu đang hiển thị, Service vẫn ghi Room.
        if (current.isPaused) {
            previous.copy(
                isPaused = true,
                warningThresholds = current.warningThresholds,
                unsyncedCount = current.unsyncedCount
            )
        } else {
            current
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    // Pause chỉ đổi UI; Service vẫn ghi Room (PR A không stopService).
    fun pauseMonitoring() {
        isPaused.value = true
    }

    fun resumeMonitoring() {
        isPaused.value = false
    }

    private val exporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = exporting.asStateFlow()

    // Channel để mỗi kết quả chỉ mở share sheet một lần, kể cả khi xoay màn hình.
    private val exportChannel = Channel<ExportResult>(Channel.BUFFERED)
    val exportResults: Flow<ExportResult> = exportChannel.receiveAsFlow()

    fun exportCsv() {
        if (exporting.value) return
        exporting.value = true
        viewModelScope.launch {
            val result = try {
                csvExporter.export()
                    ?.let { ExportResult.Ready(it) }
                    ?: ExportResult.Empty
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ExportResult.Failed
            } finally {
                exporting.value = false
            }
            exportChannel.send(result)
        }
    }

}

sealed interface ExportResult {
    data class Ready(val uri: Uri) : ExportResult
    data object Empty : ExportResult
    data object Failed : ExportResult
}
