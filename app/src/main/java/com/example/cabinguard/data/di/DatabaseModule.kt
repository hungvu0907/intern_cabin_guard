package com.example.cabinguard.data.di

import android.content.Context
import androidx.room.Room
import com.example.cabinguard.data.local.CabinDatabase
import com.example.cabinguard.data.local.CabinTelemetryDao
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
            CabinDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideDao(database: CabinDatabase): CabinTelemetryDao {
        return database.cabinTelemetryDao()
    }
}
