package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SpokenCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface SpokenCommandDao {
    @Query("SELECT * FROM spoken_commands WHERE isEnabled = 1")
    fun getAllActiveCommands(): Flow<List<SpokenCommand>>

    @Query("SELECT * FROM spoken_commands WHERE isEnabled = 1 AND (language = :lang OR language = 'all')")
    suspend fun getCommandsForLanguage(lang: String): List<SpokenCommand>

    @Query("SELECT * FROM spoken_commands")
    fun getAllCommands(): Flow<List<SpokenCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<SpokenCommand>)

    @Update
    suspend fun update(command: SpokenCommand)

    @Delete
    suspend fun delete(command: SpokenCommand)

    @Query("SELECT * FROM spoken_commands")
    suspend fun getAllCommandsList(): List<SpokenCommand>

    @Query("DELETE FROM spoken_commands")
    suspend fun deleteAllCommands(): Int

    @Query("SELECT COUNT(*) FROM spoken_commands")
    suspend fun getCount(): Int
}
