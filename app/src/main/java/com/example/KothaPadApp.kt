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

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this, applicationScope) }
    val vaultManager by lazy { com.example.data.vault.VaultManager(this) }
    val preferencesManager by lazy { PreferencesManager(this) }
    val vaultConfigStore by lazy { com.example.data.vault.VaultConfigStore.getInstance(this, applicationScope) }
    val settingsRepository by lazy {
        com.example.data.vault.SettingsRepository(this, vaultManager, applicationScope, vaultConfigStore)
    }
    val vaultNoteRepository by lazy {
        com.example.data.vault.VaultNoteRepository(this, vaultManager, preferencesManager, applicationScope, vaultConfigStore)
    }
    val vaultMigrationManager by lazy {
        com.example.data.vault.VaultMigrationManager(
            context = this,
            vaultManager = vaultManager,
            noteDao = database.noteDao(),
            stampDao = database.stampDao(),
            spokenCommandDao = database.spokenCommandDao(),
            preferencesManager = preferencesManager,
            settingsRepository = settingsRepository,
            vaultConfigStore = vaultConfigStore
        )
    }

    val noteRepository by lazy { NoteRepository(database.noteDao(), vaultNoteRepository) }
    val stampRepository by lazy { StampRepository(database.stampDao()) }
    val spokenCommandRepository by lazy { SpokenCommandRepository(database.spokenCommandDao()) }
    val markdownSettingsRepository by lazy { com.example.markdown.MarkdownSettingsRepository(preferencesManager) }

    val speechSupervisor by lazy {
        SpeechSupervisor(this, preferencesManager, applicationScope)
    }

    override fun onCreate() {
        super.onCreate()
        preferencesManager.attachSettingsRepository(settingsRepository)
        speechSupervisor.restoreCrashSafetyMute()
    }
}
