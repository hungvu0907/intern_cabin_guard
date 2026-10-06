package com.example.cabinguard.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().imePadding()) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().align(androidx.compose.ui.Alignment.TopCenter)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextButton(onClick = onBack, enabled = !state.saving, modifier = Modifier.heightIn(min = 56.dp)) { Text("Quay lại") }
            Text("Ngưỡng cảnh báo", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text("Điều chỉnh giới hạn cho cabin. Thay đổi áp dụng ngay sau khi lưu.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.loading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Đang đọc cài đặt...", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedTextField(
                value = state.temperature, onValueChange = onTemperatureChange, modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading && !state.saving, label = { Text("Nhiệt độ (°C)") }, singleLine = true,
                isError = state.temperatureError != null,
                supportingText = { Text(state.temperatureError ?: "Từ 30 đến 60 °C · mặc định 38 °C") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { co2Focus.requestFocus() })
            )
            OutlinedTextField(
                value = state.co2, onValueChange = onCo2Change, modifier = Modifier.fillMaxWidth().focusRequester(co2Focus),
                enabled = !state.loading && !state.saving, label = { Text("CO₂ (ppm)") }, singleLine = true,
                isError = state.co2Error != null,
                supportingText = { Text(state.co2Error ?: "Từ 500 đến 3000 ppm · mặc định 1000 ppm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() })
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            Button(onClick = save, enabled = canSave, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text(if (state.saving) "Đang lưu..." else "Lưu")
            }
            Text(when {
                state.loading -> "Đang chuẩn bị cài đặt"
                state.error != null -> "Cài đặt chưa được lưu"
                state.dirty -> "Có thay đổi chưa lưu. Quay lại sẽ bỏ các thay đổi này."
                else -> "Cài đặt đã được lưu."
            },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Lưu trữ trên thiết bị", style = MaterialTheme.typography.titleMedium)
            Text("Log đã sync giữ 24 giờ. Log chưa sync giữ tối đa 7 ngày. Kho sync giả lập vẫn nằm trên thiết bị.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
