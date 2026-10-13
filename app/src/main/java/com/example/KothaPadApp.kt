package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.NoteRepository
import com.example.data.repository.SpokenCommandRepository
import com.example.data.repository.StampRepository
import com.example.engine.dictation.SpeechSupervisor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KothaPadApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this, applicationScope) }
    val preferencesManager by lazy { PreferencesManager(this) }

    val noteRepository by lazy { NoteRepository(database.noteDao()) }
    val stampRepository by lazy { StampRepository(database.stampDao()) }
    val spokenCommandRepository by lazy { SpokenCommandRepository(database.spokenCommandDao()) }

    val speechSupervisor by lazy {
        SpeechSupervisor(this, preferencesManager, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        speechSupervisor.restoreCrashSafetyMute()
    }
}
