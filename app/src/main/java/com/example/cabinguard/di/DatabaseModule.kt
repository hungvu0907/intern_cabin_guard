package com.example.cabinguard.di

import android.content.Context
import androidx.room.Room
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.local.CabinTelemetryDao
import com.example.cabinguard.data.local.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CabinDatabase {
        return Room.databaseBuilder(
            context,
            CabinDatabase::class.java,
            "cabin_guard.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideTelemetryDao(
        database: CabinDatabase
    ): CabinTelemetryDao {
        return database.telemetryDao()
    }
}
