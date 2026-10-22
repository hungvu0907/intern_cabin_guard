package com.example.cabinguard.domain.engine

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.SettingsRepository
import com.example.cabinguard.domain.model.AlertThresholds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CabinSensorEngineRoomTest {

    @Test
    fun changingThresholdDoesNotInsertAnExtraRoomRow() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CabinDatabase::class.java,
        ).build()
        val dao = database.cabinTelemetryDao()
        val settings = MutableSettingsRepository(AlertThresholds(50f, 2_000f))
        val scheduler = TestCoroutineScheduler()
        val dispatcher = UnconfinedTestDispatcher(scheduler)
        val engine = CabinSensorEngine(settings, dispatcher)
        val savedReadings = Channel<CabinTelemetry>(Channel.UNLIMITED)
        val collector = CoroutineScope(SupervisorJob() + dispatcher).launch {
            engine.sensorFlow.collect { reading ->
                dao.insert(reading)
                savedReadings.send(reading)
            }
        }

        try {
            scheduler.runCurrent()
            assertFalse(savedReadings.receive().isWarning)
            assertEquals(1, dao.getTotalCount().first())

            settings.saveThresholds(20f, 100f).getOrThrow()
            scheduler.runCurrent()
            assertEquals(1, dao.getTotalCount().first())
            assertTrue(savedReadings.tryReceive().isFailure)

            scheduler.advanceTimeBy(999)
            scheduler.runCurrent()
            assertEquals(1, dao.getTotalCount().first())

            scheduler.advanceTimeBy(1)
            scheduler.runCurrent()
            assertTrue(savedReadings.receive().isWarning)
            val rows = dao.getAllLogs().first()
            assertEquals(2, rows.size)
            assertEquals(1, rows.count { it.isWarning })
        } finally {
            collector.cancelAndJoin()
            database.close()
        }
    }

    private class MutableSettingsRepository(initial: AlertThresholds) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val thresholds = state

        override suspend fun saveThresholds(
            tempThreshold: Float,
            co2Threshold: Float,
        ): Result<Unit> {
            state.value = AlertThresholds(tempThreshold, co2Threshold)
            return Result.success(Unit)
        }
    }
}
