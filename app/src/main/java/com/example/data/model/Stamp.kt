package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stamps")
data class Stamp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setName: String = "Default",
    val label: String,
    val templateText: String,
    val voiceTriggerPhrase: String? = null,
    val orderIndex: Int = 0,
    val isPinned: Boolean = false
)
