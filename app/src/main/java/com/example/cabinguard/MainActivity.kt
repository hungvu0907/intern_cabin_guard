package com.example.cabinguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.cabinguard.service.CabinTelemetryService
import com.example.cabinguard.ui.dashboard.DashboardScreen
import com.example.cabinguard.ui.theme.CabinGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        startTelemetryService()
        setContent {
            CabinGuardTheme {
                DashboardScreen()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }

    private fun startTelemetryService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, CabinTelemetryService::class.java)
        )
    }

    private companion object {
        const val REQUEST_POST_NOTIFICATIONS = 1001
    }
}
