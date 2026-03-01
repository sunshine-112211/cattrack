package com.cattrack.app.di

import android.content.Context
import androidx.room.Room
import com.cattrack.app.data.local.AppDatabase
import com.cattrack.app.data.local.CatDao
import com.cattrack.app.data.local.HealthDataDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cattrack.db"
        ).build()

    @Provides
    fun provideCatDao(db: AppDatabase): CatDao = db.catDao()

    @Provides
    fun provideHealthDataDao(db: AppDatabase): HealthDataDao = db.healthDataDao()
}
