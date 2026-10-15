package com.example.cabinguard

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThresholdSettingsStorageTest {
    @Test fun freshPreferencesUseAssignmentDefaults() = runBlocking {
        val fixture = ThresholdDataStoreFixture(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            assertEquals(CabinWarningThresholds(38.0, 1000), fixture.repository.thresholds.first())
        } finally { fixture.close() }
    }

    @Test fun updatingBothThresholdsSurvivesReopeningThePreferencesFile() = runBlocking {
        val fixture = ThresholdDataStoreFixture(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            val saved = CabinWarningThresholds(42.5, 1500)
            fixture.repository.update(saved)
            assertEquals(saved, fixture.repository.thresholds.first())
            fixture.reopen()
            assertEquals(saved, fixture.repository.thresholds.first())
            val replacement = CabinWarningThresholds(30.0, 500)
            fixture.repository.update(replacement)
            fixture.reopen()
            assertEquals(replacement, fixture.repository.thresholds.first())
        } finally { fixture.close() }
    }
}
