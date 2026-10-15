package com.example.cabinguard.widget

import android.content.Context
import android.content.ComponentName
import android.appwidget.AppWidgetManager
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

/** Widget work is isolated from sensor persistence and conflated when busy. */
@Singleton
class CabinWidgetUpdater @Inject constructor(@param:ApplicationContext private val context: Context) : WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updates = WidgetUpdateLoop(
        scope = scope,
        now = SystemClock::elapsedRealtime,
        hasWidgets = {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, CabinWidgetReceiver::class.java)).isNotEmpty()
        },
        update = { CabinWidget().updateAll(context) },
        onFailure = { Log.w("CabinWidget", "Widget update failed; telemetry continues", it) }
    )

    override fun requestUpdate(warning: Boolean?, force: Boolean) {
        updates.requestUpdate(warning, force)
    }
}
