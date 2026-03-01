package com.cattrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cattrack.app.data.model.ActivityData
import com.cattrack.app.data.model.Cat
import com.cattrack.app.data.model.HealthData

@Database(
    entities = [
        Cat::class,
        HealthData::class,
        ActivityData::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao
    abstract fun healthDataDao(): HealthDataDao
}
