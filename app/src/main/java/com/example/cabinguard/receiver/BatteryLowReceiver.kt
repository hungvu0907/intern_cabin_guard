package com.example.cabinguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cabinguard.domain.engine.CabinSensorEngine

class BatteryLowReceiver(
    private val sensorEngine: CabinSensorEngine
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        handleAction(intent?.action)
    }

    fun handleAction(action: String?) {
        when (action) {
            ACTION_BATTERY_LOW -> {
                sensorEngine.setInterval(LOW_BATTERY_INTERVAL_MS)
                Log.d(TAG, "Pin yếu → chu kỳ đọc = ${LOW_BATTERY_INTERVAL_MS}ms")
            }
            ACTION_BATTERY_OKAY -> {
                sensorEngine.setInterval(NORMAL_INTERVAL_MS)
                Log.d(TAG, "Pin ổn định → chu kỳ đọc = ${NORMAL_INTERVAL_MS}ms")
            }
        }
    }

    companion object {
        private const val TAG = "BatteryReceiver"
        const val ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW"
        const val ACTION_BATTERY_OKAY = "android.intent.action.BATTERY_OKAY"
        const val NORMAL_INTERVAL_MS = 1_000L
        const val LOW_BATTERY_INTERVAL_MS = 5_000L
    }
}
