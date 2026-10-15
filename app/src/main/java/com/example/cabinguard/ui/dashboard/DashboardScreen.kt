package com.example.cabinguard.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.sensor.CabinThresholds
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import com.example.cabinguard.ui.settings.SettingsScreen
import com.example.cabinguard.ui.theme.AutoBackground
import com.example.cabinguard.ui.theme.AutoBackgroundWarning
import com.example.cabinguard.ui.theme.AutoGaugeCo2
import com.example.cabinguard.ui.theme.AutoGaugePressure
import com.example.cabinguard.ui.theme.AutoGaugeTemp
import com.example.cabinguard.ui.theme.AutoSafeGreen
import com.example.cabinguard.ui.theme.AutoSurface
import com.example.cabinguard.ui.theme.AutoSurfaceBorder
import com.example.cabinguard.ui.theme.AutoSurfaceBorderWarning
import com.example.cabinguard.ui.theme.AutoSurfaceWarning
import com.example.cabinguard.ui.theme.AutoTrackColor
import com.example.cabinguard.ui.theme.AutoWarningRed
import com.example.cabinguard.ui.theme.CabinGuardTheme
import com.example.cabinguard.ui.theme.CabinMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

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
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.exportResults.collect { result ->
                when (result) {
                    is ExportResult.Ready -> shareCsv(context, result.uri)
                    ExportResult.Empty -> toast(context, "Chưa có log để xuất")
                    ExportResult.Failed -> toast(context, "Xuất CSV thất bại")
                }
            }
        }
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    DashboardContent(
        uiState = uiState,
        onTogglePause = {
            if (uiState.isPaused) {
                viewModel.resumeMonitoring()
            } else {
                viewModel.pauseMonitoring()
            }
        },
        isExporting = isExporting,
        onExportCsv = viewModel::exportCsv,
        onEditThresholds = { showSettings = true }
    )
}

private fun shareCsv(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Cabin CSV", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Chia sẻ lịch sử log"))
    } catch (error: android.content.ActivityNotFoundException) {
        android.util.Log.w("DashboardScreen", "No application can share CSV", error)
        toast(context, "Không có ứng dụng nhận file CSV")
    } catch (error: SecurityException) {
        android.util.Log.e("DashboardScreen", "Cannot grant CSV sharing permission", error)
        toast(context, "Không thể cấp quyền chia sẻ CSV. Vui lòng thử lại.")
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/**
 * Layout Automotive HMI hiện đại:
 * Phản hồi mượt mà cho trạng thái cảnh báo, hỗ trợ cả màn hình xe hơi Landscape lẫn màn hình dọc điện thoại.
 */
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onTogglePause: () -> Unit = {},
    isExporting: Boolean = false,
    onExportCsv: () -> Unit = {},
    onEditThresholds: () -> Unit = {}
) {
    val background by animateColorAsState(
        targetValue = if (uiState.isWarning) AutoBackgroundWarning else AutoBackground,
        animationSpec = tween(durationMillis = 400),
        label = "dashboard-bg"
    )
    val cardColor by animateColorAsState(
        targetValue = if (uiState.isWarning) AutoSurfaceWarning else AutoSurface,
        animationSpec = tween(durationMillis = 400),
        label = "dashboard-card"
    )
    val borderColor by animateColorAsState(
        targetValue = if (uiState.isWarning) AutoSurfaceBorderWarning else AutoSurfaceBorder,
        animationSpec = tween(durationMillis = 400),
        label = "dashboard-border"
    )
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 1000.dp || maxHeight < 600.dp || fontScale > 1.3f) {
            CompactDashboard(
                uiState = uiState,
                background = background,
                cardColor = cardColor,
                borderColor = borderColor,
                onTogglePause = onTogglePause,
                isExporting = isExporting,
                onExportCsv = onExportCsv,
                onEditThresholds = onEditThresholds
            )
        } else {
            WideDashboard(
                uiState = uiState,
                background = background,
                cardColor = cardColor,
                borderColor = borderColor,
                onTogglePause = onTogglePause,
                isExporting = isExporting,
                onExportCsv = onExportCsv,
                onEditThresholds = onEditThresholds
            )
        }
    }
}

/**
 * Layout ngang tối ưu cho màn hình Android Automotive Head Unit:
 * Thanh điều khiển Cockpit trên cùng, bên dưới chia tỉ lệ 60/40 giữa cụm đồng hồ đo và bảng telemetry.
 */
