package com.example.cabinguard.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.domain.model.AlertThresholds
import com.example.cabinguard.ui.theme.AutoGaugeCo2
import com.example.cabinguard.ui.theme.AutoGaugePressure
import com.example.cabinguard.ui.theme.AutoGaugeTemp
import com.example.cabinguard.ui.theme.AutoWarningRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layout 2-pane ngang cho Automotive landscape:
 * Trái (55%) = 3 gauge hình vòng cung xếp ngang
 * Phải (45%) = tiêu đề + lịch sử log
 */
@Composable
fun AutomotiveDashboardContent(
    data: CabinTelemetry,
    isWarning: Boolean,
    historyLogs: List<CabinTelemetry>,
    thresholds: AlertThresholds = AlertThresholds(),
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // === CỘT TRÁI: 3 Gauge ===
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Status bar trên cùng
            AutomotiveStatusBar(isWarning = isWarning)

            Spacer(modifier = Modifier.height(8.dp))

            // 3 gauge xếp ngang
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutomotiveGaugeCard(
                    label = "Nhiệt độ",
                    emoji = "🌡",
                    value = data.temperature,
                    unit = "°C",
                    minValue = 25f,
                    maxValue = 45f,
                    arcColor = AutoGaugeTemp,
                    isWarning = isWarning && data.temperature > thresholds.tempThreshold,
                    modifier = Modifier.weight(1f)
                )
                AutomotiveGaugeCard(
                    label = "Áp suất",
                    emoji = "💨",
                    value = data.pressure,
                    unit = "hPa",
                    minValue = 980f,
                    maxValue = 1020f,
                    arcColor = AutoGaugePressure,
                    isWarning = false,
                    modifier = Modifier.weight(1f)
                )
                AutomotiveGaugeCard(
                    label = "CO₂",
                    emoji = "🫁",
                    value = data.co2Level,
                    unit = "ppm",
                    minValue = 400f,
                    maxValue = 1200f,
                    arcColor = AutoGaugeCo2,
                    isWarning = isWarning && data.co2Level > thresholds.co2Threshold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        )

        // === CỘT PHẢI: Điều khiển + Log ===
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CabinGuard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onOpenSettings) {
                    Text("Cài đặt")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Tiêu đề log
            Text(
                text = "📋 Log (${historyLogs.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Danh sách log cuộn dọc
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(items = historyLogs, key = { it.id }) { log ->
                    AutomotiveLogItem(log = log, thresholds = thresholds)
                }
            }
        }
    }
}

@Composable
private fun AutomotiveStatusBar(isWarning: Boolean) {
    val bgColor by animateColorAsState(
        targetValue = if (isWarning) AutoWarningRed else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(500),
        label = "auto_status_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isWarning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(500),
        label = "auto_status_text"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isWarning) "⚠️ CHỈ SỐ NGUY HIỂM — KIỂM TRA CABIN!" else "✅ Cabin an toàn",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
private fun AutomotiveLogItem(log: CabinTelemetry, thresholds: AlertThresholds) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeText = timeFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log.isWarning) Color(0xFF3D1114) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = "${String.format("%.1f", log.temperature)}°C",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = if (log.temperature > thresholds.tempThreshold)
                    AutoWarningRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format("%.0f", log.co2Level)}ppm",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = if (log.co2Level > thresholds.co2Threshold)
                    AutoWarningRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (log.isWarning) "⚠️" else "✅",
                fontSize = 12.sp
            )
        }
    }
}
