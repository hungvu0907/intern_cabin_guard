package com.example.cabinguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cabinguard.ui.dashboard.DashboardScreen
import com.example.cabinguard.ui.settings.SettingsScreen

private object CabinRoutes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

/** Điều hướng Dashboard ↔ Settings, dùng chung phone và automotive. */
@Composable
fun CabinNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = CabinRoutes.DASHBOARD
    ) {
        composable(CabinRoutes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(CabinRoutes.SETTINGS) }
            )
        }
        composable(CabinRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
