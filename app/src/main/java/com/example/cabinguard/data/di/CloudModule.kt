package com.example.cabinguard.data.di

import com.example.cabinguard.data.cloud.MockRestTelemetryCloudApi
import com.example.cabinguard.data.cloud.TelemetryCloudApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudModule {

    @Binds
    @Singleton
    abstract fun bindTelemetryCloudApi(impl: MockRestTelemetryCloudApi): TelemetryCloudApi
}
