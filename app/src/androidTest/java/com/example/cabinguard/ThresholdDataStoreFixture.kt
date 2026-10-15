package com.example.cabinguard

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cabinguard.data.settings.ThresholdSettingsRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking

/** Each test owns a file and stops the old DataStore before opening the same file again. */
internal class ThresholdDataStoreFixture(context: Context) {
    private val file = File(context.cacheDir, "thresholds-${UUID.randomUUID()}.preferences_pb")
    private var job = SupervisorJob()
    var repository: ThresholdSettingsRepository = open()
        private set

    private fun open() = ThresholdSettingsRepository(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO),
            produceFile = { file }
        )
    )

    fun reopen() {
        runBlocking { job.cancelAndJoin() }
        job = SupervisorJob()
        repository = open()
    }

    fun close() {
        runBlocking { job.cancelAndJoin() }
        check(!file.exists() || file.delete()) { "Could not remove test preferences" }
    }
}
