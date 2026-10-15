package com.example.cabinguard

import com.example.cabinguard.widget.WidgetUpdateLoop
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetUpdateLoopTest {
    @Test fun noLauncherUpdatesWithoutInstalledWidgetsAndNewWidgetsCanRefresh() = runTest {
        var installed = false
        var updates = 0
        val loop = WidgetUpdateLoop(backgroundScope, { testScheduler.currentTime }, { installed }, { updates++ }, { throw it })
        loop.requestUpdate(false, false)
        runCurrent()
        assertEquals("Recording without a home-screen widget must not run Glance updates", 0, updates)
        installed = true
        loop.requestUpdate(force = true)
        runCurrent()
        assertEquals(1, updates)
        advanceTimeBy(1000)
        loop.requestUpdate(false, false)
        runCurrent()
        assertEquals(1, updates)
        advanceTimeBy(29000)
        loop.requestUpdate(false, false)
        runCurrent()
        assertEquals(2, updates)
    }

    @Test fun forceSurvivesConflationAndFailuresDoNotStopLaterUpdates() = runTest {
        var attempts = 0
        val failures = mutableListOf<Exception>()
        val gate = CompletableDeferred<Unit>()
        val loop = WidgetUpdateLoop(backgroundScope, { testScheduler.currentTime }, { true }, {
            attempts++
            if (attempts == 1) gate.await()
            if (attempts == 2) throw IllegalStateException("launcher unavailable")
        }, failures::add)
        loop.requestUpdate(false, false)
        runCurrent()
        loop.requestUpdate(force = true)
        loop.requestUpdate(false, false)
        gate.complete(Unit)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals("launcher unavailable", failures.single().message)
        loop.requestUpdate(false, false)
        runCurrent()
        assertEquals(3, attempts)
    }
}
