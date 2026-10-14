package com.example.data.backup

import android.content.Context
import com.example.data.model.Note
import com.example.data.model.SpokenCommand
import com.example.data.model.Stamp
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.NoteRepository
import com.example.data.repository.SpokenCommandRepository
import com.example.data.repository.StampRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
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
    val settings: JSONObject
)

object BackupManager {

    const val CURRENT_BACKUP_VERSION = 2
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

        // Build Manifest JSON
        val manifestObj = JSONObject().apply {
            put("backupVersion", CURRENT_BACKUP_VERSION)
            put("appVersion", appVersion)
            put("appName", "KothaPad")
            put("exportTimestamp", System.currentTimeMillis())
            put("notesCount", notes.size)
            put("templatesCount", stamps.size)
            put("commandsCount", commands.size)
        }

        // Build Notes JSON Array
        val notesArray = JSONArray()
        for (note in notes) {
            notesArray.put(noteToJson(note))
        }

        // Build Templates/Stamps JSON Array
        val stampsArray = JSONArray()
        for (stamp in stamps) {
            stampsArray.put(stampToJson(stamp))
        }

        // Build Spoken Commands JSON Array
        val commandsArray = JSONArray()
        for (cmd in commands) {
            commandsArray.put(spokenCommandToJson(cmd))
        }

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            // 1. Manifest
            zipOut.putNextEntry(ZipEntry(MANIFEST_FILE))
            zipOut.write(manifestObj.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 2. Notes
            zipOut.putNextEntry(ZipEntry(NOTES_FILE))
            zipOut.write(notesArray.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 3. Templates (Stamps)
            zipOut.putNextEntry(ZipEntry(TEMPLATES_FILE))
            zipOut.write(stampsArray.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 4. Settings
            zipOut.putNextEntry(ZipEntry(SETTINGS_FILE))
            zipOut.write(settings.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 5. Spoken Commands
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
        val entries = mutableMapOf<String, String>()

        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zipIn.readBytes()
                        entries[entry.name] = bytes.toString(Charsets.UTF_8)
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

        // 1. Validate Manifest
        val manifestStr = entries[MANIFEST_FILE]
            ?: throw IllegalArgumentException("Missing required '$MANIFEST_FILE' in backup.")
        val manifestObj = try {
            JSONObject(manifestStr)
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted '$MANIFEST_FILE': ${e.message}")
        }

        val backupVersion = safeOptInt(manifestObj, "backupVersion", -1)
        if (backupVersion <= 0) {
            throw IllegalArgumentException("Invalid backup file: 'backupVersion' is missing or invalid.")
        }
        if (backupVersion > CURRENT_BACKUP_VERSION) {
            throw IllegalArgumentException("Unsupported backup version ($backupVersion). This backup was created with a newer version of KothaPad.")
        }

        val appVersion = manifestObj.optString("appVersion", "Unknown")
        val timestamp = safeOptLong(manifestObj, "exportTimestamp", System.currentTimeMillis())

        // 2. Validate Notes
        val notesStr = entries[NOTES_FILE]
            ?: throw IllegalArgumentException("Missing required '$NOTES_FILE' in backup.")
        val notesList = mutableListOf<Note>()
        try {
            val notesArray = JSONArray(notesStr)
            for (i in 0 until notesArray.length()) {
                val item = notesArray.getJSONObject(i)
                notesList.add(jsonToNote(item))
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted notes data in '$NOTES_FILE': ${e.message}")
        }

        // 3. Validate Templates
        val templatesStr = entries[TEMPLATES_FILE]
            ?: throw IllegalArgumentException("Missing required '$TEMPLATES_FILE' in backup.")
        val stampsList = mutableListOf<Stamp>()
        try {
            val templatesArray = JSONArray(templatesStr)
            for (i in 0 until templatesArray.length()) {
                val item = templatesArray.getJSONObject(i)
                stampsList.add(jsonToStamp(item))
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted templates data in '$TEMPLATES_FILE': ${e.message}")
        }

        // 4. Validate Settings
        val settingsStr = entries[SETTINGS_FILE]
            ?: throw IllegalArgumentException("Missing required '$SETTINGS_FILE' in backup.")
        val settingsObj = try {
            JSONObject(settingsStr)
        } catch (e: Exception) {
            throw IllegalArgumentException("Corrupted settings data in '$SETTINGS_FILE': ${e.message}")
        }

        // 5. Optional Spoken Commands
        val commandsList = mutableListOf<SpokenCommand>()
        val commandsStr = entries[COMMANDS_FILE]
        if (!commandsStr.isNullOrBlank()) {
            try {
                val commandsArray = JSONArray(commandsStr)
                for (i in 0 until commandsArray.length()) {
                    commandsList.add(jsonToSpokenCommand(commandsArray.getJSONObject(i)))
                }
            } catch (_: Exception) {
                // Ignore optional command corruption if other essentials are valid
            }
        }

        ParsedBackupData(
            backupVersion = backupVersion,
            appVersion = appVersion,
            exportTimestamp = timestamp,
            notes = notesList,
            stamps = stampsList,
            commands = commandsList,
            settings = settingsObj
        )
    }

    suspend fun restoreBackup(
        noteRepository: NoteRepository,
        stampRepository: StampRepository,
        spokenCommandRepository: SpokenCommandRepository,
        preferencesManager: PreferencesManager,
        backupData: ParsedBackupData
    ): BackupSummary = withContext(Dispatchers.IO) {
        // Snapshot existing data in memory for safe rollback
        val previousNotes = noteRepository.getAllNotesList()
        val previousStamps = stampRepository.getAllStampsList()
        val previousCommands = spokenCommandRepository.getAllCommandsList()
        val previousSettings = preferencesManager.getAllPreferencesForBackup()

        try {
            // Step 1: Replace Notes
            noteRepository.deleteAllNotes()
            if (backupData.notes.isNotEmpty()) {
                noteRepository.insertAll(backupData.notes)
            }

            // Step 2: Replace Templates (Stamps)
            stampRepository.deleteAllStamps()
            if (backupData.stamps.isNotEmpty()) {
                stampRepository.insertAll(backupData.stamps)
            }

            // Step 3: Replace Spoken Commands (if present in backup)
            if (backupData.commands.isNotEmpty()) {
                spokenCommandRepository.deleteAllCommands()
                spokenCommandRepository.insertAll(backupData.commands)
            }

            // Step 4: Replace Settings
            val skippedSettings = preferencesManager.restoreAllPreferencesFromBackup(backupData.settings)

            BackupSummary(
                notesCount = backupData.notes.size,
                templatesCount = backupData.stamps.size,
                commandsCount = backupData.commands.size,
                skippedCount = skippedSettings
            )
        } catch (restoreException: Exception) {
            // Rollback immediately to prior state
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
            } catch (_: Exception) {
                // Secondary error ignored
            }

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

    fun safeToDouble(value: Any?, default: Double = 0.0): Double {
        if (value == null || value == JSONObject.NULL) return default
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
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
