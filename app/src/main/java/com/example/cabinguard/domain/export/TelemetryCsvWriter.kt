package com.example.cabinguard.domain.export

import com.example.cabinguard.data.model.CabinTelemetry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        val timeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone)
        out.append(HEADER).append('\n')
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
}
