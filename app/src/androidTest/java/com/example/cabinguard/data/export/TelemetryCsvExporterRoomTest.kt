package com.example.cabinguard.data.export

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cabinguard.data.cloud.TelemetryCloudApi
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.local.CabinTelemetry
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.repository.CabinTelemetryRepository
import com.example.cabinguard.domain.export.TelemetryCsvFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TelemetryCsvExporterRoomTest {

    @Test
    fun insertingDuringExportDoesNotDuplicateRowsOrIncludeNewRow() = runTest {
        val ids = exportWhileDatabaseChanges { _, dao ->
            dao.insert(sample(502))
        }

        assertEquals((501L downTo 1L).toList(), ids)
    }

    @Test
    fun deletingAnExportedRowDoesNotSkipTheNextPage() = runTest {
        val ids = exportWhileDatabaseChanges { database, _ ->
            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM cabin_telemetry WHERE id = 501"
            )
        }

        assertEquals((501L downTo 1L).toList(), ids)
    }

    private suspend fun exportWhileDatabaseChanges(
        changeAfterFirstPage: suspend (CabinDatabase, CabinTelemetryDao) -> Unit,
    ): List<Long> {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(appContext, CabinDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val testCache = File(appContext.cacheDir, "csv-keyset-${UUID.randomUUID()}")
        try {
            check(testCache.mkdirs())
            val roomDao = database.cabinTelemetryDao()
            (1L..501L).forEach { id -> roomDao.insert(sample(id)) }
            var firstPage = true
            val mutatingDao = object : CabinTelemetryDao by roomDao {
                override suspend fun getLogsPage(
                    maxId: Long,
                    beforeTimestamp: Long?,
                    beforeId: Long,
                    limit: Int,
                ): List<CabinTelemetry> {
                    val page = roomDao.getLogsPage(maxId, beforeTimestamp, beforeId, limit)
                    if (firstPage) {
                        firstPage = false
                        assertEquals(500, page.size)
                        changeAfterFirstPage(database, roomDao)
                    }
                    return page
                }
            }
            val cloud = object : TelemetryCloudApi {
                override suspend fun upsertAll(records: List<CabinTelemetry>) = Unit
            }
            val repository = CabinTelemetryRepository(mutatingDao, cloud)
            val exportContext = object : ContextWrapper(appContext) {
                override fun getCacheDir(): File = testCache
            }

            val lines = TelemetryCsvExporter(exportContext, repository).export()!!.readLines()

            assertEquals(TelemetryCsvFormatter.HEADER, lines.first())
            return lines.drop(1).map { it.substringBefore(',').toLong() }
        } finally {
            database.close()
            testCache.deleteRecursively()
        }
    }

    private fun sample(id: Long) = CabinTelemetry(
        id = id,
        timestamp = 100L, // Cùng timestamp để bắt lỗi keyset chỉ dựa vào timestamp.
        temperature = 36f,
        pressure = 1_000f,
        co2Level = 800f,
        isWarning = false,
    )
}
