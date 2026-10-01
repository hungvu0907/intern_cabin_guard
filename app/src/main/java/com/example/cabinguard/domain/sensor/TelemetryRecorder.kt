package com.example.cabinguard.domain.sensor

import com.example.cabinguard.data.local.CabinTelemetryDao

/** One instance per collection: reevaluation changes alerts, not measurement history. */
class TelemetryRecorder(private val dao: CabinTelemetryDao) {
    private var lastSequence = -1L
    suspend fun record(sample: EvaluatedSample) {
        if (sample.sequence != lastSequence) {
            dao.insert(sample.telemetry)
            lastSequence = sample.sequence
        }
    }
}
