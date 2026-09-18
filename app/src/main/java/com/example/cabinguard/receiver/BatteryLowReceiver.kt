package com.example.cabinguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cabinguard.domain.sensor.CabinSensorEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BatteryLowReceiver : BroadcastReceiver() {

    @Inject
    lateinit var engine: CabinSensorEngine

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        when (intent?.action) {
            Intent.ACTION_BATTERY_LOW -> {
                engine.setBatteryLow(true)
                Log.d(TAG, "battery low → 5s scan")
            }
            Intent.ACTION_BATTERY_OKAY -> {
                engine.setBatteryLow(false)
                Log.d(TAG, "battery okay → 1s scan")
            }
        }
    }

    private companion object {
        const val TAG = "BatteryLowReceiver"
    }
}
