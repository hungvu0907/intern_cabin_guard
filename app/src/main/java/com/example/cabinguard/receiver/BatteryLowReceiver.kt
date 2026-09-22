package com.example.cabinguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.cabinguard.domain.sensor.CabinSensorEngine

/**
 * Từ Android 8.0 receiver khai báo tĩnh trong Manifest không nhận được
 * BATTERY_LOW/BATTERY_OKAY, nên receiver này được đăng ký động bởi
 * CabinTelemetryService và sống cùng vòng đời của service.
 */
class BatteryLowReceiver(
    private val engine: CabinSensorEngine
) : BroadcastReceiver() {

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

    /**
     * BATTERY_LOW không phải sticky broadcast: nếu service khởi động khi pin
     * đã yếu thì sẽ không có broadcast nào, nên đọc trạng thái hiện tại
     * từ sticky ACTION_BATTERY_CHANGED.
     */
    fun syncInitialState(context: Context) {
        val status = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return
        val batteryLow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            status.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
        } else {
            val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level >= 0 && scale > 0 && level * 100 / scale <= LOW_BATTERY_PERCENT
        }
        engine.setBatteryLow(batteryLow)
        Log.d(TAG, "initial battery low = $batteryLow")
    }

    companion object {
        private const val TAG = "BatteryLowReceiver"

        /** Ngưỡng hệ thống mặc định của Android cho BATTERY_LOW. */
        private const val LOW_BATTERY_PERCENT = 15

        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
    }
}
