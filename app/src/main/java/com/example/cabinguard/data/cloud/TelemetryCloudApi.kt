package com.example.cabinguard.data.cloud

import com.example.cabinguard.data.local.CabinTelemetry

/**
 * Cổng đẩy telemetry lên cloud.
 * Document id = [CabinTelemetry.id] local để ghi đè, không tạo bản ghi trùng.
 */
interface TelemetryCloudApi {
    suspend fun upsertAll(records: List<CabinTelemetry>)
}
