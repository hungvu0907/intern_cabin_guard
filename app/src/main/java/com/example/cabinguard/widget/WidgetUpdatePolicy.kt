package com.example.cabinguard.widget

class WidgetUpdatePolicy {
    private var lastUpdate: Long? = null
    private var lastWarning: Boolean? = null
    @Synchronized fun isDue(now: Long): Boolean = lastUpdate?.let { now - it >= 30_000 } ?: true
    @Synchronized fun updated(now: Long) { lastUpdate = now }
    @Synchronized fun warningChanged(warning: Boolean): Boolean {
        val changed = warning != lastWarning
        lastWarning = warning
        return changed
    }
}
