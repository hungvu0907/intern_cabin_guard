package com.example.cabinguard.data.cloud

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cabinguard.data.local.CabinTelemetry
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MockRestTelemetryCloudApiTest {
    private lateinit var directory: File
    private lateinit var api: MockRestTelemetryCloudApi

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        directory = File(context.cacheDir, "mock-cloud-api-test").apply {
            deleteRecursively()
            mkdirs()
        }
        api = MockRestTelemetryCloudApi(directory, "device-A")
    }

    @Test
    fun repeatedUpsertIsIdempotentAndIncludesDeviceId() = runTest {
        api.upsertAll(listOf(sample(id = 5, temperature = 30f)))
        api.upsertAll(listOf(sample(id = 5, temperature = 41f)))

        val documents = directory.listFiles { file -> file.extension == "json" }.orEmpty()
        assertEquals(1, documents.size)
        val json = JSONObject(documents.single().readText())
        assertEquals("device-A", json.getString("deviceId"))
        assertEquals(5L, json.getLong("id"))
        assertEquals(41.0, json.getDouble("temperature"), 0.0)
    }

    @Test
    fun corruptDocumentIsQuarantinedAndHealed() = runTest {
        api.documentFile(9).writeText("not-json")

        api.upsertAll(listOf(sample(id = 9, temperature = 36f)))

        assertEquals(9L, JSONObject(api.documentFile(9).readText()).getLong("id"))
        assertTrue(directory.listFiles().orEmpty().any { ".corrupt-" in it.name })
    }

    @Test
    fun documentPreservesIndependentWarningFlags() = runTest {
        api.upsertAll(listOf(sample(id = 11, temperature = 30f).copy(
            isWarning = true,
            isTempWarning = false,
            isCo2Warning = true,
        )))

        val json = JSONObject(api.documentFile(11).readText())
        assertEquals(false, json.getBoolean("isTempWarning"))
        assertEquals(true, json.getBoolean("isCo2Warning"))
    }

    @Test
    fun documentCountIsCappedAndUnrelatedFilesAreUntouched() = runTest {
        api = MockRestTelemetryCloudApi(directory, "device-A", maxDocuments = 3)
        api.upsertAll((1L..3L).map { sample(it, 30f) })
        val now = System.currentTimeMillis()
        assertTrue(api.documentFile(1).setLastModified(now - 30_000L))
        assertTrue(api.documentFile(2).setLastModified(now - 20_000L))
        assertTrue(api.documentFile(3).setLastModified(now - 10_000L))
        val unrelated = File(directory, "notes.txt").apply { writeText("keep") }

        api.upsertAll(listOf(sample(4, 31f)))

        assertEquals(3, directory.listFiles { file -> file.extension == "json" }.orEmpty().size)
        assertTrue(!api.documentFile(1).exists())
        assertTrue(api.documentFile(4).exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun corruptBackupsAreCapped() = runTest {
        api = MockRestTelemetryCloudApi(directory, "device-A", maxCorruptBackups = 2)
        val now = System.currentTimeMillis()
        val backups = (1L..3L).map { number ->
            File(directory, "device-A_9.json.corrupt-$number").apply {
                writeText("broken-$number")
                assertTrue(setLastModified(now - (4 - number) * 10_000L))
            }
        }

        api.upsertAll(listOf(sample(10, 30f)))

        assertTrue(!backups[0].exists())
        assertTrue(backups[1].exists())
        assertTrue(backups[2].exists())
        assertEquals(2, directory.listFiles { file -> ".corrupt-" in file.name }.orEmpty().size)
    }

    private fun sample(id: Long, temperature: Float) = CabinTelemetry(
        id = id,
        timestamp = 1_700_000_000_000L + id,
        temperature = temperature,
        pressure = 1_000f,
        co2Level = 800f,
        isWarning = false,
    )
}
