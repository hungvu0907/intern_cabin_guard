package com.example.cabinguard.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class ThresholdSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val thresholds: Flow<CabinWarningThresholds> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(::readThresholds)

    suspend fun update(thresholds: CabinWarningThresholds) {
        dataStore.edit { preferences ->
            preferences[TEMPERATURE_KEY] = thresholds.temperatureCelsius
            preferences[CO2_KEY] = thresholds.co2Ppm
        }
    }

    private fun readThresholds(preferences: Preferences): CabinWarningThresholds {
        val defaults = CabinWarningThresholds()
        return runCatching {
            CabinWarningThresholds(
                temperatureCelsius = preferences[TEMPERATURE_KEY]
                    ?: defaults.temperatureCelsius,
                co2Ppm = preferences[CO2_KEY] ?: defaults.co2Ppm
            )
        }.getOrDefault(defaults)
    }

    private companion object {
        val TEMPERATURE_KEY = doublePreferencesKey("temperature_warning_celsius")
        val CO2_KEY = intPreferencesKey("co2_warning_ppm")
    }
}
