package com.example.cabinguard

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.widget.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSnapshotsTest {
    @Test fun widgetUsesRecordedWarningEvenWhenMeasurementsWouldSuggestAnotherStatus() {
        val recorded = CabinTelemetry(1, 123, 40.0, 1000.0, 1100, true)
        val snapshot = WidgetSnapshot(recorded)
        assertTrue("Widget must agree with history and CSV for the same recorded sample", snapshot.isWarning)
        assertFalse(WidgetSnapshot(recorded.copy(isWarning = false)).isWarning)
    }

    @Test fun activeWidgetThrottlesMeasurementsButShowsRecordedWarningChangesImmediately() = runTest {
        val source = MutableSharedFlow<WidgetSnapshot>()
        val output = mutableListOf<WidgetSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.throttleWidgetSnapshots { testScheduler.currentTime }.toList(output)
        }
        val sample = CabinTelemetry(1, 0, 37.0, 1000.0, 900, false)
        source.emit(WidgetSnapshot(null))
        source.emit(WidgetSnapshot(sample))
        advanceTimeBy(1000)
        source.emit(WidgetSnapshot(sample.copy(timestamp = 1000)))
        assertEquals(2, output.size)
        source.emit(WidgetSnapshot(sample.copy(temperature = 40.0, isWarning = true)))
        assertTrue(output.last().isWarning)
        source.emit(WidgetSnapshot(sample.copy(temperature = 40.0, isWarning = false)))
        assertFalse(output.last().isWarning)
        assertEquals(4, output.size)
        advanceTimeBy(30000)
        source.emit(output.last().copy(latest = sample.copy(timestamp = 31000)))
        assertEquals(5, output.size)
    }
}
