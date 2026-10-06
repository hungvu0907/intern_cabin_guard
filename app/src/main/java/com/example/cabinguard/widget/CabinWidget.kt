package com.example.cabinguard.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.example.cabinguard.MainActivity
import com.example.cabinguard.ui.theme.*
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetDependencies {
    fun telemetryDao(): CabinTelemetryDao
    fun settings(): ThresholdSettingsRepository
}

class CabinWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dependencies = EntryPointAccessors.fromApplication(context, WidgetDependencies::class.java)
        val data = combine(dependencies.telemetryDao().observeLatest(), dependencies.settings().thresholds) { row, limits ->
            WidgetSnapshot(row, limits)
        }.throttleWidgetSnapshots(android.os.SystemClock::elapsedRealtime)
        val initial = data.first()
        val locale = context.resources.configuration.locales[0]
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", locale).withZone(ZoneId.systemDefault())
        provideContent {
            val snapshot by data.collectAsState(initial)
            val latest = snapshot.latest
            val warning = snapshot.isWarning
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .background(if (warning) CabinDangerInk else CabinInk)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(16.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text("CabinGuard", style = TextStyle(color = ColorProvider(CabinOnInk), fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Text(if (latest == null) "Đang chờ dữ liệu" else if (warning) "CẢNH BÁO" else "Cabin an toàn",
                    style = TextStyle(color = ColorProvider(if (latest == null) CabinMuted else if (warning) Color(0xFFFFB4AB) else CabinSafe)))
                latest?.let { row ->
                    Text(String.format(locale, "%.1f °C · %d ppm", row.temperature, row.co2Level),
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp))
                    Text("Đo lúc " + formatter.format(Instant.ofEpochMilli(row.timestamp)),
                        style = TextStyle(color = ColorProvider(Color.White)))
                }
            }
        }
    }
}

class CabinWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CabinWidget()
}
