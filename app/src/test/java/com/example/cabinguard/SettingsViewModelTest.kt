package com.example.cabinguard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.SavedStateHandle
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import com.example.cabinguard.ui.settings.SettingsViewModel
import com.example.cabinguard.widget.WidgetRefresh
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private class PreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow<Preferences>(emptyPreferences())
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            gate?.await()
            if (fail) throw java.io.IOException("write failed")
            data.value = transform(data.value)
            return data.value
        }
    }
    private class Widgets : WidgetRefresh {
        var updates = 0
        var fail = false
        override fun requestUpdate(warning: Boolean?, force: Boolean) {
            if (fail) throw IllegalStateException("launcher unavailable")
            if (force) updates++
        }
    }
    @Before fun mainDispatcher() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }

    @Test fun closesOnlyAfterSuccessfulSaveAndReportsFailures() = runTest {
        val store = PreferencesStore()
        val widgets = Widgets()
        val repository = ThresholdSettingsRepository(store)
        val vm = SettingsViewModel(repository, SavedStateHandle(), widgets)
        assertEquals("38.0", vm.uiState.value.temperature)
        vm.temperatureChanged("50")
        vm.co2Changed("1500")
        store.gate = CompletableDeferred()
        vm.save()
        assertTrue(vm.uiState.value.saving)
        assertFalse(vm.uiState.value.saved)
        store.gate!!.complete(Unit)
        runCurrent()
        assertTrue(vm.uiState.value.saved)
        assertEquals(CabinWarningThresholds(50.0, 1500), repository.thresholds.first())
        assertEquals(1, widgets.updates)
        vm.consumeSaved()
        store.fail = true
        vm.save(); runCurrent()
        assertFalse(vm.uiState.value.saved)
        assertFalse(vm.uiState.value.saving)
        assertNotNull(vm.uiState.value.error)
        assertEquals(1, widgets.updates)
    }

    @Test fun validatesWithoutWritingAndRestoresDrafts() = runTest {
        val store = PreferencesStore()
        val widgets = Widgets()
        val handle = SavedStateHandle()
        val vm = SettingsViewModel(ThresholdSettingsRepository(store), handle, widgets)
        vm.temperatureChanged("29"); vm.save()
        assertNotNull(vm.uiState.value.temperatureError)
        assertTrue(store.data.value.asMap().isEmpty())
        vm.temperatureChanged("51,5"); vm.co2Changed("2000")
        val restored = SettingsViewModel(ThresholdSettingsRepository(store), SavedStateHandle(mapOf("temperature" to handle.get<String>("temperature"), "co2" to handle.get<String>("co2"))), widgets)
        assertEquals("51,5", restored.uiState.value.temperature)
        assertEquals("2000", restored.uiState.value.co2)
    }

    @Test fun widgetFailureDoesNotUndoSuccessfulSettingsSave() = runTest {
        val repository = ThresholdSettingsRepository(PreferencesStore())
        val vm = SettingsViewModel(repository, SavedStateHandle(), Widgets().apply { fail = true })
        vm.temperatureChanged("50"); vm.co2Changed("1500"); vm.save()
        runCurrent()
        assertEquals(CabinWarningThresholds(50.0, 1500), repository.thresholds.first())
        assertTrue("Saved settings must not be reported as failed when the launcher fails", vm.uiState.value.saved)
        assertNull(vm.uiState.value.error)
    }

    @Test fun leavingSettingsDiscardsUnsavedDraftAndReloadsStoredLimits() = runTest {
        val repository = ThresholdSettingsRepository(PreferencesStore())
        val vm = SettingsViewModel(repository, SavedStateHandle(), Widgets())
        vm.temperatureChanged("51")
        assertTrue(vm.uiState.value.dirty)
        vm.discardDraft(); runCurrent()
        assertEquals("38.0", vm.uiState.value.temperature)
        assertFalse(vm.uiState.value.dirty)
        repository.update(CabinWarningThresholds(45.0, 2000))
        vm.discardDraft(); runCurrent()
        assertEquals("45.0", vm.uiState.value.temperature)
        assertEquals("2000", vm.uiState.value.co2)
    }
}
