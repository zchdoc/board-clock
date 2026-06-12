package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppMappingRepository

class BaseApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "board_clock_database"
        ).fallbackToDestructiveMigration().build()
    }

    val repository: AppMappingRepository by lazy {
        AppMappingRepository(database.appMappingDao())
    }
}
