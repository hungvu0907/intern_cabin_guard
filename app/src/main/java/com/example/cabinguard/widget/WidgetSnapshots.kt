package com.example.cabinguard.widget

import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.domain.sensor.CabinWarningThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class WidgetSnapshot(val latest: CabinTelemetry?, val thresholds: CabinWarningThresholds) {
    val isWarning: Boolean get() = latest?.let { thresholds.isWarning(it.temperature, it.co2Level) } == true
}

/** Active Glance sessions also observe Room; throttle those recompositions. */
fun Flow<WidgetSnapshot>.throttleWidgetSnapshots(now: () -> Long): Flow<WidgetSnapshot> = flow {
    var previous: WidgetSnapshot? = null
    var lastEmission = 0L
    collect { current ->
        val time = now()
        val last = previous
        if (last == null || last.thresholds != current.thresholds || last.isWarning != current.isWarning ||
            (last.latest == null) != (current.latest == null) || time - lastEmission >= 30_000) {
            emit(current)
            previous = current
            lastEmission = time
        }
    }
}
