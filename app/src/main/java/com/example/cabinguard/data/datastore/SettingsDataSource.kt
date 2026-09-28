package com.example.cabinguard.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Lưu ngưỡng cảnh báo dạng Flow; mặc định 38°C / 1000 ppm khi chưa từng ghi. */
@Singleton
class SettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val tempThreshold: Flow<Float> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[Keys.TEMP_THRESHOLD] ?: CabinTelemetry.TEMP_WARNING_THRESHOLD
        }

    val co2Threshold: Flow<Float> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            prefs[Keys.CO2_THRESHOLD] ?: CabinTelemetry.CO2_WARNING_THRESHOLD
        }

    suspend fun save(tempThreshold: Float, co2Threshold: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.TEMP_THRESHOLD] = tempThreshold
            prefs[Keys.CO2_THRESHOLD] = co2Threshold
        }
    }

    private object Keys {
        val TEMP_THRESHOLD = floatPreferencesKey("temp_threshold")
        val CO2_THRESHOLD = floatPreferencesKey("co2_threshold")
    }
}
