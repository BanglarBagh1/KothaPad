package com.example.data.backup

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.Note
import com.example.data.model.SpokenCommand
import com.example.data.model.Stamp
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.NoteRepository
import com.example.data.repository.SpokenCommandRepository
import com.example.data.repository.StampRepository
import com.example.data.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(
    val notesCount: Int,
    val templatesCount: Int,
    val commandsCount: Int = 0,
    val skippedCount: Int = 0
)

data class ParsedBackupData(
    val backupVersion: Int,
    val appVersion: String,
    val exportTimestamp: Long,
    val notes: List<Note>,
    val stamps: List<Stamp>,
    val commands: List<SpokenCommand>,
    val settings: JSONObject,
    val isVaultArchive: Boolean = false,
    val vaultEntries: Map<String, ByteArray> = emptyMap()
)

object BackupManager {

    const val CURRENT_BACKUP_VERSION = 3
    const val MANIFEST_FILE = "manifest.json"
    const val NOTES_FILE = "notes.json"
    const val TEMPLATES_FILE = "templates.json"
    const val SETTINGS_FILE = "settings.json"
    const val COMMANDS_FILE = "spoken_commands.json"

    suspend fun exportBackup(
        context: Context,
        noteRepository: NoteRepository,
        stampRepository: StampRepository,
        spokenCommandRepository: SpokenCommandRepository,
        preferencesManager: PreferencesManager,
        outputStream: OutputStream,
        vaultManager: VaultManager? = null
    ): BackupSummary = withContext(Dispatchers.IO) {
        val vaultDoc = vaultManager?.getCurrentVaultDocumentFile()

        if (vaultDoc != null && vaultDoc.exists()) {
            exportVaultArchive(context, vaultManager, vaultDoc, outputStream)
        } else {
            exportLegacyJsonBackup(context, noteRepository, stampRepository, spokenCommandRepository, preferencesManager, outputStream)
        }
    }

