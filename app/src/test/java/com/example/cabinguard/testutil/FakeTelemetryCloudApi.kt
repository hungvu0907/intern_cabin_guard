package com.example.cabinguard.testutil

import com.example.cabinguard.data.cloud.CloudDocumentStore
import com.example.cabinguard.data.cloud.CloudTelemetryDocument
import com.example.cabinguard.data.cloud.TelemetryCloudApi
import com.example.cabinguard.data.local.CabinTelemetry

/** Cloud giả: lỗi ném exception trước khi ghi; thành công thì upsert theo id. */
class FakeTelemetryCloudApi : TelemetryCloudApi {

    val stored = mutableMapOf<Long, CloudTelemetryDocument>()
    var failWith: Exception? = null
    var upsertCalls: Int = 0

    override suspend fun upsertAll(records: List<CabinTelemetry>) {
        upsertCalls++
        failWith?.let { throw it }
        CloudDocumentStore.upsert(stored, records)
    }
}
