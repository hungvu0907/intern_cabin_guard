package com.example.cabinguard.ui.widget

import com.example.cabinguard.data.repository.CabinTelemetryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Glance worker không phải @AndroidEntryPoint — lấy repository qua entry point. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CabinWidgetEntryPoint {
    fun telemetryRepository(): CabinTelemetryRepository
}
