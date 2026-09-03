package com.example.cabinguard.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.cabinguard.MainActivity
import com.example.cabinguard.R
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import com.example.cabinguard.domain.engine.CabinSensorEngine
import android.app.PendingIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CabinTelemetryService : Service() {

    // Hilt inject 2 dependency dùng chung (@Singleton) với phần còn lại của app.
    @Inject lateinit var sensorEngine: CabinSensorEngine
    @Inject lateinit var repository: CabinTelemetryRepository

    // Hủy toàn bộ coroutine khi service dừng.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Job của vòng collect — dùng để tránh tạo trùng khi onStartCommand bị gọi lại.
    private var collectJob: Job? = null

    // Đếm số bản ghi service đã thu thập trong phiên chạy hiện tại.
    private var recordCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand (flags=$flags, startId=$startId)")
        // Đưa service lên foreground ngay lập tức với notification khởi tạo.
        startAsForeground()
        startCollecting()
        // START_STICKY: hệ thống tự khởi động lại service nếu bị kill do thiếu bộ nhớ.
        return START_STICKY
    }

    // Collect flow cảm biến và lưu vào Room ngay cả khi app đã tắt.
    private fun startCollecting() {
        // Nếu vòng collect đang chạy thì không tạo thêm (tránh ghi trùng / nhiều notification).
        if (collectJob?.isActive == true) {
            Log.d(TAG, "startCollecting bỏ qua — job đang chạy")
            return
        }
        collectJob = serviceScope.launch {
            try {
                sensorEngine.sensorFlow.collect { telemetry ->
                    repository.saveTelemetry(telemetry)
                    recordCount++
                    Log.d(TAG, "Đã lưu #$recordCount | temp=${telemetry.temperature} co2=${telemetry.co2Level} warn=${telemetry.isWarning}")
                    updateNotification(recordCount, telemetry.isWarning)
                }
            } catch (e: CancellationException) {
                throw e // Hủy bình thường (khi stop service) — không nuốt ngoại lệ này.
            } catch (e: Exception) {
                Log.e(TAG, "Vòng thu thập dừng do lỗi: ${e.message}", e)
            }
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification(count = 0, isWarning = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // Tạo notification channel (bắt buộc từ Android 8.0 / API 26).
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Giám sát cabin",
            NotificationManager.IMPORTANCE_LOW // LOW: không phát âm thanh mỗi lần cập nhật
        ).apply {
            description = "Thông báo trạng thái thu thập dữ liệu cảm biến nền"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    // Dựng notification hiển thị số record + trạng thái an toàn/cảnh báo.
    private fun buildNotification(count: Int, isWarning: Boolean): Notification {
        // Bấm vào notification sẽ mở lại MainActivity.
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isWarning) "⚠️ CẢNH BÁO" else "✅ An toàn"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CabinGuard đang giám sát")
            .setContentText("Đã thu thập: $count bản ghi • $statusText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)          // Không cho user vuốt tắt khi service còn chạy
            .setOnlyAlertOnce(true)    // Chỉ báo động lần đầu, các lần update sau im lặng
            .build()
    }

    private fun updateNotification(count: Int, isWarning: Boolean) {
        // Android 13+ (API 33) chỉ cho post notification khi đã có quyền POST_NOTIFICATIONS.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(count, isWarning))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "onDestroy — service bị dừng (đã thu thập $recordCount bản ghi)")
        // Hủy scope → dừng collect flow, tránh rò rỉ coroutine.
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "CabinService"
        private const val CHANNEL_ID = "cabin_telemetry_channel"
        private const val NOTIFICATION_ID = 1001

        // Bật service (dùng startForegroundService để tuân thủ giới hạn nền).
        fun start(context: Context) {
            val intent = Intent(context, CabinTelemetryService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        // Tắt service.
        fun stop(context: Context) {
            context.stopService(Intent(context, CabinTelemetryService::class.java))
        }
    }
}
