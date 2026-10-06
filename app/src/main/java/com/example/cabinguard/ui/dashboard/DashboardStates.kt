package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

const val RECENT_HISTORY_LIMIT = 200

/** State survives upstream unsubscribe/retry while the ViewModel is alive. */
class DashboardStateReducer {
    private var displayed: DashboardUiState? = null
    fun reduce(current: DashboardUiState): DashboardUiState {
        val previous = displayed
        val result = if (current.isPaused && previous?.latest != null) {
            previous.copy(isPaused = true, warningThresholds = current.warningThresholds,
                unsyncedCount = current.unsyncedCount, loading = false, readError = null)
        } else current
        displayed = result
        return result
    }
}

/** Collected by one shared StateFlow in DashboardViewModel. */
fun dashboardStates(
    latest: Flow<CabinTelemetry?>,
    history: Flow<List<CabinTelemetry>>,
    paused: Flow<Boolean>,
    thresholds: Flow<CabinWarningThresholds>,
    unsynced: Flow<Int>
): Flow<DashboardUiState> {
    val reducer = DashboardStateReducer()
    return combine(latest, history, paused, thresholds, unsynced) { row, rows, isPaused, limits, count ->
        DashboardUiState(row, rows, isPaused, limits, count)
    }.map(reducer::reduce).retryWhen { error, _ ->
        if (error is CancellationException || error !is Exception) throw error
        emit(DashboardUiState(readError = "Không đọc được dữ liệu. Đang thử lại..."))
        delay(5_000)
        true
    }
}
