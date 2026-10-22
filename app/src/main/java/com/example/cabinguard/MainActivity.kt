package com.example.cabinguard

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.cabinguard.ui.navigation.CabinNavHost
import com.example.cabinguard.ui.theme.CabinGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val isLandscape = LocalConfiguration.current.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            val isAutomotive = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_AUTOMOTIVE
            )

            CabinGuardTheme(automotiveMode = isAutomotive && isLandscape) {
                CabinNavHost()
            }
        }
    }
}
