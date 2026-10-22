package com.example.cabinguard.data.repository

import com.example.cabinguard.data.datastore.SettingsDataSource
import com.example.cabinguard.domain.model.AlertThresholds
import com.example.cabinguard.domain.model.ThresholdValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** API đọc/ghi Settings duy nhất — UI/domain không gọi DataStore trực tiếp. */
interface SettingsRepository {
    val thresholds: Flow<AlertThresholds>
    suspend fun saveThresholds(tempThreshold: Float, co2Threshold: Float): Result<Unit>
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsDataSource
) : SettingsRepository {

    override val thresholds: Flow<AlertThresholds> = combine(
        dataSource.tempThreshold,
        dataSource.co2Threshold
    ) { temp, co2 ->
        AlertThresholds(tempThreshold = temp, co2Threshold = co2)
    }

    override suspend fun saveThresholds(
        tempThreshold: Float,
        co2Threshold: Float
    ): Result<Unit> {
        val error = ThresholdValidator.validate(tempThreshold, co2Threshold)
        if (error != null) {
            return Result.failure(IllegalArgumentException(error))
        }
        dataSource.save(tempThreshold, co2Threshold)
        return Result.success(Unit)
    }
}
