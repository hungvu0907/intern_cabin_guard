package com.example.cabinguard.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `empty DataStore exposes default thresholds`() = runTest {
        val source = createDataSource()

        assertEquals(CabinTelemetry.TEMP_WARNING_THRESHOLD, source.tempThreshold.first())
        assertEquals(CabinTelemetry.CO2_WARNING_THRESHOLD, source.co2Threshold.first())
    }

    @Test
    fun `save persists both thresholds in a real DataStore`() = runTest {
        val source = createDataSource()

        source.save(tempThreshold = 42f, co2Threshold = 1_500f)

        assertEquals(42f, source.tempThreshold.first())
        assertEquals(1_500f, source.co2Threshold.first())
    }

    private fun TestScope.createDataSource(): SettingsDataSource {
        val file = File(temporaryFolder.newFolder(), "settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { file },
        )
        return SettingsDataSource(dataStore)
    }
}