    private suspend fun exportVaultArchive(
        context: Context,
        vaultManager: VaultManager,
        vaultDoc: DocumentFile,
        outputStream: OutputStream
    ): BackupSummary = withContext(Dispatchers.IO) {
        var notesCount = 0
        var templatesCount = 0

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            // Recursively collect all files from the vault
            suspend fun addDirToZip(dir: DocumentFile, currentRelPath: String) {
                val children = vaultManager.listChildren(dir)
                for (child in children) {
                    val name = child.name ?: continue
                    val relPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"

                    if (child.isDirectory) {
                        addDirToZip(child, relPath)
                    } else if (child.isFile) {
                        if (name.endsWith(".md", ignoreCase = true)) {
                            if (relPath.startsWith("Templates/", ignoreCase = true)) {
                                templatesCount++
                            } else {
                                notesCount++
                            }
                        }
                        zipOut.putNextEntry(ZipEntry(relPath))
                        context.contentResolver.openInputStream(child.uri)?.use { inStream ->
                            inStream.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }

            addDirToZip(vaultDoc, "")

            // Write Manifest
            val manifestObj = JSONObject().apply {
                put("backupVersion", CURRENT_BACKUP_VERSION)
                put("appVersion", appVersion)
                put("appName", "KothaPad")
                put("isVault", true)
                put("exportTimestamp", System.currentTimeMillis())
                put("notesCount", notesCount)
                put("templatesCount", templatesCount)
            }
            zipOut.putNextEntry(ZipEntry(MANIFEST_FILE))
            zipOut.write(manifestObj.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.finish()
            zipOut.flush()
        }

        BackupSummary(notesCount = notesCount, templatesCount = templatesCount)
    }

    private suspend fun exportLegacyJsonBackup(
        context: Context,
        noteRepository: NoteRepository,
        stampRepository: StampRepository,
        spokenCommandRepository: SpokenCommandRepository,
        preferencesManager: PreferencesManager,
        outputStream: OutputStream
    ): BackupSummary = withContext(Dispatchers.IO) {
        val notes = noteRepository.getAllNotesList()
        val stamps = stampRepository.getAllStampsList()
        val commands = spokenCommandRepository.getAllCommandsList()
        val settings = preferencesManager.getAllPreferencesForBackup()

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }

        val manifestObj = JSONObject().apply {
            put("backupVersion", 2)
            put("appVersion", appVersion)
            put("appName", "KothaPad")
            put("isVault", false)
            put("exportTimestamp", System.currentTimeMillis())
            put("notesCount", notes.size)
            put("templatesCount", stamps.size)
            put("commandsCount", commands.size)
        }

        val notesArray = JSONArray()
        for (note in notes) {
            notesArray.put(noteToJson(note))
        }

        val stampsArray = JSONArray()
        for (stamp in stamps) {
            stampsArray.put(stampToJson(stamp))
        }

        val commandsArray = JSONArray()
        for (cmd in commands) {
            commandsArray.put(spokenCommandToJson(cmd))
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(MANIFEST_FILE))
            zipOut.write(manifestObj.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(NOTES_FILE))
            zipOut.write(notesArray.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(TEMPLATES_FILE))
            zipOut.write(stampsArray.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(SETTINGS_FILE))
            zipOut.write(settings.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry(COMMANDS_FILE))
            zipOut.write(commandsArray.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.finish()
            zipOut.flush()
        }

        BackupSummary(
            notesCount = notes.size,
            templatesCount = stamps.size,
            commandsCount = commands.size
        )
    }

    suspend fun validateAndParseBackup(inputStream: InputStream): ParsedBackupData = withContext(Dispatchers.IO) {
        val entries = mutableMapOf<String, ByteArray>()

        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val buffer = ByteArrayOutputStream()
                        zipIn.copyTo(buffer)
                        entries[entry.name] = buffer.toByteArray()
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("The selected file is not a valid or readable ZIP backup: ${e.message}")
        }

        if (entries.isEmpty()) {
            throw IllegalArgumentException("The backup archive is empty or corrupted.")
        }

        // Check if vault backup
        val isVault = entries.keys.any { it.startsWith(".kothapad/") || (it.endsWith(".md", ignoreCase = true) && !it.equals(MANIFEST_FILE)) }

        val manifestBytes = entries[MANIFEST_FILE]
        val manifestObj = manifestBytes?.let {
            try {
                JSONObject(it.toString(Charsets.UTF_8))
            } catch (_: Exception) {
                null
            }
        }

        val appVersion = manifestObj?.optString("appVersion", "1.0") ?: "1.0"
        val timestamp = manifestObj?.let { safeOptLong(it, "exportTimestamp", System.currentTimeMillis()) } ?: System.currentTimeMillis()
        val backupVersion = manifestObj?.let { safeOptInt(it, "backupVersion", if (isVault) 3 else 2) } ?: (if (isVault) 3 else 2)

        if (isVault) {
            // Count notes and templates
            var notesCount = 0
            var templatesCount = 0
            val fakeNotes = mutableListOf<Note>()
            val fakeStamps = mutableListOf<Stamp>()

            for ((path, bytes) in entries) {
                if (path.endsWith(".md", ignoreCase = true)) {
                    if (path.startsWith("Templates/", ignoreCase = true)) {
                        templatesCount++
                        val label = path.substringAfterLast("/").removeSuffix(".md")
                        fakeStamps.add(Stamp(label = label, templateText = bytes.toString(Charsets.UTF_8)))
                    } else {
                        notesCount++
                        val title = path.substringAfterLast("/").removeSuffix(".md")
                        val folder = if (path.contains("/")) path.substringBeforeLast("/") else "General"
                        fakeNotes.add(Note(title = title, content = bytes.toString(Charsets.UTF_8), folder = folder))
                    }
                }
            }

            return@withContext ParsedBackupData(
                backupVersion = backupVersion,
                appVersion = appVersion,
                exportTimestamp = timestamp,
                notes = fakeNotes,
                stamps = fakeStamps,
                commands = emptyList(),
                settings = JSONObject(),
                isVaultArchive = true,
                vaultEntries = entries
            )
        }

        // Legacy JSON validation
        val notesStr = entries[NOTES_FILE]?.toString(Charsets.UTF_8)
            ?: throw IllegalArgumentException("Missing required '$NOTES_FILE' in backup.")
        val notesList = mutableListOf<Note>()
        try {
            val notesArray = JSONArray(notesStr)
            for (i in 0 until notesArray.length()) {
                notesList.add(jsonToNote(notesArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted notes data in '$NOTES_FILE': ${e.message}")
        }

        val templatesStr = entries[TEMPLATES_FILE]?.toString(Charsets.UTF_8)
            ?: throw IllegalArgumentException("Missing required '$TEMPLATES_FILE' in backup.")
        val stampsList = mutableListOf<Stamp>()
        try {
            val templatesArray = JSONArray(templatesStr)
            for (i in 0 until templatesArray.length()) {
                stampsList.add(jsonToStamp(templatesArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted templates data in '$TEMPLATES_FILE': ${e.message}")
        }

        val settingsStr = entries[SETTINGS_FILE]?.toString(Charsets.UTF_8)
            ?: throw IllegalArgumentException("Missing required '$SETTINGS_FILE' in backup.")
        val settingsObj = try {
            JSONObject(settingsStr)
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted settings data in '$SETTINGS_FILE': ${e.message}")
        }

        val commandsList = mutableListOf<SpokenCommand>()
        val commandsStr = entries[COMMANDS_FILE]?.toString(Charsets.UTF_8)
        if (!commandsStr.isNullOrBlank()) {
            try {
                val commandsArray = JSONArray(commandsStr)
                for (i in 0 until commandsArray.length()) {
                    commandsList.add(jsonToSpokenCommand(commandsArray.getJSONObject(i)))
                }
            } catch (_: Exception) {}
        }

        ParsedBackupData(
            backupVersion = backupVersion,
            appVersion = appVersion,
            exportTimestamp = timestamp,
            notes = notesList,
            stamps = stampsList,
            commands = commandsList,
            settings = settingsObj,
            isVaultArchive = false,
            vaultEntries = emptyMap()
        )
    }

    suspend fun restoreBackup(
        context: Context,
        noteRepository: NoteRepository,
        stampRepository: StampRepository,
        spokenCommandRepository: SpokenCommandRepository,
        preferencesManager: PreferencesManager,
        vaultManager: VaultManager? = null,
        backupData: ParsedBackupData
    ): BackupSummary = withContext(Dispatchers.IO) {
        val vaultDoc = vaultManager?.getCurrentVaultDocumentFile()

        if (vaultDoc != null && vaultDoc.exists()) {
            restoreVaultBackupWithRollback(context, vaultManager, vaultDoc, preferencesManager, backupData)
        } else {
            restoreLegacyRoomBackup(noteRepository, stampRepository, spokenCommandRepository, preferencesManager, backupData)
        }
    }

    private suspend fun restoreVaultBackupWithRollback(
        context: Context,
        vaultManager: VaultManager,
        vaultDoc: DocumentFile,
        preferencesManager: PreferencesManager,
        backupData: ParsedBackupData
    ): BackupSummary = withContext(Dispatchers.IO) {
        // Snapshot existing vault files for rollback
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
                            val buffer = ByteArrayOutputStream()
                            stream.copyTo(buffer)
                            snapshot[relPath] = buffer.toByteArray()
                        }
                    }
                }
            }
            snapshotDir(vaultDoc, "")
        } catch (_: Exception) {}

        // Helper to rollback if anything fails
        suspend fun performRollback() {
            try {
                // Clear damaged files
                val currentChildren = vaultManager.listChildren(vaultDoc)
                for (ch in currentChildren) {
                    vaultManager.delete(ch)
                }
                // Restore snapshot
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
            // Delete existing files in vault
            val currentChildren = vaultManager.listChildren(vaultDoc)
            for (ch in currentChildren) {
                vaultManager.delete(ch)
            }

            var notesRestored = 0
            var templatesRestored = 0

            if (backupData.isVaultArchive) {
                // Restore directly from vault entries
                for ((relPath, bytes) in backupData.vaultEntries) {
                    if (relPath.equals(MANIFEST_FILE, ignoreCase = true)) continue
                    val idx = relPath.lastIndexOf('/')
                    val dir = if (idx != -1) {
                        vaultManager.getOrCreateDirRecursive(vaultDoc, relPath.substring(0, idx))
                            ?: throw IllegalStateException("Cannot create directory for $relPath")
                    } else vaultDoc
                    val fileName = if (idx != -1) relPath.substring(idx + 1) else relPath
                    val textContent = bytes.toString(Charsets.UTF_8)
                    val mime = if (fileName.endsWith(".json")) "application/json" else "text/markdown"
                    val written = vaultManager.writeTextAtomic(dir, fileName, textContent, mime)
                        ?: throw IllegalStateException("Failed to write file $relPath")

                    if (fileName.endsWith(".md", ignoreCase = true)) {
                        if (relPath.startsWith("Templates/", ignoreCase = true)) {
                            templatesRestored++
                        } else {
                            notesRestored++
                        }
                    }
                }
            } else {
                // Convert legacy backup into vault files
                val kothaDir = vaultManager.getOrCreateDirRecursive(vaultDoc, ".kothapad")
                    ?: throw IllegalStateException("Cannot create .kothapad directory")
                val templatesDir = vaultManager.getOrCreateDirRecursive(vaultDoc, "Templates")
                    ?: throw IllegalStateException("Cannot create Templates directory")

                // Write templates
                for (stamp in backupData.stamps) {
                    val cleanLabel = VaultManager.sanitizeFilename(stamp.label.ifBlank { "Template" })
                    vaultManager.writeTextAtomic(templatesDir, "$cleanLabel.md", stamp.templateText, "text/markdown")
                    templatesRestored++
                }

                // Write notes and metadata
                val notesMetaObj = JSONObject()
                for (note in backupData.notes) {
                    val folderName = if (note.folder.isBlank()) "General" else note.folder.trim()
                    val targetDir = vaultManager.getOrCreateDirRecursive(vaultDoc, folderName) ?: vaultDoc
                    val cleanTitle = VaultManager.sanitizeFilename(note.title.ifBlank { "Untitled Note" })
                    val uniqueFileName = vaultManager.resolveDuplicateFileName(targetDir, cleanTitle, ".md")

                    vaultManager.writeTextAtomic(targetDir, uniqueFileName, note.content, "text/markdown")
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
                    notesMetaObj.put(relPath, meta)
                    notesRestored++
                }

                // Write .kothapad/notes-meta.json
                val rootMeta = JSONObject().apply {
                    put("schemaVersion", 1)
                    put("notes", notesMetaObj)
                }
                vaultManager.writeTextAtomic(kothaDir, "notes-meta.json", rootMeta.toString(2), "application/json")

                // Write .kothapad/settings.json
                vaultManager.writeTextAtomic(kothaDir, "settings.json", backupData.settings.toString(2), "application/json")

                // Write .kothapad/workspace.json
                val wsObj = JSONObject().apply {
                    put("schemaVersion", 1)
                    put("lastActiveFolder", "General")
                    put("expandedFolders", JSONArray().apply { put("General"); put("Templates") })
                }
                vaultManager.writeTextAtomic(kothaDir, "workspace.json", wsObj.toString(2), "application/json")
            }

            BackupSummary(
                notesCount = notesRestored,
                templatesCount = templatesRestored
            )
        } catch (restoreException: Exception) {
            performRollback()
            throw Exception("Restoration failed midway: ${restoreException.message}. Previous vault data was safely rolled back.")
        }
    }

    private suspend fun restoreLegacyRoomBackup(
        noteRepository: NoteRepository,
        stampRepository: StampRepository,
        spokenCommandRepository: SpokenCommandRepository,
        preferencesManager: PreferencesManager,
        backupData: ParsedBackupData
    ): BackupSummary = withContext(Dispatchers.IO) {
        val previousNotes = noteRepository.getAllNotesList()
        val previousStamps = stampRepository.getAllStampsList()
        val previousCommands = spokenCommandRepository.getAllCommandsList()
        val previousSettings = preferencesManager.getAllPreferencesForBackup()

        try {
            noteRepository.deleteAllNotes()
            if (backupData.notes.isNotEmpty()) {
                noteRepository.insertAll(backupData.notes)
            }

            stampRepository.deleteAllStamps()
            if (backupData.stamps.isNotEmpty()) {
                stampRepository.insertAll(backupData.stamps)
            }

            if (backupData.commands.isNotEmpty()) {
                spokenCommandRepository.deleteAllCommands()
                spokenCommandRepository.insertAll(backupData.commands)
            }

            val skippedSettings = preferencesManager.restoreAllPreferencesFromBackup(backupData.settings)

            BackupSummary(
                notesCount = backupData.notes.size,
                templatesCount = backupData.stamps.size,
                commandsCount = backupData.commands.size,
                skippedCount = skippedSettings
            )
        } catch (restoreException: Exception) {
            try {
                noteRepository.deleteAllNotes()
                if (previousNotes.isNotEmpty()) {
                    noteRepository.insertAll(previousNotes)
                }
                stampRepository.deleteAllStamps()
                if (previousStamps.isNotEmpty()) {
                    stampRepository.insertAll(previousStamps)
                }
                if (previousCommands.isNotEmpty()) {
                    spokenCommandRepository.deleteAllCommands()
                    spokenCommandRepository.insertAll(previousCommands)
                }
                preferencesManager.restoreAllPreferencesFromBackup(previousSettings)
            } catch (_: Exception) {}

            throw Exception("Restoration failed midway: ${restoreException.message}. Previous data was safely rolled back.")
        }
    }

    // --- JSON Conversion Helpers ---

    private fun noteToJson(note: Note): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("content", note.content)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
            put("isPinned", note.isPinned)
            put("languageCode", note.languageCode ?: JSONObject.NULL)
            put("highlightColor", note.highlightColor ?: JSONObject.NULL)
            put("fontSizeModifier", note.fontSizeModifier.toDouble())
            put("folder", note.folder)
            put("tags", note.tags)
            put("isArchived", note.isArchived)
            put("isDeleted", note.isDeleted)
            put("deletedAt", note.deletedAt ?: JSONObject.NULL)
            put("sessionDurationSec", note.sessionDurationSec)
        }
    }

    private fun jsonToNote(obj: JSONObject): Note {
        return Note(
            id = safeOptLong(obj, "id", 0L),
            title = obj.optString("title", "Untitled Note"),
            content = obj.optString("content", ""),
            createdAt = safeOptLong(obj, "createdAt", System.currentTimeMillis()),
            updatedAt = safeOptLong(obj, "updatedAt", System.currentTimeMillis()),
            isPinned = safeOptBoolean(obj, "isPinned", false),
            languageCode = if (obj.isNull("languageCode")) null else obj.optString("languageCode"),
            highlightColor = if (obj.isNull("highlightColor")) null else obj.optString("highlightColor"),
            fontSizeModifier = safeOptFloat(obj, "fontSizeModifier", 0.0f),
            folder = obj.optString("folder", "General"),
            tags = obj.optString("tags", ""),
            isArchived = safeOptBoolean(obj, "isArchived", false),
            isDeleted = safeOptBoolean(obj, "isDeleted", false),
            deletedAt = if (obj.isNull("deletedAt")) null else safeOptLong(obj, "deletedAt", 0L),
            sessionDurationSec = safeOptLong(obj, "sessionDurationSec", 0L)
        )
    }

    private fun stampToJson(stamp: Stamp): JSONObject {
        return JSONObject().apply {
            put("id", stamp.id)
            put("setName", stamp.setName)
            put("label", stamp.label)
            put("templateText", stamp.templateText)
            put("voiceTriggerPhrase", stamp.voiceTriggerPhrase ?: JSONObject.NULL)
            put("orderIndex", stamp.orderIndex)
            put("isPinned", stamp.isPinned)
        }
    }

    private fun jsonToStamp(obj: JSONObject): Stamp {
        return Stamp(
            id = safeOptLong(obj, "id", 0L),
            setName = obj.optString("setName", "Default"),
            label = obj.optString("label", "Untitled Template"),
            templateText = obj.optString("templateText", ""),
            voiceTriggerPhrase = if (obj.isNull("voiceTriggerPhrase")) null else obj.optString("voiceTriggerPhrase"),
            orderIndex = safeOptInt(obj, "orderIndex", 0),
            isPinned = safeOptBoolean(obj, "isPinned", false)
        )
    }

    private fun spokenCommandToJson(cmd: SpokenCommand): JSONObject {
        return JSONObject().apply {
            put("id", cmd.id)
            put("language", cmd.language)
            put("phrase", cmd.phrase)
            put("actionType", cmd.actionType)
            put("replacementText", cmd.replacementText)
            put("commandCode", cmd.commandCode ?: JSONObject.NULL)
            put("isEnabled", cmd.isEnabled)
        }
    }

    private fun jsonToSpokenCommand(obj: JSONObject): SpokenCommand {
        return SpokenCommand(
            id = safeOptLong(obj, "id", 0L),
            language = obj.optString("language", "en"),
            phrase = obj.optString("phrase", ""),
            actionType = obj.optString("actionType", "PUNCTUATION"),
            replacementText = obj.optString("replacementText", ""),
            commandCode = if (obj.isNull("commandCode")) null else obj.optString("commandCode"),
            isEnabled = safeOptBoolean(obj, "isEnabled", true)
        )
    }

    // --- Safe Type Conversion Helpers ---

    fun safeToLong(value: Any?, default: Long = 0L): Long {
        if (value == null || value == JSONObject.NULL) return default
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: default
            is Boolean -> if (value) 1L else 0L
            else -> default
        }
    }

