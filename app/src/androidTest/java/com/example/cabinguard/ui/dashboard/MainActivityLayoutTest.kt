package com.example.cabinguard.ui.dashboard

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cabinguard.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Run on both a phone and an Automotive AVD to exercise both real device features. */
    @Test
    fun landscapeDashboardMatchesDeviceFeature() {
        composeRule.runOnUiThread {
            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        val isAutomotive = composeRule.activity.packageManager.hasSystemFeature(
            PackageManager.FEATURE_AUTOMOTIVE,
        )
        val expected = if (isAutomotive) "automotive_dashboard" else "phone_dashboard"
        val unexpected = if (isAutomotive) "phone_dashboard" else "automotive_dashboard"
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(expected).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(expected).assertIsDisplayed()
        composeRule.onNodeWithTag(unexpected).assertDoesNotExist()
    }
}
