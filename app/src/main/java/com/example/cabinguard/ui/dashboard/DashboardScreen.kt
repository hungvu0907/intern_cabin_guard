package com.example.cabinguard.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.domain.model.CabinUiState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.example.cabinguard.service.CabinTelemetryService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CabinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyLogs by viewModel.historyLogs.collectAsState()

    //Animation màu nền khi chuyển trạng thái
    val isWarning = uiState is CabinUiState.Warning
    val backgroundColor by animateColorAsState(
        targetValue = if (isWarning) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 500),
        label = "bg_color"
    )
    val topBarColor by animateColorAsState(
        targetValue = if (isWarning) Color(0xFFC62828) else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 500),
        label = "topbar_color"
    )
    val topBarTextColor by animateColorAsState(
        targetValue = if (isWarning) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(durationMillis = 500),
        label = "topbar_text_color"
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isWarning) "⚠️ CabinGuard — CẢNH BÁO!" else "CabinGuard",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = topBarTextColor
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is CabinUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Đang kết nối cảm biến...", fontSize = 16.sp)
                        }
                    }
                }
                is CabinUiState.Normal -> {
                    DashboardContent(
                        data = state.data,
                        isWarning = false,
                        historyLogs = historyLogs
                    )
                }
                is CabinUiState.Warning -> {
                    DashboardContent(
                        data = state.data,
                        isWarning = true,
                        historyLogs = historyLogs
                    )
                }
            }
        }
    }
} 
// Hiển thị 3 đồng hồ đo + lịch sử log realtime.
@Composable
private fun DashboardContent(
    data: CabinTelemetry,
    isWarning: Boolean,
    historyLogs: List<CabinTelemetry>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Nút điều khiển Foreground Service (giám sát nền)
        item {
            ServiceControlRow()
        }

        // Banner cảnh báo — chỉ hiển thị khi isWarning = true
        if (isWarning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE53935))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠️ PHÁT HIỆN CHỈ SỐ NGUY HIỂM!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Gauge 1: Nhiệt độ (°C)
        item {
            SensorGaugeCard(
                label = "Nhiệt độ",
                emoji = "🌡",
                value = data.temperature,
                unit = "°C",
                minValue = 25f,
                maxValue = 45f,
                barColor = Color(0xFFFF9800),
                isWarning = isWarning && data.temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD
            )
        }

        // Gauge 2: Áp suất (hPa)
        item {
            SensorGaugeCard(
                label = "Áp suất",
                emoji = "💨",
                value = data.pressure,
                unit = "hPa",
                minValue = 980f,
                maxValue = 1020f,
                barColor = Color(0xFF2196F3),
                isWarning = false
            )
        }

        // Gauge 3: CO2 (ppm)
        item {
            SensorGaugeCard(
                label = "CO2",
                emoji = "🫁",
                value = data.co2Level,
                unit = "ppm",
                minValue = 400f,
                maxValue = 1200f,
                barColor = Color(0xFF4CAF50),
                isWarning = isWarning && data.co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD
            )
        }

        // [UI-03] Tiêu đề phần lịch sử log
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "📋 Lịch sử Log (${historyLogs.size} bản ghi)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // [UI-03] Danh sách log — LazyColumn items
        items(
            items = historyLogs,
            key = { it.id }
        ) { log ->
            LogHistoryItem(log = log)
        }
    }
}

@Composable
private fun ServiceControlRow() {
    val context = LocalContext.current

    // Launcher xin quyền notification; nếu được cấp thì bật service ngay.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        CabinTelemetryService.start(context)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    CabinTelemetryService.start(context)
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("▶ Bật giám sát nền")
        }
        OutlinedButton(
            onClick = { CabinTelemetryService.stop(context) },
            modifier = Modifier.weight(1f)
        ) {
            Text("⏹ Tắt giám sát nền")
        }
    }
}

@Composable
private fun LogHistoryItem(log: CabinTelemetry) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeText = timeFormat.format(Date(log.timestamp))

    val bgColor = if (log.isWarning) Color(0xFFFFEBEE) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cột 1: Thời gian
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Cột 2: Nhiệt độ
            Text(
                text = "${String.format("%.1f", log.temperature)}°C",
                fontWeight = FontWeight.Medium,
                color = if (log.temperature > CabinTelemetry.TEMP_WARNING_THRESHOLD)
                    Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
            )
            // Cột 3: CO2
            Text(
                text = "${String.format("%.0f", log.co2Level)} ppm",
                fontWeight = FontWeight.Medium,
                color = if (log.co2Level > CabinTelemetry.CO2_WARNING_THRESHOLD)
                    Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
            )
            // Cột 4: Icon cảnh báo
            Text(
                text = if (log.isWarning) "⚠️" else "✅",
                fontSize = 16.sp
            )
        }
    }
}
