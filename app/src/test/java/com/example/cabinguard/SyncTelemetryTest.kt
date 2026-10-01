package com.example.cabinguard

import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.model.CabinTelemetry
import com.example.cabinguard.data.remote.TelemetryRemoteDataSource
import com.example.cabinguard.domain.sync.SyncTelemetryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SyncTelemetryTest {
    private class Logs(size: Int) : CabinTelemetryDao {
        val rows = (1..size).map { CabinTelemetry(it.toLong(), it.toLong(), 38.0, 1000.0, 1000, false) }.toMutableList()
        var failMark = false
        var noProgress = false
        override suspend fun insert(telemetry: CabinTelemetry) { rows += telemetry }
        override fun observeAll() = flowOf(rows.toList())
        override fun observeLatest() = flowOf(rows.lastOrNull())
        override fun observeCount() = flowOf(rows.size)
        override fun observeUnsyncedCount() = flowOf(rows.count { !it.isSynced })
        override suspend fun getAllLogs() = rows.toList()
        override suspend fun getUnsynced(limit: Int) = rows.filter { !it.isSynced }.take(limit)
        override suspend fun markSynced(ids: List<Long>): Int {
            if (failMark) throw IllegalStateException("mark failed")
            if (noProgress) return 0
            var changed = 0
            rows.replaceAll { if (it.id in ids && !it.isSynced) { changed++; it.copy(isSynced = true) } else it }
            return changed
        }
        override suspend fun countUnsyncedOlderThan(cutoff: Long) = rows.count { !it.isSynced && it.timestamp < cutoff }
        override suspend fun deleteExpired(syncedCutoff: Long, hardCutoff: Long): Int = error("Not used")
    }
    private class Remote : TelemetryRemoteDataSource {
        val documents = mutableMapOf<Long, CabinTelemetry>()
        var calls = 0
        var failOnCall = -1
        var cancel = false
        override suspend fun upsert(records: List<CabinTelemetry>) {
            calls++
            if (cancel) throw CancellationException("cancel")
            if (calls == failOnCall) throw IllegalStateException("upload failed")
            records.forEach { documents[it.id] = it }
        }
    }

    @Test fun batchesAndRetryAfterPartialUpload() = runTest {
        val logs = Logs(1001)
        val remote = Remote().apply { failOnCall = 2 }
        val sync = SyncTelemetryUseCase(logs, remote)
        try { sync(); fail("Expected upload failure") } catch (_: IllegalStateException) { }
        assertEquals(500, logs.rows.count { it.isSynced })
        remote.failOnCall = -1
        assertEquals(501, sync())
        assertEquals(1001, remote.documents.size)
        assertEquals(0, sync())
    }
    @Test fun retryAfterRemoteCommitBeforeLocalMarkDoesNotDuplicate() = runTest {
        val logs = Logs(3).apply { failMark = true }
        val remote = Remote()
        val sync = SyncTelemetryUseCase(logs, remote)
        try { sync(); fail("Expected mark failure") } catch (_: IllegalStateException) { }
        assertTrue(logs.rows.none { it.isSynced })
        logs.failMark = false
        assertEquals(3, sync())
        assertEquals(3, remote.documents.size)
    }
    @Test fun noProgressFailsAndCancellationPropagates() = runTest {
        val logs = Logs(1).apply { noProgress = true }
        val remote = Remote()
        try { SyncTelemetryUseCase(logs, remote)(); fail("Expected no-progress failure") } catch (_: IllegalStateException) { }
        assertEquals(1, remote.calls)
        remote.cancel = true
        try { SyncTelemetryUseCase(logs, remote)(); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertTrue(logs.rows.none { it.isSynced })
    }
}
