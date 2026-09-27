package com.example.cabinguard

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cabinguard.domain.engine.CabinSensorEngine
import com.example.cabinguard.receiver.BatteryLowReceiver
import com.example.cabinguard.worker.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CabinGuardApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // Engine dùng chung (@Singleton) — receiver sẽ đổi chu kỳ đọc của nó khi pin thay đổi.
    @Inject lateinit var sensorEngine: CabinSensorEngine

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleCleanupWork()
        registerBatteryReceiver()
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        ContextCompat.registerReceiver(
            this,
            BatteryLowReceiver(sensorEngine),
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Lên lịch dọn log định kỳ mỗi 24h, chỉ chạy khi pin không yếu.
    private fun scheduleCleanupWork() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // không dọn khi pin yếu
            .build()

        val request = PeriodicWorkRequestBuilder<CleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // KEEP: nếu đã có lịch thì giữ nguyên, không tạo trùng mỗi lần mở app.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CleanupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
