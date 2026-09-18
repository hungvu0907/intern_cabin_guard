package com.example.cabinguard

import com.example.cabinguard.domain.sensor.CabinSensorEngine
import com.example.cabinguard.domain.sensor.CabinThresholds
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking

/**
 * Chạy thử CabinSensorEngine ngoài Android để kiểm tra nhanh logic cảm biến.
 *
 *   ./gradlew :cli:run --args="--battery-low"
 */
fun isBatteryLowMode(args: Array<String>): Boolean =
    args.contains("--battery-low")

fun main(args: Array<String>) = runBlocking {
    val engine = CabinSensorEngine()
    engine.setBatteryLow(isBatteryLowMode(args))

    println("CabinGuard CLI · chu kỳ quét ${engine.scanIntervalMillis}ms")
    println(
        "Ngưỡng cảnh báo: ${CabinThresholds.TEMPERATURE_WARNING_CELSIUS}°C / " +
            "${CabinThresholds.CO2_WARNING_PPM} ppm"
    )

    engine.observeTelemetry()
        .take(SAMPLE_COUNT)
        .collect { telemetry ->
            println(
                "[${telemetry.timestamp}] " +
                    "%.1f°C · %.0f hPa · %d ppm · %s".format(
                        telemetry.temperature,
                        telemetry.pressure,
                        telemetry.co2Level,
                        if (telemetry.isWarning) "CẢNH BÁO" else "AN TOÀN"
                    )
            )
        }
}

private const val SAMPLE_COUNT = 5
