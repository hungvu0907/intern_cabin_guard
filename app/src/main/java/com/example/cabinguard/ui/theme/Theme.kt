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

internal val CabinInk = Color(0xFF10141C)
internal val CabinPanel = Color(0xFF1A212C)
internal val CabinLine = Color(0x22E8E4DC)
internal val CabinSand = Color(0xFFC4A574)
internal val CabinSafe = Color(0xFF7D9B86)
internal val CabinDanger = Color(0xFFC45C5C)
internal val CabinDangerInk = Color(0xFF3A1616)
internal val CabinDangerPanel = Color(0xFF5A2424)
internal val CabinOnInk = Color(0xFFE8E4DC)
internal val CabinMuted = Color(0xFF9A948A)

private val CabinColorScheme = darkColorScheme(
    primary = CabinSand,
    onPrimary = Color(0xFF1A140C),
    background = CabinInk,
    surface = CabinPanel,
    onBackground = CabinOnInk,
    onSurface = CabinOnInk,
    error = CabinDanger,
    secondary = CabinSafe
)

private val CabinTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp,
        color = CabinOnInk
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.15.sp,
        color = CabinOnInk
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        color = CabinMuted
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
        color = CabinOnInk
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum",
        color = CabinMuted
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = CabinMuted
    )
)

@Composable
fun CabinGuardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CabinColorScheme,
        typography = CabinTypography,
        content = content
    )
}
