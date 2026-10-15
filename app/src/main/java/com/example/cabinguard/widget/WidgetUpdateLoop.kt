package com.example.cabinguard.widget

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Launcher work runs separately from recording and keeps urgent requests when busy. */
internal class WidgetUpdateLoop(
    scope: CoroutineScope,
    private val now: () -> Long,
    private val hasWidgets: () -> Boolean,
    private val update: suspend () -> Unit,
    private val onFailure: (Exception) -> Unit
) : WidgetRefresh {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val urgentPending = AtomicBoolean(false)
    private val policy = WidgetUpdatePolicy()

    init {
        scope.launch {
            for (request in requests) {
                try {
                    val time = now()
                    val force = urgentPending.getAndSet(false)
                    if (force || policy.isDue(time)) {
                        if (hasWidgets()) update()
                        // Also throttle installation checks when no widget is present.
                        policy.updated(time)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    policy.failed()
                    onFailure(error)
                }
            }
        }
    }

    @Synchronized
    override fun requestUpdate(warning: Boolean?, force: Boolean) {
        val urgent = force || (warning != null && policy.warningChanged(warning))
        if (urgent) urgentPending.set(true)
        if (urgent || policy.isDue(now())) requests.trySend(Unit)
    }
}
