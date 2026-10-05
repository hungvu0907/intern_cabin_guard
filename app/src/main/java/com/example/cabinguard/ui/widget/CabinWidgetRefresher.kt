package com.example.cabinguard.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [WDG-05] Service (hoặc ViewModel khi service tắt) ghi Room → Flow phát
 * → đánh thức session Glance. updateAll không khởi động lại provideGlance
 * nếu session còn sống; collectAsState trong widget nhận bản ghi mới.
 */
@Singleton
@OptIn(FlowPreview::class)
class CabinWidgetRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: CabinTelemetryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            repository.observeLatest()
                .sampleWidgetUpdates()
                .collect {
                    val installedWidgets = GlanceAppWidgetManager(context)
                        .getGlanceIds(CabinHomeWidget::class.java)
                    if (installedWidgets.isNotEmpty()) {
                        CabinHomeWidget().updateAll(context)
                    }
                }
        }
    }
}

internal const val WIDGET_REFRESH_INTERVAL_MS = 5_000L

@OptIn(FlowPreview::class)
internal fun <T> Flow<T>.sampleWidgetUpdates(
    intervalMillis: Long = WIDGET_REFRESH_INTERVAL_MS,
): Flow<T> = sample(intervalMillis)
