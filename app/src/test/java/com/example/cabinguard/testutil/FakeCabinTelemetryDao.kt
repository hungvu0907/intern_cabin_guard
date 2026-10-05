package com.example.cabinguard.testutil

import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.local.CabinTelemetryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** DAO trong RAM cho unit test sync — không cần Room. */
class FakeCabinTelemetryDao(
    initial: List<CabinTelemetry> = emptyList(),
) : CabinTelemetryDao {

    private val rows = MutableStateFlow(initial.toList())
    var failMarkSyncedAfterFirst: Exception? = null

    fun snapshot(): List<CabinTelemetry> = rows.value

    fun replace(telemetry: CabinTelemetry) {
        rows.value = rows.value.map { row -> if (row.id == telemetry.id) telemetry else row }
    }

    override suspend fun insert(telemetry: CabinTelemetry) {
        val without = rows.value.filterNot { it.id == telemetry.id }
        rows.value = without + telemetry
    }

    override fun getAllLogs(): Flow<List<CabinTelemetry>> =
        rows.asStateFlow().map { list -> list.sortedByDescending { it.timestamp } }

    override fun getRecentLogs(limit: Int): Flow<List<CabinTelemetry>> =
        getAllLogs().map { it.take(limit) }

    override fun observeLatest(): Flow<CabinTelemetry?> =
        getAllLogs().map { it.firstOrNull() }

    override suspend fun getLogsPage(limit: Int, offset: Int): List<CabinTelemetry> =
        rows.value.sortedByDescending { it.timestamp }.drop(offset).take(limit)

    override suspend fun deleteSyncedOlderThan(timestamp: Long) {
        rows.value = rows.value.filterNot { it.isSynced && it.timestamp < timestamp }
    }

    override suspend fun deleteOlderThanHardLimit(timestamp: Long) {
        rows.value = rows.value.filterNot { it.timestamp < timestamp }
    }

    override fun getTotalCount(): Flow<Int> = rows.asStateFlow().map { it.size }

    override suspend fun clearAll() {
        rows.value = emptyList()
    }

    override suspend fun getUnsynced(limit: Int): List<CabinTelemetry> =
        rows.value.filter { !it.isSynced }.sortedBy { it.timestamp }.take(limit)

    override suspend fun markSynced(ids: List<Long>) {
        failMarkSyncedAfterFirst?.let { failure ->
            val firstId = ids.firstOrNull()
            rows.value = rows.value.map { row ->
                if (row.id == firstId) row.copy(isSynced = true) else row
            }
            throw failure
        }
        val idSet = ids.toSet()
        rows.value = rows.value.map { row ->
            if (row.id in idSet) row.copy(isSynced = true) else row
        }
    }
}
