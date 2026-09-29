package com.example.cabinguard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import com.example.cabinguard.domain.sensor.CabinSensorEngine
import com.example.cabinguard.work.CleanupOldLogsWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@HiltAndroidApp
class CabinGuardApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var thresholdSettings: ThresholdSettingsRepository

    @Inject
    lateinit var sensorEngine: CabinSensorEngine

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            thresholdSettings.thresholds
                .distinctUntilChanged()
                .collect(sensorEngine::setWarningThresholds)
        }
        CleanupOldLogsWorker.enqueue(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
