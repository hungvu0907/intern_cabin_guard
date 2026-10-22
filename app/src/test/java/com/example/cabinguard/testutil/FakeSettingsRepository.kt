package com.example.cabinguard.testutil

import com.example.cabinguard.data.repository.SettingsRepository
import com.example.cabinguard.domain.model.AlertThresholds
import com.example.cabinguard.domain.model.ThresholdValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake Settings để test engine / ViewModel mà không cần DataStore. */
class FakeSettingsRepository(
    initial: AlertThresholds = AlertThresholds()
) : SettingsRepository {

    private val _thresholds = MutableStateFlow(initial)
    override val thresholds: Flow<AlertThresholds> = _thresholds.asStateFlow()

    override suspend fun saveThresholds(
        tempThreshold: Float,
        co2Threshold: Float
    ): Result<Unit> {
        val error = ThresholdValidator.validate(tempThreshold, co2Threshold)
        if (error != null) {
            return Result.failure(IllegalArgumentException(error))
        }
        _thresholds.value = AlertThresholds(tempThreshold, co2Threshold)
        return Result.success(Unit)
    }

    fun emit(thresholds: AlertThresholds) {
        _thresholds.value = thresholds
    }
}
