package com.example.data.repository

import com.example.data.db.SpokenCommandDao
import com.example.data.model.SpokenCommand
import kotlinx.coroutines.flow.Flow

class SpokenCommandRepository(private val spokenCommandDao: SpokenCommandDao) {
    val allActiveCommands: Flow<List<SpokenCommand>> = spokenCommandDao.getAllActiveCommands()
    val allCommands: Flow<List<SpokenCommand>> = spokenCommandDao.getAllCommands()

    suspend fun getCommandsForLanguage(lang: String): List<SpokenCommand> =
        spokenCommandDao.getCommandsForLanguage(lang)

    suspend fun insertAll(commands: List<SpokenCommand>) = spokenCommandDao.insertAll(commands)

    suspend fun update(command: SpokenCommand) = spokenCommandDao.update(command)

    suspend fun delete(command: SpokenCommand) = spokenCommandDao.delete(command)
    suspend fun getAllCommandsList(): List<SpokenCommand> = spokenCommandDao.getAllCommandsList()
    suspend fun deleteAllCommands(): Int = spokenCommandDao.deleteAllCommands()
}
