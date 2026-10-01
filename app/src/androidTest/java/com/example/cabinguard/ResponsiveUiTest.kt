package com.example.cabinguard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.ui.dashboard.*
import com.example.cabinguard.ui.settings.*
import com.example.cabinguard.ui.theme.CabinGuardTheme
import org.junit.*
import org.junit.Assert.*

class ResponsiveUiTest {
    @get:Rule val compose = createComposeRule()

    private fun checkDashboard(width: Int, height: Int) {
        var exports = 0
        var settings = 0
        compose.setContent {
            CabinGuardTheme {
                Box(Modifier.requiredSize(width.dp, height.dp)) {
                    DashboardContent(
                        DashboardUiState(latest = CabinTelemetry(1, 123, 40.0, 1000.0, 1100, true)),
                        onExportCsv = { exports++ }, onEditThresholds = { settings++ }
                    )
                }
            }
        }
        compose.onNodeWithText("Chỉnh ngưỡng").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Xuất CSV").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, settings); assertEquals(1, exports) }
    }

    @Test fun phonePortraitKeepsSettingsAndExportAccessible() = checkDashboard(360, 640)
    @Test fun phoneLandscapeKeepsSettingsAndExportAccessible() = checkDashboard(640, 360)

    @Test fun settingsShowsSaveErrorAndDisablesActionsDuringWrite() {
        compose.setContent {
            CabinGuardTheme {
                SettingsContent(SettingsUiState("50", "1500", loading = false, saving = true, error = "Lưu thất bại"), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("50").assertExists()
        compose.onNodeWithText("1500").assertExists()
        compose.onNodeWithText("Đang lưu...").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Lưu thất bại").performScrollTo().assertIsDisplayed()
    }
}
