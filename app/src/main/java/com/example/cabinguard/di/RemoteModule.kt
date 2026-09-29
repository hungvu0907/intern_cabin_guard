package com.example.cabinguard.di

import com.example.cabinguard.data.remote.MockTelemetryRemote
import com.example.cabinguard.data.remote.TelemetryRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    // Đổi sang FirestoreTelemetryRemote khi dự án có Firebase.
    @Binds
    abstract fun bindTelemetryRemote(
        remote: MockTelemetryRemote
    ): TelemetryRemoteDataSource
}
