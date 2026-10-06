package com.example.cabinguard.widget

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

/** Widget work is isolated from sensor persistence and conflated when busy. */
@Singleton
class CabinWidgetUpdater @Inject constructor(@param:ApplicationContext private val context: Context) : WidgetRefresh {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val urgentPending = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val policy = WidgetUpdatePolicy()

    init {
        scope.launch {
            for (request in requests) {
                try {
                    val now = SystemClock.elapsedRealtime()
                    val force = urgentPending.getAndSet(false)
                    if (force || policy.isDue(now)) {
                        CabinWidget().updateAll(context)
                        policy.updated(now)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    policy.failed()
                    Log.w("CabinWidget", "Widget update failed; telemetry continues", error)
                }
            }
        }
    }

    @Synchronized
    override fun requestUpdate(warning: Boolean?, force: Boolean) {
        // Force requests must survive later ordinary samples in the conflated channel.
        val urgent = force || (warning != null && policy.warningChanged(warning))
        if (urgent) urgentPending.set(true)
        if (urgent || policy.isDue(SystemClock.elapsedRealtime())) requests.trySend(Unit)
    }
}
