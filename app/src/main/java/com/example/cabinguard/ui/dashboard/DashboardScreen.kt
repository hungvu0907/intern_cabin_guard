package com.example.cabinguard.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.cabinguard.domain.sensor.CabinThresholds
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

/** Kích thước chạm tối thiểu theo guideline Android Automotive. */
private val MinTouchTarget = 76.dp

/** Quy đổi một giá trị đo về tỉ lệ 0..1 trên dải [min, max] để vẽ gauge. */
private fun Double.progressIn(min: Double, max: Double): Float =
    ((this - min) / (max - min)).toFloat().coerceIn(0f, 1f)

private val timeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        uiState = uiState,
        onTogglePause = {
            if (uiState.isPaused) {
                viewModel.resumeMonitoring()
            } else {
                viewModel.pauseMonitoring()
            }
        }
    )
}

/**
 * Layout ngang cho màn hình Android Automotive: header ở trên, bên dưới chia
 * 60% số đo (3 ô nằm ngang) và 40% lịch sử log.
 */
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onTogglePause: () -> Unit = {}
) {
    val background by animateColorAsState(
        targetValue = if (uiState.isWarning) WarningBackground else SafeBackground,
        label = "dashboard-background"
    )
    val cardColor by animateColorAsState(
        targetValue = if (uiState.isWarning) CardWarning else CardSafe,
        label = "dashboard-card"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        DashboardHeader(
            uiState = uiState,
            cardColor = cardColor,
            onTogglePause = onTogglePause
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MetricsPane(
                latest = uiState.latest,
                cardColor = cardColor,
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            )
            HistoryPane(
                history = uiState.history,
                cardColor = cardColor,
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    uiState: DashboardUiState,
    cardColor: Color,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CabinGuard",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    uiState.isPaused -> "Đã tạm dừng hiển thị · Service vẫn ghi log"
                    uiState.isWarning -> "CẢNH BÁO: quá nhiệt hoặc khí độc"
                    else -> "Cabin an toàn"
                },
                color = when {
                    uiState.isPaused -> Color.White.copy(alpha = 0.7f)
                    uiState.isWarning -> Color(0xFFFFC9C9)
                    else -> SafeAccent
                },
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Button(
            onClick = onTogglePause,
            modifier = Modifier.heightIn(min = MinTouchTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = cardColor,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 32.dp)
        ) {
            Text(
                text = if (uiState.isPaused) "Tiếp tục" else "Tạm dừng",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun MetricsPane(
    latest: CabinTelemetry?,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    if (latest == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Đang đọc cảm biến...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 18.sp
            )
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(cardColor = cardColor) {
            MetricGauge(
                label = "Nhiệt độ",
                valueText = "%.1f °C".format(latest.temperature),
                progress = latest.temperature.progressIn(
                    CabinThresholds.TEMPERATURE_MIN_CELSIUS,
                    CabinThresholds.TEMPERATURE_MAX_CELSIUS
                ),
                warning = latest.temperature > CabinThresholds.TEMPERATURE_WARNING_CELSIUS
            )
        }
        MetricCard(cardColor = cardColor) {
            MetricGauge(
                label = "Áp suất",
                valueText = "%.0f hPa".format(latest.pressure),
                progress = latest.pressure.progressIn(
                    CabinThresholds.PRESSURE_MIN_HPA,
                    CabinThresholds.PRESSURE_MAX_HPA
                ),
                warning = false
            )
        }
        MetricCard(cardColor = cardColor) {
            Co2Reading(co2Level = latest.co2Level)
        }
    }
}

/** Một ô số đo; 3 ô chia đều chiều ngang và cao bằng nhau. */
@Composable
private fun RowScope.MetricCard(
    cardColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(cardColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MetricGauge(
    label: String,
    valueText: String,
    progress: Float,
    warning: Boolean
) {
    val accent = if (warning) WarningAccent else SafeAccent

    // aspectRatio tự lấy cạnh nhỏ hơn của ô, nên gauge co theo màn hình thấp.
    Box(
        modifier = Modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
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
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        }
    }
}

@Composable
private fun Co2Reading(co2Level: Int) {
    val co2OverLimit = co2Level > CabinThresholds.CO2_WARNING_PPM
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "CO2", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        Text(
            text = "$co2Level ppm",
            color = if (co2OverLimit) Color.White else SafeAccent,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (co2OverLimit) {
                "Vượt ngưỡng ${CabinThresholds.CO2_WARNING_PPM} ppm"
            } else {
                "Dưới ngưỡng ${CabinThresholds.CO2_WARNING_PPM} ppm"
            },
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun HistoryPane(
    history: List<CabinTelemetry>,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Lịch sử log",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = history,
                key = { it.id }
            ) { item ->
                HistoryRow(item = item, cardColor = cardColor)
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

@Preview(device = "spec:width=1280dp,height=720dp,dpi=160")
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
