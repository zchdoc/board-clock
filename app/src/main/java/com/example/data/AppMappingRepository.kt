package com.example.data

import kotlinx.coroutines.flow.Flow

class AppMappingRepository(private val dao: AppMappingDao) {
    val allMappings: Flow<List<AppMapping>> = dao.getAllMappings()

    suspend fun insertMapping(mapping: AppMapping) {
        dao.insertMapping(mapping)
    }

    suspend fun deleteMapping(slot: Int) {
        dao.deleteMapping(slot)
    }
}
