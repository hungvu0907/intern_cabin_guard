package com.example.cabinguard.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.cabinguard.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Nền cảnh báo trùng Dashboard phone (`0xFFFFEBEE` / `0xFFC62828`). */
private val WarningBackground = Color(0xFFFFEBEE)
private val WarningContent = Color(0xFFC62828)
private val NormalBackground = Color(0xFFFFFBFE)
private val NormalContent = Color(0xFF1C1B1F)

class CabinHomeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CabinWidgetEntryPoint::class.java,
        ).telemetryRepository()
        val latestFlow = repository.observeLatest()
        // provideGlance chạy trên main — đọc Room ở IO trước khi vẽ.
        val initial = withContext(Dispatchers.IO) { latestFlow.first() }

        provideContent {
            val latest by latestFlow.collectAsState(initial = initial)
            CabinWidgetContent(latest.toCabinWidgetUi())
        }
    }
}

@Composable
private fun CabinWidgetContent(ui: CabinWidgetUi) {
    val background = if (ui.isWarning) WarningBackground else NormalBackground
    val content = if (ui.isWarning) WarningContent else NormalContent

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(background))
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "CabinGuard",
            style = TextStyle(
                color = ColorProvider(content),
                fontSize = 12.sp,
            ),
        )
        Text(
            text = ui.temperatureText,
            style = TextStyle(
                color = ColorProvider(content),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = ui.statusText,
            style = TextStyle(
                color = ColorProvider(content),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
