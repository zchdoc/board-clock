package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_mappings")
data class AppMapping(
    @PrimaryKey val slot: Int, // 0 to 9
    val packageName: String,
    val appName: String,
    val password: String = ""
)
