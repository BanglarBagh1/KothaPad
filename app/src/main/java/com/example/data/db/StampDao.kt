package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Stamp
import kotlinx.coroutines.flow.Flow

@Dao
interface StampDao {
    @Query("SELECT * FROM stamps ORDER BY isPinned DESC, orderIndex ASC")
    fun getAllStamps(): Flow<List<Stamp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stamp: Stamp): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stamps: List<Stamp>)

    @Update
    suspend fun update(stamp: Stamp)

    @Update
    suspend fun updateAll(stamps: List<Stamp>)

    @Delete
    suspend fun delete(stamp: Stamp)

    @Query("SELECT COUNT(*) FROM stamps")
    suspend fun getCount(): Int
}
