package com.example.cabinguard.domain.export

import com.example.cabinguard.data.model.CabinTelemetry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Ghi lịch sử log ra CSV. Thuần Kotlin để unit test không cần Android.
 * Ghi thẳng vào [Appendable] theo từng dòng, không dựng cả file thành một
 * String vì 24h log có thể lên tới ~86.400 dòng.
 */
object TelemetryCsvWriter {

    const val HEADER =
        "id,timestamp_iso,temperature_c,pressure_hpa,co2_ppm,is_warning,is_synced"

    fun write(
        rows: Iterable<CabinTelemetry>,
        out: Appendable,
        zone: ZoneId = ZoneId.systemDefault(),
        includeHeader: Boolean = true
    ) {
        val timeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone)
        if (includeHeader) out.append(HEADER).append('\n')
        rows.forEach { row ->
            out.append(row.id.toString()).append(',')
                .append(timeFormatter.format(Instant.ofEpochMilli(row.timestamp))).append(',')
                // Locale.US: locale Việt Nam in "38,50" làm lệch cột CSV.
                .append(String.format(Locale.US, "%.2f", row.temperature)).append(',')
                .append(String.format(Locale.US, "%.2f", row.pressure)).append(',')
                .append(row.co2Level.toString()).append(',')
                .append(row.isWarning.toString()).append(',')
                .append(row.isSynced.toString()).append('\n')
        }
    }

    /** Query and release one bounded batch at a time; writes one header even if empty. */
    suspend fun writeBatches(
        out: Appendable,
        zone: ZoneId = ZoneId.systemDefault(),
        loadBatch: suspend (last: CabinTelemetry?) -> List<CabinTelemetry>
    ): Int {
        out.append(HEADER).append('\n')
        var last: CabinTelemetry? = null
        var written = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val batch = loadBatch(last)
            if (batch.isEmpty()) return written
            val next = batch.last()
            check(last == null || next.timestamp > last.timestamp ||
                (next.timestamp == last.timestamp && next.id > last.id)) { "CSV pagination made no progress" }
            write(batch, out, zone, includeHeader = false)
            written += batch.size
            last = next
        }
    }
}
