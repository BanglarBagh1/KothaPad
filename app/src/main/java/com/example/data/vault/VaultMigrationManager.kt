package com.example.data.vault

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.NoteDao
import com.example.data.db.SpokenCommandDao
import com.example.data.db.StampDao
import com.example.data.model.Note
import com.example.data.model.Stamp
import com.example.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class MigrationSummary(
    val notesCount: Int,
    val templatesCount: Int,
    val foldersCount: Int
)

class VaultMigrationManager(
    private val context: Context,
    private val vaultManager: VaultManager,
    private val noteDao: NoteDao,
    private val stampDao: StampDao,
    private val spokenCommandDao: SpokenCommandDao,
    private val preferencesManager: PreferencesManager,
    private val settingsRepository: SettingsRepository,
    private val vaultConfigStore: VaultConfigStore = VaultConfigStore.getInstance(context)
) {

    companion object {
        const val KOTHAPAD_DIR = VaultConfigStore.CONFIG_DIR_NAME
        const val NOTES_META_FILE = VaultConfigStore.FILE_NOTES_META
        const val WORKSPACE_FILE = VaultConfigStore.FILE_WORKSPACE
        const val TEMPLATES_DIR = "Templates"
        const val GENERAL_DIR = "General"
    }

    suspend fun hasOldInternalData(): Boolean = withContext(Dispatchers.IO) {
        try {
            noteDao.getNoteCount() > 0 || stampDao.getCount() > 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getOldNotesCount(): Int = withContext(Dispatchers.IO) {
        try {
            noteDao.getNoteCount()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun isMigrationCompleted(): Boolean = withContext(Dispatchers.IO) {
        try {
            preferencesManager.isOldDataMigrated.first()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun doesVaultHaveData(vaultDoc: DocumentFile): Boolean = withContext(Dispatchers.IO) {
        try {
            if (vaultConfigStore.hasConfigDir(vaultDoc)) return@withContext true
            val children = vaultManager.listChildren(vaultDoc)
            for (child in children) {
                val name = child.name ?: continue
                if (name.startsWith(".")) continue
                if (child.isFile && name.endsWith(".md", ignoreCase = true)) return@withContext true
                if (child.isDirectory && !name.equals(TEMPLATES_DIR, ignoreCase = true)) {
                    val subChildren = vaultManager.listChildren(child)
                    if (subChildren.any { it.name?.endsWith(".md", ignoreCase = true) == true }) {
                        return@withContext true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun migrateInternalDataToVault(
        vaultDoc: DocumentFile,
        replaceExisting: Boolean = false,
        onProgress: suspend (String) -> Unit = {}
    ): Result<MigrationSummary> = withContext(Dispatchers.IO) {
        // Snapshot existing vault files for safe rollback if migration fails midway
        val snapshot = mutableMapOf<String, ByteArray>()
        try {
            suspend fun snapshotDir(dir: DocumentFile, currentRelPath: String) {
                val children = vaultManager.listChildren(dir)
                for (child in children) {
                    val name = child.name ?: continue
                    val relPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
                    if (child.isDirectory) {
                        snapshotDir(child, relPath)
                    } else if (child.isFile) {
                        context.contentResolver.openInputStream(child.uri)?.use { stream ->
                            val buffer = java.io.ByteArrayOutputStream()
                            stream.copyTo(buffer)
                            snapshot[relPath] = buffer.toByteArray()
                        }
                    }
                }
            }
            snapshotDir(vaultDoc, "")
        } catch (_: Exception) {}

        suspend fun performRollback() {
            try {
                val currentChildren = vaultManager.listChildren(vaultDoc)
                for (ch in currentChildren) {
                    val name = ch.name ?: ""
                    // Do not delete external hidden folders like .obsidian
                    if (!name.startsWith(".") || name == KOTHAPAD_DIR) {
                        vaultManager.delete(ch)
                    }
                }
                for ((relPath, bytes) in snapshot) {
                    val idx = relPath.lastIndexOf('/')
                    val dir = if (idx != -1) {
                        vaultManager.getOrCreateDirRecursive(vaultDoc, relPath.substring(0, idx)) ?: vaultDoc
                    } else vaultDoc
                    val fileName = if (idx != -1) relPath.substring(idx + 1) else relPath
                    vaultManager.writeTextAtomic(dir, fileName, bytes.toString(Charsets.UTF_8))
                }
            } catch (_: Exception) {}
        }

        try {
            onProgress("Preparing vault migration...")

            // If user explicitly chose Replace, remove existing non-hidden files and .kothapad
            if (replaceExisting) {
                onProgress("Clearing previous vault data...")
                val children = vaultManager.listChildren(vaultDoc)
                for (child in children) {
                    val name = child.name ?: continue
                    if (!name.startsWith(".") || name == KOTHAPAD_DIR) {
                        vaultManager.delete(child)
                    }
                }
            }

            // 1. Fetch old data from Room & Preferences
            val oldNotes = noteDao.getAllNotesList()
            val oldStamps = stampDao.getAllStampsList()
            val customFolders = preferencesManager.customFolders.first()

            vaultConfigStore.initVault(vaultDoc)
                ?: throw IllegalStateException("Failed to initialize .KOTHAPAD folder in vault")

            val templatesDir = vaultManager.getOrCreateDirRecursive(vaultDoc, TEMPLATES_DIR)
                ?: throw IllegalStateException("Failed to create Templates folder in vault")

            // 2. Create custom folders
            onProgress("Creating folder structure...")
            for (f in customFolders) {
                if (f.isNotBlank() && !f.equals(GENERAL_DIR, ignoreCase = true)) {
                    vaultManager.getOrCreateDirRecursive(vaultDoc, f)
                }
            }
            for (n in oldNotes) {
                val f = n.folder.trim()
                if (f.isNotBlank() && !f.equals(GENERAL_DIR, ignoreCase = true)) {
                    vaultManager.getOrCreateDirRecursive(vaultDoc, f)
                }
            }

            // 3. Migrate Templates
            onProgress("Migrating templates...")
            var writtenTemplates = 0
            for (st in oldStamps) {
                val cleanLabel = VaultManager.sanitizeFilename(st.label.ifBlank { "Template" })
                val fileName = "$cleanLabel.md"
                val written = vaultManager.writeTextAtomic(
                    parentDir = templatesDir,
                    fileName = fileName,
                    text = st.templateText,
                    mimeType = "text/markdown"
                )
                if (written != null) {
                    writtenTemplates++
                }
            }

            // 4. Migrate Notes & build metadata map
            onProgress("Migrating ${oldNotes.size} notes...")
            val metadataObj = JSONObject()
            var writtenNotes = 0

            for (note in oldNotes) {
                val folderName = if (note.folder.isBlank()) GENERAL_DIR else note.folder.trim()
                val targetDir = vaultManager.getOrCreateDirRecursive(vaultDoc, folderName) ?: vaultDoc

                val cleanTitle = VaultManager.sanitizeFilename(note.title.ifBlank { "Untitled Note" })
                val uniqueFileName = vaultManager.resolveDuplicateFileName(targetDir, cleanTitle, ".md")

                val writtenFile = vaultManager.writeTextAtomic(
                    parentDir = targetDir,
                    fileName = uniqueFileName,
                    text = note.content,
                    mimeType = "text/markdown"
                )

                if (writtenFile != null) {
                    writtenNotes++
                    val relPath = if (folderName.isEmpty() || folderName == ".") uniqueFileName else "$folderName/$uniqueFileName"

                    val meta = JSONObject().apply {
                        put("isPinned", note.isPinned)
                        put("isArchived", note.isArchived)
                        put("isDeleted", note.isDeleted)
                        if (note.deletedAt != null) put("deletedAt", note.deletedAt)
                        put("createdAt", note.createdAt)
                        put("updatedAt", note.updatedAt)
                        if (note.languageCode != null) put("languageCode", note.languageCode)
                        if (note.highlightColor != null) put("highlightColor", note.highlightColor)
                        put("fontSizeModifier", note.fontSizeModifier.toDouble())
                        put("tags", note.tags)
                        put("sessionDurationSec", note.sessionDurationSec)
                    }
                    metadataObj.put(relPath, meta)
                }
            }

            // 5. Write notes-meta.json
            onProgress("Writing notes metadata...")
            val rootMeta = JSONObject().apply {
                put("schemaVersion", 1)
                put("notes", metadataObj)
            }
            vaultConfigStore.writeNotesMeta(vaultDoc, rootMeta)

            // 6. Write workspace.json
            onProgress("Writing workspace settings...")
            val lastActiveFolder = preferencesManager.lastActiveFolder.first()
            val expanded = preferencesManager.expandedFolders.first()
            val wsObj = JSONObject().apply {
                put("schemaVersion", 1)
                put("lastActiveFolder", lastActiveFolder)
                val arr = JSONArray()
                for (exp in expanded) {
                    arr.put(exp)
                }
                put("expandedFolders", arr)
            }
            vaultConfigStore.writeWorkspace(vaultDoc, wsObj)

            // 7. Write settings.json into vault
            onProgress("Writing application settings...")
            settingsRepository.exportCurrentPreferencesToVault(vaultDoc, preferencesManager)

            // 8. Verification: Check file counts
            if (oldNotes.isNotEmpty() && writtenNotes == 0) {
                throw IllegalStateException("No notes could be written to vault storage.")
            }

            // 9. Mark migration completed
            preferencesManager.setOldDataMigrated(true)
            onProgress("Migration completed successfully!")

            Result.success(
                MigrationSummary(
                    notesCount = writtenNotes,
                    templatesCount = writtenTemplates,
                    foldersCount = customFolders.size
                )
            )
        } catch (e: Exception) {
            performRollback()
            Result.failure(e)
        }
    }

    suspend fun deleteOldInternalData(): Int = withContext(Dispatchers.IO) {
        val notesDeleted = noteDao.deleteAllNotes()
        stampDao.deleteAllStamps()
        spokenCommandDao.deleteAllCommands()
        notesDeleted
    }
}
