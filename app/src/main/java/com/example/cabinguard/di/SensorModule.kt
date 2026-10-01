package com.example.cabinguard.di

import com.example.cabinguard.domain.sensor.CabinSensorEngine
import com.example.cabinguard.widget.CabinWidgetUpdater
import com.example.cabinguard.widget.WidgetRefresh
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {
    @Provides @Singleton fun engine() = CabinSensorEngine()
    @Provides fun widgetRefresh(updater: CabinWidgetUpdater): WidgetRefresh = updater
}