    fun safeToInt(value: Any?, default: Int = 0): Int {
        if (value == null || value == JSONObject.NULL) return default
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            is Boolean -> if (value) 1 else 0
            else -> default
        }
    }

    fun safeToFloat(value: Any?, default: Float = 0.0f): Float {
        if (value == null || value == JSONObject.NULL) return default
        return when (value) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: default
            else -> default
        }
    }

    fun safeToBoolean(value: Any?, default: Boolean = false): Boolean {
        if (value == null || value == JSONObject.NULL) return default
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when {
                value.equals("true", ignoreCase = true) -> true
                value.equals("false", ignoreCase = true) -> false
                value == "1" -> true
                value == "0" -> false
                else -> default
            }
            else -> default
        }
    }

    fun safeOptLong(obj: JSONObject, key: String, default: Long = 0L): Long {
        return safeToLong(obj.opt(key), default)
    }

    fun safeOptInt(obj: JSONObject, key: String, default: Int = 0): Int {
        return safeToInt(obj.opt(key), default)
    }

    fun safeOptFloat(obj: JSONObject, key: String, default: Float = 0.0f): Float {
        return safeToFloat(obj.opt(key), default)
    }

    fun safeOptBoolean(obj: JSONObject, key: String, default: Boolean = false): Boolean {
        return safeToBoolean(obj.opt(key), default)
    }
}
