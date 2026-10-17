package com.example.data.repository

import com.example.data.db.NoteDao
import com.example.data.model.Note
import com.example.data.vault.VaultNoteRepository
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    var vaultNoteRepository: VaultNoteRepository? = null
) {
    val allNotes: Flow<List<Note>>
        get() = vaultNoteRepository?.allNotes ?: noteDao.getAllNotes()

    val activeNotes: Flow<List<Note>>
        get() = vaultNoteRepository?.activeNotes ?: noteDao.getActiveNotes()

    val archivedNotes: Flow<List<Note>>
        get() = vaultNoteRepository?.archivedNotes ?: noteDao.getArchivedNotes()

    val trashedNotes: Flow<List<Note>>
        get() = vaultNoteRepository?.trashedNotes ?: noteDao.getTrashedNotes()

    fun getNote(id: Long): Flow<Note?> =
        vaultNoteRepository?.getNote(id) ?: noteDao.getNoteById(id)

    suspend fun getNoteOnce(id: Long): Note? =
        vaultNoteRepository?.getNoteOnce(id) ?: noteDao.getNoteByIdOnce(id)

    suspend fun getLatestActiveNote(): Note? =
        vaultNoteRepository?.getLatestActiveNote() ?: noteDao.getLatestActiveNote()

    suspend fun getLatestNote(): Note? =
        vaultNoteRepository?.getLatestNote() ?: noteDao.getLatestNote()

    fun searchNotes(query: String): Flow<List<Note>> =
        vaultNoteRepository?.searchNotes(query) ?: noteDao.searchNotes(query)

    suspend fun insert(note: Note): Long =
        vaultNoteRepository?.insert(note) ?: noteDao.insert(note)

    suspend fun insertAll(notes: List<Note>): List<Long> =
        vaultNoteRepository?.insertAll(notes) ?: noteDao.insertAll(notes)

    suspend fun update(note: Note) {
        vaultNoteRepository?.update(note) ?: noteDao.update(note)
    }

    suspend fun updateAll(notes: List<Note>) {
        vaultNoteRepository?.updateAll(notes) ?: noteDao.updateAll(notes)
    }

    suspend fun delete(note: Note) {
        vaultNoteRepository?.delete(note) ?: noteDao.delete(note)
    }

    suspend fun deleteAll(notes: List<Note>) {
        vaultNoteRepository?.deleteAll(notes) ?: noteDao.deleteAll(notes)
    }

    suspend fun deleteById(id: Long) {
        vaultNoteRepository?.deleteById(id) ?: noteDao.deleteById(id)
    }

    suspend fun getAllNotesList(): List<Note> =
        vaultNoteRepository?.getAllNotesList() ?: noteDao.getAllNotesList()

    suspend fun deleteAllNotes(): Int =
        vaultNoteRepository?.deleteAllNotes() ?: noteDao.deleteAllNotes()

    suspend fun autoCleanTrash(olderThanTimestamp: Long): Int =
        vaultNoteRepository?.autoCleanTrash(olderThanTimestamp) ?: noteDao.autoCleanTrash(olderThanTimestamp)

    suspend fun emptyTrash(): Int =
        vaultNoteRepository?.emptyTrash() ?: noteDao.emptyTrash()
}
