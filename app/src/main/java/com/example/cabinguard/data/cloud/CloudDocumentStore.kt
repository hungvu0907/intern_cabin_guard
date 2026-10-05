package com.example.cabinguard.data.cloud

import com.example.cabinguard.data.local.CabinTelemetry

/** Payload cloud — khóa là id local, không mang cờ isSynced. */
data class CloudTelemetryDocument(
    val id: Long,
    val timestamp: Long,
    val temperature: Float,
    val pressure: Float,
    val co2Level: Float,
    val isWarning: Boolean,
)

fun CabinTelemetry.toCloudDocument(): CloudTelemetryDocument = CloudTelemetryDocument(
    id = id,
    timestamp = timestamp,
    temperature = temperature,
    pressure = pressure,
    co2Level = co2Level,
    isWarning = isWarning,
)

/** Gộp theo id: cùng id thì ghi đè, không thêm document mới. */
object CloudDocumentStore {
    fun upsert(
        documents: MutableMap<Long, CloudTelemetryDocument>,
        incoming: List<CabinTelemetry>,
    ) {
        incoming.forEach { record ->
            documents[record.id] = record.toCloudDocument()
        }
    }
}
