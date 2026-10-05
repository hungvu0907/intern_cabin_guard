package com.example.cabinguard.ui.dashboard

import com.example.cabinguard.MainDispatcherRule
import com.example.cabinguard.data.export.TelemetryCsvExporter
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
import kotlinx.coroutines.flow.first
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
    private lateinit var csvExporter: TelemetryCsvExporter
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
        csvExporter = mockk()
        every { repository.getRecentLogs() } returns history
        coEvery { repository.saveTelemetry(any()) } just runs
    }

    @After
    fun tearDown() {
        CabinTelemetryService.isRunning = false
    }

    @Test
    fun `uiState starts as Loading before any emission`() {
        val viewModel = viewModel()
        assertEquals(CabinUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState is Normal and saves when service is not running`() = runTest {
        val viewModel = viewModel()
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
        val viewModel = viewModel()
        val data = telemetry(temperature = 30f, co2Level = 800f)

        sensorEvents.emit(data)

        assertTrue(viewModel.uiState.value is CabinUiState.Normal)
        coVerify(exactly = 0) { repository.saveTelemetry(any()) }
    }

    @Test
    fun `uiState is Warning when temperature exceeds 38C`() = runTest {
        val viewModel = viewModel()
        val data = telemetry(temperature = 39f, co2Level = 500f)

        sensorEvents.emit(data)

        val state = viewModel.uiState.value
        assertTrue(state is CabinUiState.Warning)
        assertEquals(data, (state as CabinUiState.Warning).data)
    }

    @Test
    fun `uiState is Warning when CO2 exceeds 1000 ppm`() = runTest {
        val viewModel = viewModel()
        val data = telemetry(temperature = 28f, co2Level = 1100f)

        sensorEvents.emit(data)

        assertTrue(viewModel.uiState.value is CabinUiState.Warning)
    }

    @Test
    fun `uiState switches from Normal to Warning on next reading`() = runTest {
        val viewModel = viewModel()

        sensorEvents.emit(telemetry(temperature = 32f, co2Level = 600f))
        assertTrue(viewModel.uiState.value is CabinUiState.Normal)

        sensorEvents.emit(telemetry(temperature = 40f, co2Level = 600f))
        assertTrue(viewModel.uiState.value is CabinUiState.Warning)
    }

    @Test
    fun `export emits Empty when history has no logs`() = runTest {
        coEvery { csvExporter.export() } returns null
        val viewModel = viewModel()

        viewModel.onExportHistory()

        assertEquals(ExportHistoryEvent.Empty, viewModel.exportEvents.first())
    }

    @Test
    fun `export emits ready file from streaming exporter`() = runTest {
        val file = java.io.File("build/tmp/cabinguard_history.csv")
        coEvery { csvExporter.export() } returns file
        val viewModel = viewModel()

        viewModel.onExportHistory()

        val event = viewModel.exportEvents.first()
        assertTrue(event is ExportHistoryEvent.Ready)
        assertEquals(file, (event as ExportHistoryEvent.Ready).file)
    }

    private fun viewModel() = CabinViewModel(
        engine,
        repository,
        FakeSettingsRepository(),
        csvExporter,
    )

}
