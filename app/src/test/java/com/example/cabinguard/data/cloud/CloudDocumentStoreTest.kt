package com.example.cabinguard.data.cloud

import com.example.cabinguard.data.local.CabinTelemetry
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudDocumentStoreTest {

    @Test
    fun `upsert with the same id overwrites instead of duplicating`() {
        val documents = mutableMapOf<Long, CloudTelemetryDocument>()
        val first = CabinTelemetry(id = 9, temperature = 28f, co2Level = 500f)
        val updated = first.copy(temperature = 42f, isWarning = true)

        CloudDocumentStore.upsert(documents, listOf(first))
        CloudDocumentStore.upsert(documents, listOf(updated))

        assertEquals(1, documents.size)
        assertEquals(42f, documents.getValue(9).temperature)
        assertEquals(true, documents.getValue(9).isWarning)
    }
}
