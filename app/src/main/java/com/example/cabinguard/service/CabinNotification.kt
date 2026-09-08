package com.example.cabinguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService

object CabinNotification {

    const val CHANNEL_ID = "cabin_guard_monitoring"
    const val NOTIF_ID = 1

    fun createChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>()
            ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Cabin monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    fun build(
        context: Context,
        recordCount: Int,
        isWarning: Boolean
    ): Notification {
        val status = if (isWarning) "CẢNH BÁO" else "AN TOÀN"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("CabinGuard")
            .setContentText("Records: $recordCount · $status")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun notify(
        context: Context,
        recordCount: Int,
        isWarning: Boolean
    ) {
        val manager = context.getSystemService<NotificationManager>()
            ?: return
        manager.notify(
            NOTIF_ID,
            build(context, recordCount, isWarning)
        )
    }
}
