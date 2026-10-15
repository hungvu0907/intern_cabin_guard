package com.example.cabinguard.data.export

import android.content.Context
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

    private fun sample(id: Long) = CabinTelemetry(
        id = id,
        timestamp = id,
        temperature = 36f,
        pressure = 1_000f,
        co2Level = 800f,
        isWarning = false,
    )
}
