package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMappingDao {
    @Query("SELECT * FROM app_mappings ORDER BY slot ASC")
    fun getAllMappings(): Flow<List<AppMapping>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: AppMapping)

    @Query("DELETE FROM app_mappings WHERE slot = :slot")
    suspend fun deleteMapping(slot: Int)
}
