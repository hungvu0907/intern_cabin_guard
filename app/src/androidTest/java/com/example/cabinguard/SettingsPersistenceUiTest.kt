package com.example.cabinguard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import com.example.cabinguard.ui.settings.SettingsScreen
import com.example.cabinguard.ui.settings.SettingsViewModel
import com.example.cabinguard.ui.theme.CabinGuardTheme
import com.example.cabinguard.widget.WidgetRefresh
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsPersistenceUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var fixture: ThresholdDataStoreFixture
    private val stores = mutableListOf<ViewModelStore>()
    private var forcedWidgetUpdates = 0

    @Before fun setUp() {
        fixture = ThresholdDataStoreFixture(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { stores.forEach { it.clear() } }
        fixture.close()
    }

    private fun newViewModel(): SettingsViewModel {
        val store = ViewModelStore().also { stores += it }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(
                fixture.repository, SavedStateHandle(), object : WidgetRefresh {
                    override fun requestUpdate(warning: Boolean?, force: Boolean) {
                        if (force) forcedWidgetUpdates++
                    }
                }
            ) as T
        }
        return ViewModelProvider(store, factory)[SettingsViewModel::class.java]
    }

    @Test fun editedThresholdsAreSavedAndDisplayedAfterReopeningSettingsAndDataStore() {
        lateinit var original: SettingsViewModel
        compose.runOnIdle { original = newViewModel() }
        var current by mutableStateOf(original)
        var open by mutableStateOf(true)
        compose.setContent {
            CabinGuardTheme {
                if (open) SettingsScreen(onBack = { open = false }, viewModel = current)
            }
        }
        compose.waitUntil(10_000) { !original.uiState.value.loading }
        compose.onNode(hasSetTextAction() and hasText("Nhiệt độ (°C)"))
            .performScrollTo().performTextReplacement("42,5")
        compose.onNode(hasSetTextAction() and hasText("CO₂ (ppm)"))
            .performScrollTo().performTextReplacement("1500")
        compose.onNodeWithText("Lưu", substring = false).performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { !open }
        assertEquals(CabinWarningThresholds(42.5, 1500), runBlocking { fixture.repository.thresholds.first() })
        compose.runOnIdle {
            assertEquals(1, forcedWidgetUpdates)
            stores.forEach { it.clear() }
        }

        // Recreate both the store and ViewModel; the displayed values must come from disk.
        fixture.reopen()
        lateinit var reopened: SettingsViewModel
        compose.runOnIdle {
            reopened = newViewModel()
            current = reopened
            open = true
        }
        compose.waitUntil(10_000) { !reopened.uiState.value.loading }
        compose.onNode(hasSetTextAction() and hasText("Nhiệt độ (°C)"))
            .assertTextContains("42.5")
        compose.onNode(hasSetTextAction() and hasText("CO₂ (ppm)"))
            .assertTextContains("1500")
        compose.onNodeWithText("Lưu", substring = false).performScrollTo().assertIsNotEnabled()
    }

    @Test fun externalPreferencesUpdatePreservesDraftAndDiscardLoadsTheLatestSavedValues() {
        lateinit var original: SettingsViewModel
        compose.runOnIdle { original = newViewModel() }
        var current by mutableStateOf(original)
        var open by mutableStateOf(true)
        compose.setContent {
            CabinGuardTheme {
                if (open) SettingsScreen(onBack = { open = false }, viewModel = current)
            }
        }
        compose.waitUntil(10_000) { !original.uiState.value.loading }
        compose.onNode(hasSetTextAction() and hasText("Nhiệt độ (°C)"))
            .performScrollTo().performTextReplacement("4")
        compose.onNode(hasSetTextAction() and hasText("CO₂ (ppm)"))
            .performScrollTo().performTextReplacement("15")

        val externallySaved = CabinWarningThresholds(50.0, 2000)
        runBlocking { fixture.repository.update(externallySaved) }
        compose.onNode(hasSetTextAction() and hasText("Nhiệt độ (°C)"))
            .assertTextContains("4")
        compose.onNode(hasSetTextAction() and hasText("CO₂ (ppm)"))
            .assertTextContains("15")
        compose.runOnIdle {
            assertEquals("4", original.uiState.value.temperature)
            assertEquals("15", original.uiState.value.co2)
            assertTrue(original.uiState.value.dirty)
        }

        compose.onNodeWithText("Quay lại").performScrollTo().performClick()
        compose.waitUntil(10_000) { !open }
        lateinit var reopened: SettingsViewModel
        compose.runOnIdle {
            stores.forEach { it.clear() }
            reopened = newViewModel()
            current = reopened
            open = true
        }
        compose.waitUntil(10_000) { !reopened.uiState.value.loading }
        compose.onNode(hasSetTextAction() and hasText("Nhiệt độ (°C)"))
            .assertTextContains("50.0")
        compose.onNode(hasSetTextAction() and hasText("CO₂ (ppm)"))
            .assertTextContains("2000")
        compose.onNodeWithText("Lưu", substring = false).performScrollTo().assertIsNotEnabled()
        assertEquals(externallySaved, runBlocking { fixture.repository.thresholds.first() })
    }
}
