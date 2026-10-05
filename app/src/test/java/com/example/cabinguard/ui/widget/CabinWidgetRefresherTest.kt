package com.example.cabinguard.ui.widget

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
