package com.example.cabinguard.widget

/** Small boundary keeps Settings tests independent of Android and the launcher. */
interface WidgetRefresh {
    fun requestUpdate(warning: Boolean? = null, force: Boolean = false)
}
