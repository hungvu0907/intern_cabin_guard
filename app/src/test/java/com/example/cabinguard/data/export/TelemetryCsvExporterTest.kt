package com.example.cabinguard.data.export

import android.content.Context
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.atomic.AtomicInteger

class TelemetryCsvExporterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `exports pages directly to a file`() = runTest {
        val context = mockk<Context>()
        val repository = mockk<CabinTelemetryRepository>()
        every { context.cacheDir } returns temporaryFolder.root
        coEvery { repository.getMaxLogId() } returns 2L
        coEvery { repository.getLogsPage(2L, null, Long.MAX_VALUE, 500) } returns
            listOf(sample(2), sample(1))
        coEvery { repository.getLogsPage(2L, 1L, 1L, 500) } returns emptyList()

        val file = TelemetryCsvExporter(context, repository).export()

        assertNotNull(file)
        assertEquals(3, file!!.readLines().size)
        assertEquals("2", file.readLines()[1].substringBefore(','))
        coVerify(exactly = 1) { repository.getMaxLogId() }
        coVerify(exactly = 1) { repository.getLogsPage(2L, null, Long.MAX_VALUE, 500) }
        coVerify(exactly = 1) { repository.getLogsPage(2L, 1L, 1L, 500) }
    }

    @Test
    fun `two callers cannot write the shared CSV file at the same time`() = runBlocking {
        val context = mockk<Context>()
        val repository = mockk<CabinTelemetryRepository>()
        val exporter = TelemetryCsvExporter(context, repository)
        val firstReadStarted = CompletableDeferred<Unit>()
        val releaseFirstRead = CompletableDeferred<Unit>()
        val secondReadStarted = CompletableDeferred<Unit>()
        val secondCallerStarted = CompletableDeferred<Unit>()
        val reads = AtomicInteger()
        every { context.cacheDir } returns temporaryFolder.root
        coEvery { repository.getMaxLogId() } coAnswers {
            if (reads.incrementAndGet() == 1) {
                firstReadStarted.complete(Unit)
                releaseFirstRead.await()
            } else {
                secondReadStarted.complete(Unit)
            }
            1L
        }
        coEvery { repository.getLogsPage(1L, null, Long.MAX_VALUE, 500) } returns listOf(sample(1))
        coEvery { repository.getLogsPage(1L, 1L, 1L, 500) } returns emptyList()

        val first = async(Dispatchers.Default) { exporter.export() }
        withTimeout(5_000) { firstReadStarted.await() }
        val second = async(Dispatchers.Default) {
            secondCallerStarted.complete(Unit)
            exporter.export()
        }
        secondCallerStarted.await()
        try {
            assertNull(withTimeoutOrNull(200) { secondReadStarted.await() })
        } finally {
            releaseFirstRead.complete(Unit)
        }

        assertNotNull(withTimeout(5_000) { first.await() })
        assertNotNull(withTimeout(5_000) { second.await() })
        assertEquals(2, reads.get())
    }

    private fun sample(id: Long) = CabinTelemetry(
        id = id,
        timestamp = id,
        temperature = 36f,
        pressure = 1_000f,
        co2Level = 800f,
        isWarning = false,
    )
}
