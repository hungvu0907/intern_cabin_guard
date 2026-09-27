package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.MainDispatcherRule
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import com.example.cabinguard.domain.engine.CabinSensorEngine
import com.example.cabinguard.domain.model.CabinUiState
import com.example.cabinguard.service.CabinTelemetryService
import com.example.cabinguard.testutil.FakeSettingsRepository
import com.example.cabinguard.telemetry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CabinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var engine: CabinSensorEngine
    private lateinit var repository: CabinTelemetryRepository
    private lateinit var sensorEvents: MutableSharedFlow<CabinTelemetry>
    private lateinit var history: MutableStateFlow<List<CabinTelemetry>>

    @Before
    fun setup() {
        CabinTelemetryService.isRunning = false
        sensorEvents = MutableSharedFlow(extraBufferCapacity = 8)
        history = MutableStateFlow(emptyList())
        engine = mockk(relaxed = true)
        every { engine.sensorFlow } returns sensorEvents
        repository = mockk(relaxed = true)
        every { repository.getAllLogs() } returns history
        coEvery { repository.saveTelemetry(any()) } just runs
    }

    @After
    fun tearDown() {
        CabinTelemetryService.isRunning = false
    }

    @Test
    fun `uiState starts as Loading before any emission`() {
        val viewModel = CabinViewModel(engine, repository, FakeSettingsRepository())
        assertEquals(CabinUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState is Normal and saves when service is not running`() = runTest {
        val viewModel = CabinViewModel(engine, repository, FakeSettingsRepository())
        val data = telemetry(temperature = 30f, co2Level = 800f)

        sensorEvents.emit(data)

        val state = viewModel.uiState.value
        assertTrue(state is CabinUiState.Normal)
        assertEquals(data, (state as CabinUiState.Normal).data)
        coVerify(exactly = 1) { repository.saveTelemetry(data) }
    }

    @Test
    fun `does not save telemetry when service is already running`() = runTest {
        CabinTelemetryService.isRunning = true
        val viewModel = CabinViewModel(engine, repository, FakeSettingsRepository())
        val data = telemetry(temperature = 30f, co2Level = 800f)

        sensorEvents.emit(data)

        assertTrue(viewModel.uiState.value is CabinUiState.Normal)
        coVerify(exactly = 0) { repository.saveTelemetry(any()) }
    }

    @Test
    fun `uiState is Warning when temperature exceeds 38C`() = runTest {
        val viewModel = CabinViewModel(engine, repository, FakeSettingsRepository())
        val data = telemetry(temperature = 39f, co2Level = 500f)

        sensorEvents.emit(data)

        val state = viewModel.uiState.value
        assertTrue(state is CabinUiState.Warning)
        assertEquals(data, (state as CabinUiState.Warning).data)
    }

    @Test
    fun `uiState is Warning when CO2 exceeds 1000 ppm`() = runTest {
        val viewModel = CabinViewModel(engine, repository, FakeSettingsRepository())
        val data = telemetry(temperature = 28f, co2Level = 1100f)

        sensorEvents.emit(data)

        assertTrue(viewModel.uiState.value is CabinUiState.Warning)
    }

    @Test
    fun `uiState switches from Normal to Warning on next reading`() = runTest {
        val viewModel = CabinViewModel(engine, repository, FakeSettingsRepository())

        sensorEvents.emit(telemetry(temperature = 32f, co2Level = 600f))
        assertTrue(viewModel.uiState.value is CabinUiState.Normal)

        sensorEvents.emit(telemetry(temperature = 40f, co2Level = 600f))
        assertTrue(viewModel.uiState.value is CabinUiState.Warning)
    }

}
