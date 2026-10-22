package com.example.cabinguard.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.cabinguard.data.repository.SettingsRepository
import com.example.cabinguard.domain.model.AlertThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsScreenShowsStoredValuesAndSavesThem() {
        val repository = RecordingSettingsRepository(AlertThresholds(42f, 1_500f))
        val viewModel = SettingsViewModel(repository)

        composeRule.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel)
        }

        composeRule.onNodeWithText("Cài đặt").assertIsDisplayed()
        composeRule.onNodeWithText("Nhiệt độ  ·  42 °C").assertIsDisplayed()
        composeRule.onNodeWithText("CO2  ·  1500 ppm").assertIsDisplayed()
        composeRule.onNodeWithText("Lưu ngưỡng").performClick()
        composeRule.waitUntil { repository.saved == AlertThresholds(42f, 1_500f) }
        assertEquals(AlertThresholds(42f, 1_500f), repository.saved)
    }
}

private class RecordingSettingsRepository(initial: AlertThresholds) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val thresholds: Flow<AlertThresholds> = state
    var saved: AlertThresholds? = null

    override suspend fun saveThresholds(
        tempThreshold: Float,
        co2Threshold: Float,
    ): Result<Unit> = Result.success(Unit).also {
        saved = AlertThresholds(tempThreshold, co2Threshold)
        state.value = saved!!
    }
}
