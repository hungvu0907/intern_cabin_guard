package com.example.cabinguard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.ui.dashboard.DashboardUiState
import com.example.cabinguard.ui.dashboard.DashboardStateReducer
import com.example.cabinguard.ui.dashboard.dashboardStates
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class DashboardStateTest {
    @Test fun missingOrUnreadableDataIsNeverShownAsSafe() {
        assertEquals("Đang chờ mẫu cảm biến", DashboardUiState().statusText)
        assertEquals("Đang đọc dữ liệu...", DashboardUiState(loading = true).statusText)
        assertEquals("Lỗi đọc dữ liệu", DashboardUiState(readError = "Lỗi đọc dữ liệu").statusText)
    }
    @Test fun pausedStateExplainsThatRecordingContinues() {
        val row = CabinTelemetry(timestamp = 1, temperature = 37.0, pressure = 1000.0, co2Level = 900, isWarning = false)
        assertEquals("Cabin an toàn", DashboardUiState(latest = row).statusText)
        assertTrue(DashboardUiState(latest = row, isPaused = true).statusText.contains("Service vẫn ghi log"))
    }

    @Test fun pausedDisplayRetainsDataAndRecoversFromAnInitiallyPausedSubscription() {
        val row = CabinTelemetry(timestamp = 1, temperature = 37.0, pressure = 1000.0, co2Level = 900, isWarning = false)
        val reducer = DashboardStateReducer()
        assertEquals(row, reducer.reduce(DashboardUiState(latest = row, isPaused = true)).latest)
        assertEquals(row, reducer.reduce(DashboardUiState(latest = row.copy(timestamp = 2), isPaused = true, unsyncedCount = 2)).latest)
        assertEquals(2, reducer.reduce(DashboardUiState(latest = row.copy(timestamp = 3), isPaused = true, unsyncedCount = 2)).unsyncedCount)
        assertEquals(4L, reducer.reduce(DashboardUiState(latest = row.copy(timestamp = 4))).latest!!.timestamp)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun readFailureIsVisibleAndRetriesWithoutCrashingTheCollector() = runTest {
        var reads = 0
        val row = CabinTelemetry(timestamp = 1, temperature = 37.0, pressure = 1000.0, co2Level = 900, isWarning = false)
        val source = flow {
            reads++
            if (reads == 1) throw java.io.IOException("database busy")
            emit(row)
            awaitCancellation()
        }
        val states = mutableListOf<DashboardUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dashboardStates(source, flowOf(emptyList()), flowOf(false), flowOf(CabinWarningThresholds()), flowOf(0)).toList(states)
        }
        runCurrent()
        assertNotNull(states.last().readError)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(row, states.last().latest)
        assertNull(states.last().readError)
    }
}
