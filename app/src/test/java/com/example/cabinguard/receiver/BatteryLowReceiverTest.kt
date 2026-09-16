package com.example.cabinguard.receiver

import com.example.cabinguard.domain.engine.CabinSensorEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryLowReceiverTest {

    @Test
    fun `BATTERY_LOW stretches interval to 5 seconds`() = runTest {
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
        BatteryLowReceiver(engine).handleAction(BatteryLowReceiver.ACTION_BATTERY_LOW)

        val collected = mutableListOf<Any>()
        backgroundScope.launch {
            engine.sensorFlow.collect { collected.add(it) }
        }
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, collected.size)
    }

    @Test
    fun `BATTERY_OKAY restores 1 second interval after low battery`() = runTest {
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
        val receiver = BatteryLowReceiver(engine)
        receiver.handleAction(BatteryLowReceiver.ACTION_BATTERY_LOW)
        receiver.handleAction(BatteryLowReceiver.ACTION_BATTERY_OKAY)

        val collected = mutableListOf<Any>()
        backgroundScope.launch {
            engine.sensorFlow.collect { collected.add(it) }
        }
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, collected.size)
    }

    @Test
    fun `unknown action keeps default 1 second interval`() = runTest {
        val engine = CabinSensorEngine(UnconfinedTestDispatcher(testScheduler))
        BatteryLowReceiver(engine).handleAction("android.intent.action.POWER_CONNECTED")

        val collected = mutableListOf<Any>()
        backgroundScope.launch {
            engine.sensorFlow.collect { collected.add(it) }
        }
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, collected.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, collected.size)
    }
}
