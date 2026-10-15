package com.example.cabinguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ==========================================
// Automotive HMI & Cockpit Color Palette
// ==========================================
// Dark cockpit backgrounds
val AutoBackground = Color(0xFF090D15)
val AutoBackgroundWarning = Color(0xFF260A0E)
val AutoSurface = Color(0xFF121927)
val AutoSurfaceVariant = Color(0xFF1B2436)
val AutoSurfaceWarning = Color(0xFF381016)
val AutoSurfaceBorder = Color(0x2E6482AD)
val AutoSurfaceBorderWarning = Color(0x66FF453A)

// Automotive Instrument Gauge Accents
val AutoGaugeTemp = Color(0xFFFF9500)
val AutoGaugePressure = Color(0xFF00B4D8)
val AutoGaugeCo2 = Color(0xFF10B981)
val AutoWarningRed = Color(0xFFFF453A)
val AutoSafeGreen = Color(0xFF34D399)
val AutoTrackColor = Color(0x26FFFFFF)

// Legacy compatibility tokens for CabinWidget and existing modules
internal val CabinInk = AutoBackground
internal val CabinPanel = AutoSurface
internal val CabinLine = Color(0x22E8E4DC)
internal val CabinSand = Color(0xFF38BDF8)
internal val CabinSafe = AutoSafeGreen
internal val CabinDanger = AutoWarningRed
internal val CabinDangerInk = AutoBackgroundWarning
internal val CabinDangerPanel = AutoSurfaceWarning
internal val CabinOnInk = Color(0xFFF1F5F9)
internal val CabinMuted = Color(0xFF94A3B8)

private val AutomotiveColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF090D15),
    background = AutoBackground,
    surface = AutoSurface,
    surfaceVariant = AutoSurfaceVariant,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = AutoWarningRed,
    secondary = AutoSafeGreen
)

private val AutomotiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        letterSpacing = (-0.5).sp,
        color = Color.White
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp,
        color = Color.White
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        color = Color.White
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp,
        color = Color.White
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = Color(0xFF94A3B8)
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
        color = Color(0xFFF1F5F9)
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum",
        color = Color(0xFF94A3B8)
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = Color(0xFF94A3B8)
    )
)

@Composable
fun CabinGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AutomotiveColorScheme,
        typography = AutomotiveTypography,
        content = content
    )
}
