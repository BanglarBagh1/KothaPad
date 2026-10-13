package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spoken_commands")
data class SpokenCommand(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val language: String, // "en", "bn", or "all"
    val phrase: String,
    val actionType: String, // "PUNCTUATION" or "ACTION"
    val replacementText: String = "",
    val commandCode: String? = null, // "DELETE_LAST_WORD", "UNDO", "STOP_DICTATION"
    val isEnabled: Boolean = true
)
