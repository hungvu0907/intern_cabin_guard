package com.example.cabinguard.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cabinguard.ui.theme.AutoBackground
import com.example.cabinguard.ui.theme.AutoGaugeCo2
import com.example.cabinguard.ui.theme.AutoGaugeTemp
import com.example.cabinguard.ui.theme.AutoSurface
import com.example.cabinguard.ui.theme.AutoSurfaceBorder
import com.example.cabinguard.ui.theme.AutoSurfaceVariant
import com.example.cabinguard.ui.theme.CabinMuted

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val close = { viewModel.discardDraft(); onBack() }
    BackHandler(enabled = true) { if (!state.saving) close() }
    LaunchedEffect(state.saved) {
        if (state.saved) { viewModel.consumeSaved(); close() }
    }
    SettingsContent(state, close, viewModel::temperatureChanged, viewModel::co2Changed, viewModel::save)
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onTemperatureChange: (String) -> Unit,
    onCo2Change: (String) -> Unit,
    onSave: () -> Unit
) {
    val co2Focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val canSave = !state.loading && !state.saving && state.dirty
    val save = { if (canSave) { keyboard?.hide(); onSave() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoBackground)
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header điều hướng Automotive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    enabled = !state.saving,
                    modifier = Modifier.heightIn(min = 56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Text("Quay lại", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0x2238BDF8), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "CẤU HÌNH BUỒNG LÁI",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ngưỡng cảnh báo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Điều chỉnh giới hạn cho cabin. Thay đổi áp dụng ngay sau khi lưu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CabinMuted
                )
            }

            if (state.loading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF38BDF8))
                    Text("Đang đọc cài đặt...", style = MaterialTheme.typography.bodySmall, color = CabinMuted)
                }
            }

            // Card cấu hình Nhiệt độ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutoSurface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, AutoSurfaceBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌡", fontSize = 18.sp)
                    Text("CẢNH BÁO NHIỆT ĐỘ", color = AutoGaugeTemp, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }

                OutlinedTextField(
                    value = state.temperature,
                    onValueChange = onTemperatureChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading && !state.saving,
                    label = { Text("Nhiệt độ (°C)") },
                    singleLine = true,
                    isError = state.temperatureError != null,
                    supportingText = {
                        Text(state.temperatureError ?: "Từ 30 đến 60 °C · mặc định 38 °C")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { co2Focus.requestFocus() }),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Card cấu hình CO2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutoSurface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, AutoSurfaceBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🫁", fontSize = 18.sp)
                    Text("CẢNH BÁO NỒNG ĐỘ KHÍ CO₂", color = AutoGaugeCo2, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }

                OutlinedTextField(
                    value = state.co2,
                    onValueChange = onCo2Change,
                    modifier = Modifier.fillMaxWidth().focusRequester(co2Focus),
                    enabled = !state.loading && !state.saving,
                    label = { Text("CO₂ (ppm)") },
                    singleLine = true,
                    isError = state.co2Error != null,
                    supportingText = {
                        Text(state.co2Error ?: "Từ 500 đến 3000 ppm · mặc định 1000 ppm")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            // Nút Lưu kiểu Automotive HUD
            Button(
                onClick = save,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF090D15),
                    disabledContainerColor = AutoSurfaceVariant,
                    disabledContentColor = CabinMuted
                )
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF090D15))
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = if (state.saving) "Đang lưu..." else "Lưu",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = when {
                    state.loading -> "Đang chuẩn bị cài đặt"
                    state.error != null -> "Cài đặt chưa được lưu"
                    state.dirty -> "Có thay đổi chưa lưu. Quay lại sẽ bỏ các thay đổi này."
                    else -> "Cài đặt đã được lưu."
                },
                style = MaterialTheme.typography.bodySmall,
                color = CabinMuted
            )

            HorizontalDivider(color = AutoSurfaceBorder)

            // Thông tin lưu trữ trên thiết bị
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutoSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Lưu trữ trên thiết bị",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Log đã sync giữ 24 giờ. Log chưa sync giữ tối đa 7 ngày. Kho sync giả lập vẫn nằm trên thiết bị.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CabinMuted
                )
            }
        }
    }
}
