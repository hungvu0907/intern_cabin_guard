package com.example.cabinguard.ui.widget

import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CabinWidgetRefresherTest {

    @Test
    fun `no widget skips updateAll until a widget is installed`() = runTest {
        val readings = MutableSharedFlow<CabinTelemetry?>(extraBufferCapacity = 4)
        var installed = false
        var refreshCount = 0
        val refresher = CabinWidgetRefresher(
            latestReadings = readings,
            hasInstalledWidgets = { installed },
            updateWidgets = { refreshCount++ },
            scope = backgroundScope,
        )
        refresher.start()
        runCurrent()

        readings.emit(null)
        advanceTimeBy(WIDGET_REFRESH_INTERVAL_MS)
        runCurrent()
        assertEquals(0, refreshCount)

        installed = true
        readings.emit(null)
        advanceTimeBy(WIDGET_REFRESH_INTERVAL_MS)
        runCurrent()
        assertEquals(1, refreshCount)
    }

    @Test
    fun `room changes are sampled to at most one widget refresh every five seconds`() = runTest {
        val readings = MutableSharedFlow<Int>(extraBufferCapacity = 4)
        val sampledReadings = mutableListOf<Int>()
        val collectJob = backgroundScope.launch {
            readings.sampleWidgetUpdates().collect(sampledReadings::add)
        }
        runCurrent()

        readings.emit(1)
        readings.emit(2)
        advanceTimeBy(WIDGET_REFRESH_INTERVAL_MS - 1)
        assertEquals(emptyList<Int>(), sampledReadings)

        readings.emit(3)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(3), sampledReadings)

        readings.emit(4)
        advanceTimeBy(WIDGET_REFRESH_INTERVAL_MS)
        runCurrent()
        assertEquals(listOf(3, 4), sampledReadings)

        collectJob.cancel()
        advanceUntilIdle()
    }
}
