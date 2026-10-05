package com.example.cabinguard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/** Dark color scheme tối ưu cho màn hình xe — độ tương phản cao, ít chói mắt */
val AutomotiveColorScheme = darkColorScheme(
    background = AutoBackground,
    surface = AutoSurface,
    surfaceVariant = AutoSurfaceVariant,
    primary = AutoPrimary,
    onPrimary = AutoOnPrimary,
    onBackground = AutoOnSurface,
    onSurface = AutoOnSurface,
    onSurfaceVariant = AutoOnSurfaceVariant,
    error = AutoWarningRed,
    primaryContainer = AutoSurfaceVariant,
    onPrimaryContainer = AutoOnSurface
)

/** true khi app đang ở chế độ Automotive landscape */
val LocalAutomotiveMode = staticCompositionLocalOf { false }

@Composable
fun CabinGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    automotiveMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        automotiveMode -> AutomotiveColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAutomotiveMode provides automotiveMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}