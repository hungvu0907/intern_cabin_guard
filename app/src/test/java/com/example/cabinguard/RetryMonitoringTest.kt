package com.example.cabinguard

import com.example.cabinguard.domain.sensor.retryMonitoring
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RetryMonitoringTest {
    @Test fun failedPersistenceReportsAndRetriesAfterBackoff() = runTest {
        var attempts = 0
        val failures = mutableListOf<Exception>()
        var recorded = false
        retryMonitoring(attempt = {
            attempts++
            if (attempts == 1) throw java.io.IOException("disk busy")
            recorded = true
        }, onFailure = { failures += it })
        assertTrue(recorded)
        assertEquals(2, attempts)
        assertEquals(1, failures.size)
        assertEquals(5_000L, testScheduler.currentTime)
    }
    @Test fun cancellationNeverRetriesOrReportsAStorageError() = runTest {
        var attempts = 0
        var failures = 0
        try {
            retryMonitoring(attempt = { attempts++; throw CancellationException("service destroyed") }, onFailure = { failures++ })
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
        assertEquals(1, attempts)
        assertEquals(0, failures)
    }
}
