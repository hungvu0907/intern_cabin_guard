package com.example.cabinguard.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.domain.sensor.CabinSensorEngine
import com.example.cabinguard.receiver.BatteryLowReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CabinTelemetryService : Service() {

    @Inject
    lateinit var dao: CabinTelemetryDao

    @Inject
    lateinit var engine: CabinSensorEngine

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
    private var collectJob: Job? = null

    // lazy vì engine chỉ được Hilt inject trong super.onCreate().
    private val batteryReceiver by lazy { BatteryLowReceiver(engine) }

    override fun onCreate() {
        super.onCreate()
        CabinNotification.createChannel(this)
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            BatteryLowReceiver.intentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        batteryReceiver.syncInitialState(this)
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startForeground(
            CabinNotification.NOTIF_ID,
            CabinNotification.build(
                context = this,
                recordCount = 0,
                isWarning = false
            )
        )
        startCollecting()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        unregisterReceiver(batteryReceiver)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startCollecting() {
        if (collectJob?.isActive == true) return
        collectJob = scope.launch {
            Log.d(TAG, "collecting telemetry")
            engine.observeTelemetry().collect { telemetry ->
                dao.insert(telemetry)
                val count = dao.observeCount().first()
                CabinNotification.notify(
                    context = this@CabinTelemetryService,
                    recordCount = count,
                    isWarning = telemetry.isWarning
                )
            }
        }
    }

    private companion object {
        const val TAG = "CabinTelemetryService"
    }
}
