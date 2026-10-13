package com.example.data.repository

import com.example.data.db.StampDao
import com.example.data.model.Stamp
import kotlinx.coroutines.flow.Flow

class StampRepository(private val stampDao: StampDao) {
    val allStamps: Flow<List<Stamp>> = stampDao.getAllStamps()

    suspend fun insert(stamp: Stamp): Long = stampDao.insert(stamp)

    suspend fun insertAll(stamps: List<Stamp>) = stampDao.insertAll(stamps)

    suspend fun update(stamp: Stamp) = stampDao.update(stamp)

    suspend fun updateAll(stamps: List<Stamp>) = stampDao.updateAll(stamps)

    suspend fun delete(stamp: Stamp) = stampDao.delete(stamp)
    suspend fun getAllStampsList(): List<Stamp> = stampDao.getAllStampsList()
    suspend fun deleteAllStamps(): Int = stampDao.deleteAllStamps()
}