@Composable
private fun WideDashboard(
    uiState: DashboardUiState,
    background: Color,
    cardColor: Color,
    borderColor: Color,
    onTogglePause: () -> Unit,
    isExporting: Boolean,
    onExportCsv: () -> Unit,
    onEditThresholds: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        DashboardHeader(
            uiState = uiState,
            cardColor = cardColor,
            borderColor = borderColor,
            onTogglePause = onTogglePause,
            onEditThresholds = onEditThresholds
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            MetricsPane(
                latest = uiState.latest,
                warningThresholds = uiState.warningThresholds,
                cardColor = cardColor,
                borderColor = borderColor,
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
            )
            HistoryPane(
                history = uiState.history,
                cardColor = cardColor,
                borderColor = borderColor,
                isExporting = isExporting,
                onExportCsv = onExportCsv,
                warningThresholds = uiState.warningThresholds,
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Chế độ Compact hiển thị dạng cuộn một bề mặt nhưng vẫn giữ trọn vẹn visual cụm đồng hồ ô tô.
 */
@Composable
private fun CompactDashboard(
    uiState: DashboardUiState,
    background: Color,
    cardColor: Color,
    borderColor: Color,
    onTogglePause: () -> Unit,
    isExporting: Boolean,
    onExportCsv: () -> Unit,
    onEditThresholds: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CabinGuard",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    StatusBeacon(uiState = uiState)
                }

                Text(
                    text = uiState.statusText,
                    color = statusColor(uiState),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ngưỡng: %.1f °C · %d ppm".format(
                            uiState.warningThresholds.temperatureCelsius,
                            uiState.warningThresholds.co2Ppm
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sync giả lập trên thiết bị · ${uiState.unsyncedCount} log chờ sync",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            uiState.latest?.let { row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardColor, RoundedCornerShape(18.dp))
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THÔNG SỐ BUỒNG LÁI",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Đo lúc " + timeFormatter.format(Instant.ofEpochMilli(row.timestamp)),
                            color = CabinMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 3 đồng hồ đo thu nhỏ dạng vòng cung (Dial Gauge) cho giao diện dọc
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactMetricItem(
                            label = "Nhiệt độ",
                            valueText = "%.1f".format(row.temperature),
                            unit = "°C",
                            progress = row.temperature.progressIn(
                                CabinThresholds.TEMPERATURE_MIN_CELSIUS,
                                CabinThresholds.TEMPERATURE_MAX_CELSIUS
                            ),
                            accentColor = AutoGaugeTemp,
                            isWarning = row.temperature > uiState.warningThresholds.temperatureCelsius,
                            modifier = Modifier.weight(1f)
                        )
                        CompactMetricItem(
                            label = "Áp suất",
                            valueText = "%.0f".format(row.pressure),
                            unit = "hPa",
                            progress = row.pressure.progressIn(
                                CabinThresholds.PRESSURE_MIN_HPA,
                                CabinThresholds.PRESSURE_MAX_HPA
                            ),
                            accentColor = AutoGaugePressure,
                            isWarning = false,
                            modifier = Modifier.weight(1f)
                        )
                        CompactMetricItem(
                            label = "CO₂",
                            valueText = "${row.co2Level}",
                            unit = "ppm",
                            progress = row.co2Level.toDouble().progressIn(
                                CabinThresholds.CO2_MIN_PPM.toDouble(),
                                uiState.warningThresholds.co2Ppm.toDouble().coerceAtLeast(1200.0)
                            ),
                            accentColor = AutoGaugeCo2,
                            isWarning = row.co2Level > uiState.warningThresholds.co2Ppm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } ?: Text("Chưa có số đo", color = CabinMuted, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Button(
                onClick = onExportCsv,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardColor,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = if (isExporting) "Đang xuất..." else "Xuất CSV",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEditThresholds,
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardColor,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text("Chỉnh ngưỡng", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                OutlinedButton(
                    onClick = onTogglePause,
                    enabled = uiState.latest != null && uiState.readError == null,
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (uiState.isPaused) AutoGaugeTemp else Color.White
                    )
                ) {
                    Text(
                        text = if (uiState.isPaused) "Tiếp tục" else "Tạm dừng hiển thị",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Lịch sử log",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$RECENT_HISTORY_LIMIT mẫu gần nhất · CSV xuất toàn bộ log hiện có",
                    style = MaterialTheme.typography.bodySmall,
                    color = CabinMuted
                )
            }
        }

        if (uiState.history.isEmpty()) {
            item { Text("Chưa có log để hiển thị", color = CabinMuted) }
        }
        items(uiState.history, key = { it.id }) {
            HistoryRow(it, cardColor, borderColor, uiState.warningThresholds)
        }
    }
}

private fun statusColor(state: DashboardUiState): Color = when {
    state.readError != null -> AutoWarningRed
    state.latest == null || state.loading || state.isPaused -> CabinMuted
    state.isWarning -> AutoWarningRed
    else -> AutoSafeGreen
}

/**
 * Thanh tiêu đề buồng lái: hiển thị tên hệ thống, trạng thái trực quan dạng đèn tín hiệu và nút thao tác.
 */
@Composable
private fun DashboardHeader(
    uiState: DashboardUiState,
    cardColor: Color,
    borderColor: Color,
    onTogglePause: () -> Unit,
    onEditThresholds: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CabinGuard",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Box(
                    modifier = Modifier
                        .background(Color(0x2238BDF8), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "COCKPIT HUD",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Sync giả lập · ${uiState.unsyncedCount} log chờ sync",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                StatusBeacon(uiState = uiState)
                Text(
                    text = uiState.statusText,
                    color = statusColor(uiState),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Ngưỡng: %.1f °C · %d ppm".format(
                        uiState.warningThresholds.temperatureCelsius,
                        uiState.warningThresholds.co2Ppm
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                uiState.latest?.let { row ->
                    Text(
                        text = "•  Đo lúc " + timeFormatter.format(Instant.ofEpochMilli(row.timestamp)),
                        color = CabinMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onEditThresholds,
                modifier = Modifier.heightIn(min = MinTouchTarget),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardColor,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, borderColor),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Text(text = "Chỉnh ngưỡng", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onTogglePause,
                enabled = uiState.latest != null && uiState.readError == null,
                modifier = Modifier.heightIn(min = MinTouchTarget),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isPaused) Color(0xFF2E2214) else cardColor,
                    contentColor = if (uiState.isPaused) AutoGaugeTemp else Color.White
                ),
                border = BorderStroke(1.dp, if (uiState.isPaused) AutoGaugeTemp.copy(alpha = 0.5f) else borderColor),
                contentPadding = PaddingValues(horizontal = 32.dp)
            ) {
                Text(
                    text = if (uiState.isPaused) "Tiếp tục" else "Tạm dừng",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Đèn tín hiệu trạng thái trực quan như trên bảng đồng hồ ô tô */
@Composable
private fun StatusBeacon(uiState: DashboardUiState) {
    val beaconColor = statusColor(uiState)
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(beaconColor.copy(alpha = 0.25f), CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(beaconColor, CircleShape)
        )
    }
}

@Composable
private fun MetricsPane(
    latest: CabinTelemetry?,
    warningThresholds: CabinWarningThresholds,
    cardColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    if (latest == null) {
        Box(
            modifier = modifier
                .background(cardColor, RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Đang đọc cảm biến...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Hệ thống đang kết nối telemetry buồng lái",
                    color = CabinMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val tempWarning = latest.temperature > warningThresholds.temperatureCelsius
        val co2Warning = latest.co2Level > warningThresholds.co2Ppm

        AutomotiveGaugeCard(
            label = "Nhiệt độ",
            icon = "🌡",
            valueText = "%.1f".format(latest.temperature),
            unit = "°C",
            progress = latest.temperature.progressIn(
                CabinThresholds.TEMPERATURE_MIN_CELSIUS,
                CabinThresholds.TEMPERATURE_MAX_CELSIUS
            ),
            accentColor = AutoGaugeTemp,
            isWarning = tempWarning,
            minLabel = "25°C",
            maxLabel = "45°C",
            statusText = if (tempWarning) "QUÁ NHIỆT" else "BÌNH THƯỜNG",
            thresholdNote = "Giới hạn %.1f °C".format(warningThresholds.temperatureCelsius),
            cardColor = cardColor,
            borderColor = borderColor
        )

        AutomotiveGaugeCard(
            label = "Áp suất",
            icon = "💨",
            valueText = "%.0f".format(latest.pressure),
            unit = "hPa",
            progress = latest.pressure.progressIn(
                CabinThresholds.PRESSURE_MIN_HPA,
                CabinThresholds.PRESSURE_MAX_HPA
            ),
            accentColor = AutoGaugePressure,
            isWarning = false,
            minLabel = "980",
            maxLabel = "1020",
            statusText = "CHUẨN",
            thresholdNote = "Mục tiêu 1013 hPa",
            cardColor = cardColor,
            borderColor = borderColor
        )

        AutomotiveGaugeCard(
            label = "Nồng độ CO₂",
            icon = "🫁",
            valueText = "${latest.co2Level}",
            unit = "ppm",
            progress = latest.co2Level.toDouble().progressIn(
                CabinThresholds.CO2_MIN_PPM.toDouble(),
                warningThresholds.co2Ppm.toDouble().coerceAtLeast(1200.0)
            ),
            accentColor = AutoGaugeCo2,
            isWarning = co2Warning,
            minLabel = "400",
            maxLabel = "${warningThresholds.co2Ppm}+",
            statusText = if (co2Warning) "VƯỢT NGƯỠNG" else "TRONG LÀNH",
            thresholdNote = if (co2Warning) "Vượt ngưỡng ${warningThresholds.co2Ppm} ppm" else "Trong ngưỡng ${warningThresholds.co2Ppm} ppm",
            cardColor = cardColor,
            borderColor = borderColor
        )
    }
}

/**
 * Đồng hồ kim vòng cung chuẩn HMI ô tô:
 * Thể hiện giá trị số to, vạch chia gauge kim loại, vòng đo gradient và nhãn trạng thái glanceable.
 */
@Composable
private fun RowScope.AutomotiveGaugeCard(
    label: String,
    icon: String,
    valueText: String,
    unit: String,
    progress: Float,
    accentColor: Color,
    isWarning: Boolean,
    minLabel: String,
    maxLabel: String,
    statusText: String,
    thresholdNote: String,
    cardColor: Color,
    borderColor: Color
) {
    val displayColor = if (isWarning) AutoWarningRed else accentColor
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "gauge-anim"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(cardColor, RoundedCornerShape(20.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isWarning) AutoSurfaceBorderWarning else borderColor
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header trên card: Icon + Tên thông số + Badge trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = icon, fontSize = 16.sp)
                    Text(
                        text = label.uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(displayColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        color = displayColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Vòng tròn Gauge hình vòm buồng lái
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.minDimension <= 0f) return@Canvas
                    val strokeWidth = minOf(14.dp.toPx(), size.minDimension / 6)
                    val arcSize = Size(size.minDimension - strokeWidth * 2, size.minDimension - strokeWidth * 2)
                    val topLeft = Offset(strokeWidth, strokeWidth)
                    val startAngle = 135f
                    val sweepRange = 270f

                    // Vẽ các vạch chia nhỏ (Ticks) xung quanh viền đồng hồ
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.minDimension / 2f
                    val tickLength = 6.dp.toPx()
                    val totalTicks = 24

                    for (i in 0..totalTicks) {
                        val tickAngle = startAngle + (sweepRange / totalTicks) * i
                        val tickFraction = i.toFloat() / totalTicks
                        val rad = Math.toRadians(tickAngle.toDouble())
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()

                        val startP = Offset(
                            center.x + (outerRadius - tickLength) * cosVal,
                            center.y + (outerRadius - tickLength) * sinVal
                        )
                        val endP = Offset(
                            center.x + outerRadius * cosVal,
                            center.y + outerRadius * sinVal
                        )

                        val tickColor = if (tickFraction <= animatedProgress) {
                            displayColor.copy(alpha = 0.85f)
                        } else {
                            Color(0x33FFFFFF)
                        }

                        drawLine(
                            color = tickColor,
                            start = startP,
                            end = endP,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Vòng track nền
                    drawArc(
                        color = AutoTrackColor,
                        startAngle = startAngle,
                        sweepAngle = sweepRange,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Vòng giá trị hoạt động
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                displayColor.copy(alpha = 0.65f),
                                displayColor
                            )
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepRange * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Điểm sáng ở đầu kim gauge
                    if (animatedProgress > 0.02f) {
                        val currentAngle = startAngle + sweepRange * animatedProgress
                        val currentRad = Math.toRadians(currentAngle.toDouble())
                        val arcRadius = (size.minDimension - strokeWidth * 2) / 2f
                        val needlePoint = Offset(
                            center.x + arcRadius * cos(currentRad).toFloat(),
                            center.y + arcRadius * sin(currentRad).toFloat()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = strokeWidth * 0.45f,
                            center = needlePoint
                        )
                    }
                }

                // Chữ số đo trung tâm to rõ ràng
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = valueText,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " $unit",
                            color = displayColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        text = thresholdNote,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Thang đo Min - Max ở chân đồng hồ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = minLabel, color = CabinMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(text = maxLabel, color = CabinMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/** Hàng thông số đồng hồ vòng cung (Dial Arc Gauge) cho chế độ xem dọc trên điện thoại */
@Composable
private fun CompactMetricItem(
    label: String,
    valueText: String,
    unit: String,
    progress: Float,
    accentColor: Color,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    val displayColor = if (isWarning) AutoWarningRed else accentColor
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "compact-gauge-anim"
    )

    Column(
        modifier = modifier
            .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isWarning) AutoSurfaceBorderWarning else Color(0x1FFFFFFF)
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        // Vòng cung Gauge thu nhỏ
        Box(
            modifier = Modifier
                .size(72.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.minDimension <= 0f) return@Canvas
                val strokeWidth = 6.dp.toPx()
                val arcSize = Size(size.minDimension - strokeWidth * 2, size.minDimension - strokeWidth * 2)
                val topLeft = Offset(strokeWidth, strokeWidth)
                val startAngle = 135f
                val sweepRange = 270f

                // Vòng track nền
                drawArc(
                    color = AutoTrackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepRange,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Vòng giá trị hoạt động (gradient sweep)
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            displayColor.copy(alpha = 0.6f),
                            displayColor
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepRange * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Điểm sáng kim đo ở đầu vòng
                if (animatedProgress > 0.02f) {
                    val currentAngle = startAngle + sweepRange * animatedProgress
                    val currentRad = Math.toRadians(currentAngle.toDouble())
                    val arcRadius = (size.minDimension - strokeWidth * 2) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val needlePoint = Offset(
                        center.x + arcRadius * cos(currentRad).toFloat(),
                        center.y + arcRadius * sin(currentRad).toFloat()
                    )
                    drawCircle(
                        color = Color.White,
                        radius = strokeWidth * 0.5f,
                        center = needlePoint
                    )
                }
            }

            // Giá trị hiển thị ở tâm vòng tròn
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = valueText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = unit,
                    color = displayColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Bảng hiển thị Lịch sử log buồng lái:
 * Danh sách hiển thị theo phong cách telemetry telemetry strip với mốc giờ, thông số và badge rõ ràng.
 */
@Composable
private fun HistoryPane(
    history: List<CabinTelemetry>,
    cardColor: Color,
    borderColor: Color,
    isExporting: Boolean,
    onExportCsv: () -> Unit,
    warningThresholds: CabinWarningThresholds? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(cardColor, RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LỊCH SỬ TELEMETRY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$RECENT_HISTORY_LIMIT mẫu gần nhất · CSV xuất toàn bộ log",
                    color = CabinMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onExportCsv,
                enabled = !isExporting,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x2E38BDF8),
                    contentColor = Color(0xFF38BDF8)
                ),
                border = BorderStroke(1.dp, Color(0x4D38BDF8))
            ) {
                Text(
                    text = if (isExporting) "Đang xuất..." else "Xuất CSV",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có log để hiển thị",
                    color = CabinMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(
                    items = history,
                    key = { it.id }
                ) { item ->
                    HistoryRow(item = item, cardColor = Color(0x14FFFFFF), borderColor = Color(0x1AFFFFFF), warningThresholds = warningThresholds)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: CabinTelemetry,
    cardColor: Color,
    borderColor: Color = Color(0x1AFFFFFF),
    warningThresholds: CabinWarningThresholds? = null
) {
    val tempExceeded = warningThresholds?.let { item.temperature > it.temperatureCelsius } ?: item.isWarning
    val co2Exceeded = warningThresholds?.let { item.co2Level > it.co2Ppm } ?: item.isWarning

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Text(
                text = timeFormatter.format(Instant.ofEpochMilli(item.timestamp)),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "%.1f°C".format(item.temperature),
                    color = if (tempExceeded) AutoWarningRed else AutoGaugeTemp,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (tempExceeded) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = "%.0f hPa".format(item.pressure),
                    color = AutoGaugePressure,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.co2Level} ppm",
                    color = if (co2Exceeded) AutoWarningRed else AutoGaugeCo2,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (co2Exceeded) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        Box(
            modifier = Modifier
                .background(
                    if (item.isWarning) AutoWarningRed.copy(alpha = 0.2f) else AutoSafeGreen.copy(alpha = 0.2f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (item.isWarning) "CẢNH BÁO" else "AN TOÀN",
                color = if (item.isWarning) AutoWarningRed else AutoSafeGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Preview(name = "Phone Portrait", device = "spec:width=360dp,height=640dp,dpi=160")
@Composable
private fun DashboardPhonePreview() {
    CabinGuardTheme {
        DashboardContent(
            uiState = DashboardUiState(
                latest = CabinTelemetry(
                    id = 1,
                    timestamp = System.currentTimeMillis(),
                    temperature = 38.5,
                    pressure = 1012.0,
                    co2Level = 920,
                    isWarning = false
                ),
                history = emptyList()
            )
        )
    }
}

@Preview(name = "Automotive Landscape Warning", device = "spec:width=1280dp,height=720dp,dpi=160")
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
