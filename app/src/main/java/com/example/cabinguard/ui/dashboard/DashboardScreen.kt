package com.example.cabinguard.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.ui.theme.CabinGuardTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SafeBackground = Color(0xFF0B1220)
private val WarningBackground = Color(0xFF7A1212)
private val CardSafe = Color(0xFF151C2C)
private val CardWarning = Color(0xFF9B1C1C)
private val TrackColor = Color(0x33FFFFFF)
private val SafeAccent = Color(0xFF3DDC97)
private val WarningAccent = Color(0xFFFF6B6B)

private val timeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(uiState = uiState)
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState
) {
    val background by animateColorAsState(
        targetValue = if (uiState.isWarning) WarningBackground else SafeBackground,
        label = "dashboard-background"
    )
    val cardColor by animateColorAsState(
        targetValue = if (uiState.isWarning) CardWarning else CardSafe,
        label = "dashboard-card"
    )
    val latest = uiState.latest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "CabinGuard",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (uiState.isWarning) "CẢNH BÁO: quá nhiệt hoặc khí độc" else "Cabin an toàn",
            color = if (uiState.isWarning) Color(0xFFFFC9C9) else SafeAccent,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (latest == null) {
            Text(text = "Đang đọc cảm biến...", color = Color.White.copy(alpha = 0.7f))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricGauge(
                    label = "Nhiệt độ",
                    valueText = "%.1f °C".format(latest.temperature),
                    progress = ((latest.temperature - 18.0) / 32.0).toFloat().coerceIn(0f, 1f),
                    warning = latest.temperature > 38,
                    modifier = Modifier.weight(1f)
                )
                MetricGauge(
                    label = "Áp suất",
                    valueText = "%.0f hPa".format(latest.pressure),
                    progress = ((latest.pressure - 100.0) / 1900.0).toFloat().coerceIn(0f, 1f),
                    warning = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "CO2", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text(
                        text = "${latest.co2Level} ppm",
                        color = if (latest.co2Level > 1000) Color.White else SafeAccent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (latest.co2Level > 1000) "Vượt ngưỡng 1000 ppm" else "Dưới ngưỡng 1000 ppm",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Lịch sử log",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = uiState.history,
                key = { it.id }
            ) { item ->
                HistoryRow(item = item, cardColor = cardColor)
            }
        }
    }
}

@Composable
private fun MetricGauge(
    label: String,
    valueText: String,
    progress: Float,
    warning: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (warning) WarningAccent else SafeAccent

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(148.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(size.minDimension - strokeWidth, size.minDimension - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                drawArc(
                    color = TrackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                drawArc(
                    color = accent,
                    startAngle = 135f,
                    sweepAngle = 270f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: CabinTelemetry,
    cardColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = timeFormatter.format(Instant.ofEpochMilli(item.timestamp)),
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "%.1f°C  ·  %.0f hPa  ·  %d ppm".format(
                    item.temperature,
                    item.pressure,
                    item.co2Level
                ),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = if (item.isWarning) "CẢNH BÁO" else "AN TOÀN",
            color = if (item.isWarning) WarningAccent else SafeAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun DashboardWarningPreview() {
    CabinGuardTheme {
        DashboardContent(
            uiState = DashboardUiState(
                latest = CabinTelemetry(
                    id = 1,
                    timestamp = System.currentTimeMillis(),
                    temperature = 41.2,
                    pressure = 1008.0,
                    co2Level = 1180,
                    isWarning = true
                ),
                history = emptyList()
            )
        )
    }
}
