package com.example.data.repository

import com.example.data.db.NoteDao
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<Note>> = noteDao.getTrashedNotes()

    fun getNote(id: Long): Flow<Note?> = noteDao.getNoteById(id)

    suspend fun getNoteOnce(id: Long): Note? = noteDao.getNoteByIdOnce(id)

    suspend fun getLatestActiveNote(): Note? = noteDao.getLatestActiveNote()

    suspend fun getLatestNote(): Note? = noteDao.getLatestNote()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun insert(note: Note): Long = noteDao.insert(note)

    suspend fun insertAll(notes: List<Note>): List<Long> = noteDao.insertAll(notes)

    suspend fun update(note: Note) = noteDao.update(note)

    suspend fun updateAll(notes: List<Note>) = noteDao.updateAll(notes)

    suspend fun delete(note: Note) = noteDao.delete(note)

    suspend fun deleteAll(notes: List<Note>) = noteDao.deleteAll(notes)

    suspend fun deleteById(id: Long) = noteDao.deleteById(id)

    suspend fun autoCleanTrash(olderThanTimestamp: Long): Int = noteDao.autoCleanTrash(olderThanTimestamp)

    suspend fun emptyTrash(): Int = noteDao.emptyTrash()
}
