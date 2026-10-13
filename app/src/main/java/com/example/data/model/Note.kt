package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Untitled Note",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val languageCode: String? = null, // null inherits global app default
    val highlightColor: String? = null,
    val fontSizeModifier: Float = 0f,
    // Note Manager extensions
    val folder: String = "General",
    val tags: String = "", // Comma-separated tags, e.g. "ideas,todo"
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val sessionDurationSec: Long = 0L
)
