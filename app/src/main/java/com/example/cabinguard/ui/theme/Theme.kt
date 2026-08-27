package com.example.cabinguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CabinColorScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    background = Color(0xFF0B1220),
    surface = Color(0xFF151C2C),
    onBackground = Color(0xFFF2F5FA),
    onSurface = Color(0xFFF2F5FA),
    error = Color(0xFFFF4D4D)
)

@Composable
fun CabinGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CabinColorScheme,
        content = content
    )
}
