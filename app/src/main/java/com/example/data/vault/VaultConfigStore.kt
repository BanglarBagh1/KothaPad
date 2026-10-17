package com.example.data.vault

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * VaultConfigStore: The single source of truth and ONLY class allowed to touch the .KOTHAPAD folder.
 * Operates like Obsidian's .obsidian folder:
 * - A vault is just a folder identified by its content/path.
 * - .KOTHAPAD is reused if it exists; only created with defaults if absent.
 * - All operations are protected by a single Mutex to prevent duplicate folders or files.
 * - Files are updated in-place via truncate mode ("wt"), preserving unknown keys.
 * - Automatic duplicate cleanup and migration merges stray/duplicate config files.
 */
class VaultConfigStore(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val mutex = Mutex()

    // In-memory cache of parsed JSON per file name for the currently open vault
    private val jsonCache = ConcurrentHashMap<String, JSONObject>()
    // In-memory record of the last serialized string written to disk (for no-op write avoidance)
    private val lastWrittenContent = ConcurrentHashMap<String, String>()
    // Debounce jobs per config file
    private val debounceJobs = ConcurrentHashMap<String, Job>()

    private var activeVaultUri: String? = null
    private var activeConfigDir: DocumentFile? = null

    companion object {
        const val CONFIG_DIR_NAME = ".KOTHAPAD"
        const val TRASH_DIR_NAME = "trash"
        const val FILE_SETTINGS = "settings.json"
        const val FILE_NOTES_META = "notes-meta.json"
        const val FILE_WORKSPACE = "workspace.json"

        val ALLOWED_FILES = setOf(FILE_SETTINGS, FILE_NOTES_META, FILE_WORKSPACE)

        @Volatile
        private var instance: VaultConfigStore? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ): VaultConfigStore {
            return instance ?: synchronized(this) {
                instance ?: VaultConfigStore(context.applicationContext, scope).also { instance = it }
            }
        }

        fun isConfigDir(dirName: String): Boolean {
            return dirName.equals(CONFIG_DIR_NAME, ignoreCase = true)
        }

        fun isConfigFile(fileName: String): Boolean {
            return ALLOWED_FILES.any { it.equals(fileName, ignoreCase = true) }
        }

        fun isConfigFileOrDuplicate(fileName: String): Boolean {
            val lower = fileName.lowercase()
            return (lower.startsWith("settings") || lower.startsWith("notes-meta") || lower.startsWith("workspace")) &&
                    (lower.endsWith(".json") || lower.endsWith(".bak"))
        }

        fun deepMerge(target: JSONObject, source: JSONObject): JSONObject {
            val keys = source.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val srcVal = source.get(key)
                if (target.has(key)) {
                    val tgtVal = target.get(key)
                    if (tgtVal is JSONObject && srcVal is JSONObject) {
                        deepMerge(tgtVal, srcVal)
                    } else {
                        target.put(key, srcVal)
                    }
                } else {
                    target.put(key, srcVal)
                }
            }
            return target
        }
    }

    /**
     * Initializes a vault root on open:
     * Resolves the canonical .KOTHAPAD directory, performs duplicate cleanup & migration,
     * and seeds in-memory caches.
     */
    suspend fun initVault(vaultRoot: DocumentFile): DocumentFile? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val rootUri = vaultRoot.uri.toString()
            if (activeVaultUri != rootUri) {
                // Vault switched or newly opened
                debounceJobs.values.forEach { it.cancel() }
                debounceJobs.clear()
                jsonCache.clear()
                lastWrittenContent.clear()
                activeVaultUri = rootUri
                activeConfigDir = null
            }

            val configDir = resolveConfigDirInternal(vaultRoot) ?: return@withContext null
            activeConfigDir = configDir

            // Run duplicate cleanup and migration once inside mutex
            cleanupAndMigrateDuplicatesInternal(vaultRoot, configDir)

            configDir
        }
    }

    /**
     * Check if a vault already has an existing .KOTHAPAD folder.
     */
    suspend fun hasConfigDir(vaultRoot: DocumentFile): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val children = vaultRoot.listFiles()
                children.any { it.isDirectory && isConfigDir(it.name ?: "") }
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun hasConfigFile(vaultRoot: DocumentFile, fileName: String): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val configDir = resolveConfigDirInternal(vaultRoot) ?: return@withContext false
                val children = configDir.listFiles()
                children.any { it.isFile && it.name.equals(fileName, ignoreCase = true) }
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Resolves the canonical .KOTHAPAD directory inside vaultRoot.
     * Lists children once. Reuses existing directory if present.
     * Creates .KOTHAPAD only if absent, verifying creation.
     */
    suspend fun resolveConfigDir(vaultRoot: DocumentFile): DocumentFile? = mutex.withLock {
        withContext(Dispatchers.IO) {
            resolveConfigDirInternal(vaultRoot)
        }
    }

    suspend fun getOrCreateTrashDir(vaultRoot: DocumentFile): DocumentFile? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val configDir = resolveConfigDirInternal(vaultRoot) ?: return@withContext null
            val children = configDir.listFiles()
            val existing = children.firstOrNull { it.isDirectory && it.name.equals(TRASH_DIR_NAME, ignoreCase = true) }
            if (existing != null && existing.exists()) return@withContext existing
            val created = configDir.createDirectory(TRASH_DIR_NAME)
            val relisted = configDir.listFiles()
            relisted.firstOrNull { it.isDirectory && it.name.equals(TRASH_DIR_NAME, ignoreCase = true) } ?: created
        }
    }

    suspend fun getTrashDir(vaultRoot: DocumentFile): DocumentFile? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val configDir = resolveConfigDirInternal(vaultRoot) ?: return@withContext null
            val children = configDir.listFiles()
            children.firstOrNull { it.isDirectory && it.name.equals(TRASH_DIR_NAME, ignoreCase = true) }
        }
    }

    private fun resolveConfigDirInternal(vaultRoot: DocumentFile): DocumentFile? {
        if (!vaultRoot.exists() || !vaultRoot.canWrite()) return null

        val children = vaultRoot.listFiles()
        // Find existing folder named .KOTHAPAD (case-insensitive)
        val exactMatch = children.firstOrNull { it.isDirectory && (it.name ?: "") == CONFIG_DIR_NAME }
        val caseMatch = children.firstOrNull { it.isDirectory && isConfigDir(it.name ?: "") }
        val existing = exactMatch ?: caseMatch

        if (existing != null && existing.exists()) {
            return existing
        }

        // None exists: create once with canonical name
        val created = vaultRoot.createDirectory(CONFIG_DIR_NAME)
        val relisted = vaultRoot.listFiles()
        val verified = relisted.firstOrNull { it.isDirectory && isConfigDir(it.name ?: "") }
        return verified ?: created
    }

    /**
     * Locates or creates a fixed configuration file in configDir.
     * Returns the DocumentFile. If already present, never calls createFile.
     */
    suspend fun getOrCreateFile(configDir: DocumentFile, name: String): DocumentFile? = mutex.withLock {
        withContext(Dispatchers.IO) {
            getOrCreateFileInternal(configDir, name)
        }
    }

    private fun getOrCreateFileInternal(configDir: DocumentFile, name: String): DocumentFile? {
        require(ALLOWED_FILES.contains(name)) { "Unauthorized config file: $name" }

        val children = configDir.listFiles()
        val existing = children.firstOrNull { it.isFile && it.name.equals(name, ignoreCase = true) }
        if (existing != null && existing.exists()) {
            return existing
        }

        // File does not exist yet; create it
        val created = configDir.createFile("application/json", name)
        val relisted = configDir.listFiles()

        // Check if provider renamed it (e.g. "settings (1).json" or "settings.json.json")
        val exact = relisted.firstOrNull { it.isFile && it.name.equals(name, ignoreCase = true) }
        if (exact != null) {
            return exact
        }

        val mangled = relisted.firstOrNull { it.isFile && (it.name ?: "").startsWith(name.removeSuffix(".json")) }
        if (mangled != null && mangled.name != name) {
            try {
                mangled.renameTo(name)
            } catch (_: Exception) {}
            val finalCheck = configDir.listFiles().firstOrNull { it.isFile && it.name.equals(name, ignoreCase = true) }
            if (finalCheck != null) return finalCheck
        }

        return created
    }

    // =========================================================================
    // Reading Config Files
    // =========================================================================

    suspend fun readSettings(vaultRoot: DocumentFile): JSONObject? = readConfigFile(vaultRoot, FILE_SETTINGS)
    suspend fun readNotesMeta(vaultRoot: DocumentFile): JSONObject? = readConfigFile(vaultRoot, FILE_NOTES_META)
    suspend fun readWorkspace(vaultRoot: DocumentFile): JSONObject? = readConfigFile(vaultRoot, FILE_WORKSPACE)

    suspend fun readConfigFile(vaultRoot: DocumentFile, fileName: String): JSONObject? = mutex.withLock {
        withContext(Dispatchers.IO) {
            // Check in-memory cache
            jsonCache[fileName]?.let { return@withContext JSONObject(it.toString()) }

            val configDir = resolveConfigDirInternal(vaultRoot) ?: return@withContext null
            val children = configDir.listFiles()
            val file = children.firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) } ?: return@withContext null

            val text = readFileText(file)
            if (text.isBlank()) return@withContext null

            try {
                val json = JSONObject(text)
                // Schema version check: ensure at least 1
                if (!json.has("schemaVersion") || json.optInt("schemaVersion", 1) < 1) {
                    json.put("schemaVersion", 1)
                }
                jsonCache[fileName] = JSONObject(json.toString())
                lastWrittenContent[fileName] = text
                json
            } catch (_: Exception) {
                null
            }
        }
    }

    // =========================================================================
    // In-Place Writing & Debouncing
    // =========================================================================

    suspend fun updateSettings(vaultRoot: DocumentFile, updatedJson: JSONObject, immediate: Boolean = false) {
        updateConfigFile(vaultRoot, FILE_SETTINGS, updatedJson, immediate)
    }

    suspend fun updateNotesMeta(vaultRoot: DocumentFile, updatedJson: JSONObject, immediate: Boolean = false) {
        updateConfigFile(vaultRoot, FILE_NOTES_META, updatedJson, immediate)
    }

    suspend fun updateWorkspace(vaultRoot: DocumentFile, updatedJson: JSONObject, immediate: Boolean = false) {
        updateConfigFile(vaultRoot, FILE_WORKSPACE, updatedJson, immediate)
    }

    suspend fun writeSettings(vaultRoot: DocumentFile, json: JSONObject) {
        updateConfigFile(vaultRoot, FILE_SETTINGS, json, immediate = true)
    }

    suspend fun writeNotesMeta(vaultRoot: DocumentFile, json: JSONObject) {
        updateConfigFile(vaultRoot, FILE_NOTES_META, json, immediate = true)
    }

    suspend fun writeWorkspace(vaultRoot: DocumentFile, json: JSONObject) {
        updateConfigFile(vaultRoot, FILE_WORKSPACE, json, immediate = true)
    }

    private suspend fun updateConfigFile(
        vaultRoot: DocumentFile,
        fileName: String,
        updatedJson: JSONObject,
        immediate: Boolean = false
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val existing = jsonCache[fileName] ?: readConfigFileInternal(vaultRoot, fileName) ?: JSONObject()
            val merged = if (existing.length() > 0) {
                // Preserve schemaVersion rule
                val oldVersion = existing.optInt("schemaVersion", 1)
                val newVersion = updatedJson.optInt("schemaVersion", 1)
                val preserved = deepMerge(existing, updatedJson)
                preserved.put("schemaVersion", maxOf(oldVersion, newVersion, 1))
                preserved
            } else {
                if (!updatedJson.has("schemaVersion") || updatedJson.optInt("schemaVersion", 1) < 1) {
                    updatedJson.put("schemaVersion", 1)
                }
                updatedJson
            }

            jsonCache[fileName] = JSONObject(merged.toString())

            debounceJobs[fileName]?.cancel()

            if (immediate) {
                writeToDiskInternal(vaultRoot, fileName, merged)
            } else {
                val job = scope.launch(Dispatchers.IO) {
                    delay(500)
                    mutex.withLock {
                        val toWrite = jsonCache[fileName] ?: return@withLock
                        writeToDiskInternal(vaultRoot, fileName, toWrite)
                    }
                }
                debounceJobs[fileName] = job
            }
        }
    }

    suspend fun flush(fileName: String? = null) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val rootDoc = activeConfigDir?.parentFile ?: return@withContext
            if (fileName != null) {
                debounceJobs[fileName]?.cancel()
                debounceJobs.remove(fileName)
                val json = jsonCache[fileName] ?: return@withContext
                writeToDiskInternal(rootDoc, fileName, json)
            } else {
                for ((fName, job) in debounceJobs) {
                    job.cancel()
                    val json = jsonCache[fName] ?: continue
                    writeToDiskInternal(rootDoc, fName, json)
                }
                debounceJobs.clear()
            }
        }
    }

    private fun readConfigFileInternal(vaultRoot: DocumentFile, fileName: String): JSONObject? {
        val configDir = resolveConfigDirInternal(vaultRoot) ?: return null
        val file = configDir.listFiles().firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) } ?: return null
        val text = readFileText(file)
        if (text.isBlank()) return null
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeToDiskInternal(vaultRoot: DocumentFile, fileName: String, json: JSONObject): Boolean {
        return try {
            if (!json.has("schemaVersion") || json.optInt("schemaVersion", 1) < 1) {
                json.put("schemaVersion", 1)
            }
            val formatted = json.toString(2)
            // Validate JSON formatting
            JSONObject(formatted)

            // Avoid writing if content did not change
            val lastSaved = lastWrittenContent[fileName]
            if (lastSaved == formatted) {
                return true
            }

            val configDir = resolveConfigDirInternal(vaultRoot) ?: return false
            val file = getOrCreateFileInternal(configDir, fileName) ?: return false

            // In-place update with truncate mode ("wt")
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(formatted)
                    writer.flush()
                }
            }
            lastWrittenContent[fileName] = formatted
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readFileText(file: DocumentFile): String {
        return try {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // =========================================================================
    // Duplicate Cleanup & Migration (Step 6)
    // =========================================================================

    private fun cleanupAndMigrateDuplicatesInternal(vaultRoot: DocumentFile, canonicalDir: DocumentFile) {
        try {
            val rootChildren = vaultRoot.listFiles()

            // 1. Identify stray duplicate directories (e.g. .KOTHAPAD (1), .kothapad (2), stray .kothapad)
            val duplicateDirs = rootChildren.filter {
                it.isDirectory && it.uri != canonicalDir.uri && (
                        (it.name ?: "").startsWith(".KOTHAPAD", ignoreCase = true) ||
                        (it.name ?: "").startsWith(".kothapad", ignoreCase = true)
                )
            }

            // 2. Identify stray config files sitting directly inside the vault root
            val strayRootFiles = rootChildren.filter {
                it.isFile && isConfigFileOrDuplicate(it.name ?: "")
            }

            // 3. Identify duplicate / backup files inside canonical .KOTHAPAD
            val canonicalChildren = canonicalDir.listFiles()
            val duplicateCanonicalFiles = canonicalChildren.filter {
                it.isFile && !ALLOWED_FILES.contains(it.name) && isConfigFileOrDuplicate(it.name ?: "")
            }

            if (duplicateDirs.isEmpty() && strayRootFiles.isEmpty() && duplicateCanonicalFiles.isEmpty()) {
                return // Nothing to clean up
            }

            // Merge each allowed file type
            for (targetFileName in ALLOWED_FILES) {
                val candidateFiles = mutableListOf<DocumentFile>()

                // Primary file if present
                canonicalChildren.firstOrNull { it.isFile && (it.name ?: "").equals(targetFileName, ignoreCase = true) }?.let {
                    candidateFiles.add(it)
                }

                // Duplicates in canonical directory
                val targetBase = targetFileName.removeSuffix(".json")
                for (f in duplicateCanonicalFiles) {
                    if ((f.name ?: "").lowercase().startsWith(targetBase)) {
                        candidateFiles.add(f)
                    }
                }

                // Stray files in vault root
                for (f in strayRootFiles) {
                    if ((f.name ?: "").lowercase().startsWith(targetBase)) {
                        candidateFiles.add(f)
                    }
                }

                // Files in duplicate directories
                for (dupDir in duplicateDirs) {
                    val subFiles = dupDir.listFiles()
                    for (f in subFiles) {
                        if (f.isFile && (f.name ?: "").lowercase().startsWith(targetBase)) {
                            candidateFiles.add(f)
                        }
                    }
                }

                if (candidateFiles.size > 1) {
                    // Sort candidate files from oldest to newest by lastModified
                    val sorted = candidateFiles.sortedBy { it.lastModified() }
                    val newestCanonical = getOrCreateFileInternal(canonicalDir, targetFileName) ?: continue

                    if (targetFileName == FILE_NOTES_META) {
                        mergeNotesMetaCandidates(sorted, newestCanonical)
                    } else {
                        mergeGenericJsonCandidates(sorted, newestCanonical)
                    }

                    // Verify canonical file can be read back cleanly before deleting duplicates
                    val verifiedText = readFileText(newestCanonical)
                    if (verifiedText.isNotBlank()) {
                        try {
                            JSONObject(verifiedText)
                            // Safe to delete duplicate files (excluding newestCanonical)
                            for (c in candidateFiles) {
                                if (c.uri != newestCanonical.uri) {
                                    c.delete()
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // Clean up any empty duplicate directories
            for (dupDir in duplicateDirs) {
                try {
                    val remaining = dupDir.listFiles()
                    for (rem in remaining) {
                        if (rem.isFile && isConfigFileOrDuplicate(rem.name ?: "")) {
                            rem.delete()
                        }
                    }
                    if (dupDir.listFiles().isEmpty()) {
                        dupDir.delete()
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Keep going safely; never block vault loading
        }
    }

    private fun mergeGenericJsonCandidates(candidates: List<DocumentFile>, targetFile: DocumentFile) {
        val merged = JSONObject()
        var maxVersion = 1

        for (file in candidates) {
            val text = readFileText(file)
            if (text.isBlank()) continue
            try {
                val json = JSONObject(text)
                val ver = json.optInt("schemaVersion", 1)
                if (ver > maxVersion) maxVersion = ver
                deepMerge(merged, json)
            } catch (_: Exception) {}
        }
        merged.put("schemaVersion", maxVersion)

        try {
            val formatted = merged.toString(2)
            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(formatted); it.flush() }
            }
        } catch (_: Exception) {}
    }

    private fun mergeNotesMetaCandidates(candidates: List<DocumentFile>, targetFile: DocumentFile) {
        val mergedNotesObj = JSONObject()
        val mergedRoot = JSONObject()
        var maxVersion = 1

        for (file in candidates) {
            val text = readFileText(file)
            if (text.isBlank()) continue
            try {
                val json = JSONObject(text)
                val ver = json.optInt("schemaVersion", 1)
                if (ver > maxVersion) maxVersion = ver

                // Preserve any unknown root keys
                deepMerge(mergedRoot, json)

                val notes = json.optJSONObject("notes") ?: continue
                val keys = notes.keys()
                while (keys.hasNext()) {
                    val pathKey = keys.next()
                    val candidateEntry = notes.getJSONObject(pathKey)
                    val existingEntry = mergedNotesObj.optJSONObject(pathKey)

                    if (existingEntry == null) {
                        mergedNotesObj.put(pathKey, candidateEntry)
                    } else {
                        val candUpdated = candidateEntry.optLong("updatedAt", 0L)
                        val existUpdated = existingEntry.optLong("updatedAt", 0L)
                        if (candUpdated >= existUpdated) {
                            deepMerge(existingEntry, candidateEntry)
                            mergedNotesObj.put(pathKey, existingEntry)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        mergedRoot.put("schemaVersion", maxVersion)
        mergedRoot.put("notes", mergedNotesObj)

        try {
            val formatted = mergedRoot.toString(2)
            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(formatted); it.flush() }
            }
        } catch (_: Exception) {}
    }
}
