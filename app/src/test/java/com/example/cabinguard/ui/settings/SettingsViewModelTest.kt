package com.example.cabinguard.ui.settings

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.domain.model.AlertThresholds
import com.example.cabinguard.testutil.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads default thresholds when store is empty`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
        val state = viewModel.uiState.first()
        assertEquals(CabinTelemetry.TEMP_WARNING_THRESHOLD, state.tempThreshold)
        assertEquals(CabinTelemetry.CO2_WARNING_THRESHOLD, state.co2Threshold)
        assertFalse(state.isLoading)
    }

    @Test
    fun `save rejects values outside range`() = runTest {
        val fake = FakeSettingsRepository()
        val viewModel = SettingsViewModel(fake)
        viewModel.onTempChanged(20f)
        viewModel.onCo2Changed(1000f)
        viewModel.save()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertFalse(state.isSaved)
        assertEquals(CabinTelemetry.TEMP_WARNING_THRESHOLD, fake.thresholds.first().tempThreshold)
    }

    @Test
    fun `save persists valid thresholds`() = runTest {
        val fake = FakeSettingsRepository()
        val viewModel = SettingsViewModel(fake)
        viewModel.onTempChanged(42f)
        viewModel.onCo2Changed(1500f)
        viewModel.save()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertNull(state.errorMessage)
        assertEquals(AlertThresholds(42f, 1500f), fake.thresholds.first())
    }
}
