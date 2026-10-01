package com.example.cabinguard.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = true) { if (!state.saving) onBack() }
    LaunchedEffect(state.saved) {
        if (state.saved) { viewModel.consumeSaved(); onBack() }
    }
    SettingsContent(state, onBack, viewModel::temperatureChanged, viewModel::co2Changed, viewModel::save)
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onTemperatureChange: (String) -> Unit,
    onCo2Change: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0B1220)).safeDrawingPadding().imePadding()
        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, enabled = !state.saving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Quay lại") }
        Text("Ngưỡng cảnh báo", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text("Nhiệt độ 30–60 °C · CO₂ 500–3000 ppm", color = Color.White)
        if (state.loading) CircularProgressIndicator()
        OutlinedTextField(state.temperature, onTemperatureChange, Modifier.fillMaxWidth(), enabled = !state.loading && !state.saving,
            label = { Text("Nhiệt độ (°C)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(state.co2, onCo2Change, Modifier.fillMaxWidth(), enabled = !state.loading && !state.saving,
            label = { Text("CO₂ (ppm)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, enabled = !state.loading && !state.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (state.saving) "Đang lưu..." else "Lưu")
        }
        Text("Sync giả lập lưu trên thiết bị. Log đã sync giữ 24 giờ; mọi log quá 7 ngày sẽ bị xóa.", color = Color.White.copy(alpha = 0.7f))
    }
}
