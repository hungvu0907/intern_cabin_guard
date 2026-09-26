package com.example.cabinguard.domain.export

import com.example.cabinguard.data.local.CabinTelemetry

/**
 * [EXP-01] Sinh CSV từ danh sách log (header + từng dòng).
 * Cột khớp entity Room để file chia sẻ dùng làm bằng chứng sự cố.
 */
object TelemetryCsvFormatter {

    const val HEADER = "id,timestamp,temperature,pressure,co2_level,is_warning,is_synced"

    fun format(logs: List<CabinTelemetry>): String {
        if (logs.isEmpty()) return HEADER
        val rows = logs.joinToString(separator = "\n") { log ->
            listOf(
                log.id.toString(),
                log.timestamp.toString(),
                log.temperature.toString(),
                log.pressure.toString(),
                log.co2Level.toString(),
                log.isWarning.toString(),
                log.isSynced.toString(),
            ).joinToString(separator = ",") { cell(it) }
        }
        return "$HEADER\n$rows"
    }

    /** Bọc ô nếu giá trị có dấu phẩy / ngoặc kép / xuống dòng. */
    private fun cell(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
