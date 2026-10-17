package com.example.data.vault

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.Note
import com.example.data.model.Stamp
import com.example.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class NoteMetadata(
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val languageCode: String? = null,
    val highlightColor: String? = null,
    val fontSizeModifier: Float = 0f,
    val tags: String = "",
    val sessionDurationSec: Long = 0L
)

data class WorkspaceData(
    val lastOpenedNoteRelPath: String? = null,
    val lastActiveFolder: String = "General",
    val expandedFolders: Set<String> = setOf("General", "Templates")
)

data class CachedNoteEntry(
    val note: Note,
    val file: DocumentFile,
    val parentDir: DocumentFile,
    val relPath: String,
    val lastModified: Long
)

data class ExternalNoteChange(
    val note: Note,
    val diskContent: String,
    val lastModified: Long
)

class VaultNoteRepository(
    private val context: Context,
    private val vaultManager: VaultManager,
    private val preferencesManager: PreferencesManager,
    private val repositoryScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val vaultConfigStore: VaultConfigStore = VaultConfigStore.getInstance(context, repositoryScope)
) {

    private val mutex = Mutex()

    // Primary StateFlows matching NoteRepository API
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    private val _activeNotes = MutableStateFlow<List<Note>>(emptyList())
    val activeNotes: StateFlow<List<Note>> = _activeNotes.asStateFlow()

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    val archivedNotes: StateFlow<List<Note>> = _archivedNotes.asStateFlow()

    private val _trashedNotes = MutableStateFlow<List<Note>>(emptyList())
    val trashedNotes: StateFlow<List<Note>> = _trashedNotes.asStateFlow()

    // Vault Scanner & State
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow<String?>(null)
    val scanProgress: StateFlow<String?> = _scanProgress.asStateFlow()

    private val _lastSaveError = MutableStateFlow<String?>(null)
    val lastSaveError: StateFlow<String?> = _lastSaveError.asStateFlow()

    // External change detection
    private val _externalNoteChange = MutableStateFlow<ExternalNoteChange?>(null)
    val externalNoteChange: StateFlow<ExternalNoteChange?> = _externalNoteChange.asStateFlow()

    var currentlyOpenNoteId: Long? = null

    fun clearExternalNoteChange() {
        _externalNoteChange.value = null
    }

    // Internal In-Memory Index
    // relPath -> CachedNoteEntry
    private val cache = ConcurrentHashMap<String, CachedNoteEntry>()
    // relPath <-> ID mapping
    private val pathToIdMap = ConcurrentHashMap<String, Long>()
    private val idToPathMap = ConcurrentHashMap<Long, String>()
    private val idGenerator = AtomicLong(1000L)

    // Metadata Store: relPath -> NoteMetadata
    private val metadataMap = ConcurrentHashMap<String, NoteMetadata>()

    // Current Vault Reference
    private var currentVaultDoc: DocumentFile? = null
    private var scanJob: Job? = null

    // Workspace Cache
    private var workspaceData: WorkspaceData = WorkspaceData()

    companion object {
        const val KOTHAPAD_DIR = VaultConfigStore.CONFIG_DIR_NAME
        const val NOTES_META_FILE = VaultConfigStore.FILE_NOTES_META
        const val WORKSPACE_FILE = VaultConfigStore.FILE_WORKSPACE
        const val TEMPLATES_DIR = "Templates"
        const val GENERAL_DIR = "General"
        private const val META_SCHEMA_VERSION = 1
        private const val WORKSPACE_SCHEMA_VERSION = 1
    }

    private fun getOrCreateIdForPath(relPath: String): Long {
        return pathToIdMap.computeIfAbsent(relPath) {
            val newId = idGenerator.incrementAndGet()
            idToPathMap[newId] = relPath
            newId
        }
    }

    private fun updateIdPath(oldRelPath: String, newRelPath: String) {
        val id = pathToIdMap.remove(oldRelPath)
        if (id != null) {
            pathToIdMap[newRelPath] = id
            idToPathMap[id] = newRelPath
        } else {
            getOrCreateIdForPath(newRelPath)
        }
    }

    private fun removeIdForPath(relPath: String) {
        val id = pathToIdMap.remove(relPath)
        if (id != null) {
            idToPathMap.remove(id)
        }
    }

    // =========================================================================
    // Vault Initialization & Rescan
    // =========================================================================

    suspend fun onVaultOpened(vaultDoc: DocumentFile) = withContext(Dispatchers.IO) {
        currentVaultDoc = vaultDoc
        cache.clear()
        metadataMap.clear()
        pathToIdMap.clear()
        idToPathMap.clear()

        // Single source of truth: initialize vault config and cleanup duplicates
        vaultConfigStore.initVault(vaultDoc)

        // Ensure critical note content folders exist
        vaultManager.getOrCreateDirRecursive(vaultDoc, TEMPLATES_DIR)
        vaultManager.getOrCreateDirRecursive(vaultDoc, GENERAL_DIR)

        // Load metadata & workspace via VaultConfigStore
        loadMetadata(vaultDoc)
        loadWorkspace(vaultDoc)

        // Start background indexing
        startVaultScan(vaultDoc)
    }

    fun startVaultScan(vaultDoc: DocumentFile) {
        scanJob?.cancel()
        scanJob = repositoryScope.launch {
            _isScanning.value = true
            _scanProgress.value = "Indexing notes..."
            try {
                scanVaultRecursive(vaultDoc)
            } finally {
                _isScanning.value = false
                _scanProgress.value = null
            }
        }
    }

    suspend fun rescanIfNeeded() = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext
        if (_isScanning.value) return@withContext
        startVaultScan(vaultDoc)
    }

    suspend fun rebuildIndex() = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext
        scanJob?.cancel()
        cache.clear()
        metadataMap.clear()
        pathToIdMap.clear()
        idToPathMap.clear()
        loadMetadata(vaultDoc)
        loadWorkspace(vaultDoc)
        startVaultScan(vaultDoc)
    }

    private suspend fun scanVaultRecursive(vaultDoc: DocumentFile) = withContext(Dispatchers.IO) {
        val discoveredEntries = mutableListOf<CachedNoteEntry>()
        val foundPaths = mutableSetOf<String>()
        val discoveredFolders = mutableSetOf<String>()
        var count = 0

        suspend fun traverse(dir: DocumentFile, currentRelPath: String) {
            val children = vaultManager.listChildren(dir)
            for (child in children) {
                val name = child.name ?: continue
                // CRITICAL RULE: Ignore every hidden file/folder (names starting with ".")
                // Never read/modify/delete ".obsidian", ".kothapad", or any other hidden folder
                if (name.startsWith(".")) {
                    continue
                }

                if (child.isDirectory) {
                    // Templates folder is reserved for template files (stamps)
                    if (currentRelPath.isEmpty() && name.equals(TEMPLATES_DIR, ignoreCase = true)) {
                        continue
                    }
                    val nextRel = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
                    discoveredFolders.add(nextRel)
                    traverse(child, nextRel)
                } else if (child.isFile && name.endsWith(".md", ignoreCase = true)) {
                    val fileRelPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
                    foundPaths.add(fileRelPath)
                    count++

                    if (count % 30 == 0) {
                        _scanProgress.value = "Indexed $count notes..."
                    }

                    val baseTitle = name.removeSuffix(".md")
                    val folderName = if (currentRelPath.isEmpty()) GENERAL_DIR else currentRelPath
                    val lastMod = child.lastModified()

                    // Check existing cache
                    val existing = cache[fileRelPath]
                    if (existing != null && existing.lastModified == lastMod) {
                        discoveredEntries.add(existing)
                    } else {
                        // Read markdown content
                        val content = vaultManager.readText(child)
                        val meta = metadataMap[fileRelPath] ?: NoteMetadata(
                            createdAt = if (lastMod > 0) lastMod else System.currentTimeMillis(),
                            updatedAt = if (lastMod > 0) lastMod else System.currentTimeMillis()
                        )
                        metadataMap[fileRelPath] = meta

                        val id = getOrCreateIdForPath(fileRelPath)
                        val note = Note(
                            id = id,
                            title = baseTitle,
                            content = content,
                            createdAt = meta.createdAt,
                            updatedAt = meta.updatedAt,
                            isPinned = meta.isPinned,
                            languageCode = meta.languageCode,
                            highlightColor = meta.highlightColor,
                            fontSizeModifier = meta.fontSizeModifier,
                            folder = folderName,
                            tags = meta.tags,
                            isArchived = meta.isArchived,
                            isDeleted = meta.isDeleted,
                            deletedAt = meta.deletedAt,
                            sessionDurationSec = meta.sessionDurationSec
                        )

                        // Check if this is the currently open note and content changed externally
                        if (existing != null && id == currentlyOpenNoteId && existing.note.content != content) {
                            _externalNoteChange.value = ExternalNoteChange(
                                note = note,
                                diskContent = content,
                                lastModified = lastMod
                            )
                        }

                        val entry = CachedNoteEntry(
                            note = note,
                            file = child,
                            parentDir = dir,
                            relPath = fileRelPath,
                            lastModified = lastMod
                        )
                        cache[fileRelPath] = entry
                        discoveredEntries.add(entry)
                    }
                }
            }
        }

        traverse(vaultDoc, "")

        // Remove cached items no longer on disk
        val removedKeys = cache.keys.filter { !foundPaths.contains(it) }
        for (r in removedKeys) {
            cache.remove(r)
            removeIdForPath(r)
            metadataMap.remove(r)
        }

        // Clean up orphaned metadata keys
        val orphanMeta = metadataMap.keys.filter { !foundPaths.contains(it) }
        for (orphan in orphanMeta) {
            metadataMap.remove(orphan)
        }

        // Sync discovered real folders to customFolders so they appear in Notes Manager
        try {
            val existingCustom: Set<String> = preferencesManager.customFolders.first()
            val mergedCustom: Set<String> = (existingCustom + discoveredFolders).filter {
                !it.equals(GENERAL_DIR, ignoreCase = true) && !it.equals(TEMPLATES_DIR, ignoreCase = true)
            }.toSet()
            if (mergedCustom != existingCustom) {
                preferencesManager.setCustomFolders(mergedCustom)
            }
        } catch (_: Exception) {}

        // If vault is completely brand-new / empty (0 markdown notes), create an initial welcome note
        if (discoveredEntries.isEmpty()) {
            val initialNote = Note(
                title = "Welcome to KothaPad",
                content = "# Welcome to KothaPad\n\nYour notes are stored as standard Markdown (`.md`) files inside your Vault.\n\n- Real files and folders\n- Obsidian-compatible\n- Clean Markdown format\n- Dictation and voice commands supported",
                folder = GENERAL_DIR
            )
            val createdId = insertInternal(initialNote, vaultDoc)
            val createdNote = getNoteOnce(createdId)
            if (createdNote != null) {
                discoveredEntries.add(cache[getRelPathForNote(createdNote)]!!)
            }
        }

        // Persist updated metadata
        saveMetadata()
        recomputeFlows()
    }

    private fun recomputeFlows() {
        val list = cache.values.map { it.note }.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
        _allNotes.value = list
        _activeNotes.value = list.filter { !it.isDeleted && !it.isArchived }
        _archivedNotes.value = list.filter { !it.isDeleted && it.isArchived }
        _trashedNotes.value = list.filter { it.isDeleted }
    }

    // =========================================================================
    // Metadata Persistence (.kothapad/notes-meta.json)
    // =========================================================================

    private suspend fun loadMetadata(vaultDoc: DocumentFile) = withContext(Dispatchers.IO) {
        try {
            val root = vaultConfigStore.readNotesMeta(vaultDoc) ?: return@withContext
            val notesObj = root.optJSONObject("notes") ?: return@withContext

            val keys = notesObj.keys()
            while (keys.hasNext()) {
                val relPath = keys.next()
                val obj = notesObj.optJSONObject(relPath) ?: continue
                val meta = NoteMetadata(
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.optLong("deletedAt") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    languageCode = if (obj.has("languageCode") && !obj.isNull("languageCode")) obj.optString("languageCode") else null,
                    highlightColor = if (obj.has("highlightColor") && !obj.isNull("highlightColor")) obj.optString("highlightColor") else null,
                    fontSizeModifier = obj.optDouble("fontSizeModifier", 0.0).toFloat(),
                    tags = obj.optString("tags", ""),
                    sessionDurationSec = obj.optLong("sessionDurationSec", 0L)
                )
                metadataMap[relPath] = meta
            }
        } catch (_: Exception) {
            // Safe tolerant fallback: malformed metadata is safely ignored
        }
    }

    suspend fun saveMetadata() = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext
        try {
            val root = JSONObject()
            root.put("schemaVersion", META_SCHEMA_VERSION)
            val notesObj = JSONObject()
            for ((path, meta) in metadataMap) {
                val obj = JSONObject()
                obj.put("isPinned", meta.isPinned)
                obj.put("isArchived", meta.isArchived)
                obj.put("isDeleted", meta.isDeleted)
                if (meta.deletedAt != null) obj.put("deletedAt", meta.deletedAt)
                obj.put("createdAt", meta.createdAt)
                obj.put("updatedAt", meta.updatedAt)
                if (meta.languageCode != null) obj.put("languageCode", meta.languageCode)
                if (meta.highlightColor != null) obj.put("highlightColor", meta.highlightColor)
                obj.put("fontSizeModifier", meta.fontSizeModifier.toDouble())
                obj.put("tags", meta.tags)
                obj.put("sessionDurationSec", meta.sessionDurationSec)
                notesObj.put(path, obj)
            }
            root.put("notes", notesObj)
            vaultConfigStore.updateNotesMeta(vaultDoc, root)
        } catch (_: Exception) {
            // Keep going, non-blocking
        }
    }

    // =========================================================================
    // Workspace Persistence (.kothapad/workspace.json)
    // =========================================================================

    private suspend fun loadWorkspace(vaultDoc: DocumentFile) = withContext(Dispatchers.IO) {
        try {
            val root = vaultConfigStore.readWorkspace(vaultDoc) ?: return@withContext

            val lastNote = if (root.has("lastOpenedNoteRelPath") && !root.isNull("lastOpenedNoteRelPath")) {
                root.optString("lastOpenedNoteRelPath")
            } else null
            val lastFolder = root.optString("lastActiveFolder", GENERAL_DIR)

            val expSet = mutableSetOf<String>()
            val expArr = root.optJSONArray("expandedFolders")
            if (expArr != null) {
                for (i in 0 until expArr.length()) {
                    expSet.add(expArr.getString(i))
                }
            } else {
                expSet.addAll(listOf(GENERAL_DIR, TEMPLATES_DIR))
            }

            workspaceData = WorkspaceData(
                lastOpenedNoteRelPath = lastNote,
                lastActiveFolder = lastFolder,
                expandedFolders = expSet
            )
        } catch (_: Exception) {
            // Tolerant fallback
        }
    }

    suspend fun saveWorkspace(
        lastOpenedRelPath: String? = null,
        lastFolder: String? = null,
        expandedFolders: Set<String>? = null
    ) = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext

        val updatedLastNote = lastOpenedRelPath ?: workspaceData.lastOpenedNoteRelPath
        val updatedLastFolder = lastFolder ?: workspaceData.lastActiveFolder
        val updatedExp = expandedFolders ?: workspaceData.expandedFolders

        workspaceData = WorkspaceData(
            lastOpenedNoteRelPath = updatedLastNote,
            lastActiveFolder = updatedLastFolder,
            expandedFolders = updatedExp
        )

        try {
            val root = JSONObject()
            root.put("schemaVersion", WORKSPACE_SCHEMA_VERSION)
            if (updatedLastNote != null) {
                root.put("lastOpenedNoteRelPath", updatedLastNote)
            }
            root.put("lastActiveFolder", updatedLastFolder)
            val arr = JSONArray()
            for (f in updatedExp) {
                arr.put(f)
            }
            root.put("expandedFolders", arr)
            vaultConfigStore.updateWorkspace(vaultDoc, root)
        } catch (_: Exception) {
            // Non-blocking
        }
    }

    fun getWorkspaceData(): WorkspaceData = workspaceData

    // =========================================================================
    // NoteRepository Protocol Implementations (Routed to Vault)
    // =========================================================================

    fun getNote(id: Long): Flow<Note?> {
        return allNotes.map { notes -> notes.firstOrNull { it.id == id } }
    }

    suspend fun getNoteOnce(id: Long): Note? {
        return allNotes.value.firstOrNull { it.id == id }
    }

    suspend fun getLatestActiveNote(): Note? {
        val active = activeNotes.value
        return active.firstOrNull { it.isPinned } ?: active.firstOrNull()
    }

    suspend fun getLatestNote(): Note? {
        return allNotes.value.firstOrNull()
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        val q = query.trim()
        return allNotes.map { list ->
            if (q.isEmpty()) list
            else list.filter { note ->
                note.title.contains(q, ignoreCase = true) ||
                        note.content.contains(q, ignoreCase = true) ||
                        note.tags.contains(q, ignoreCase = true) ||
                        note.folder.contains(q, ignoreCase = true)
            }
        }
    }

    suspend fun insert(note: Note): Long = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext 0L
        mutex.withLock {
            insertInternal(note, vaultDoc)
        }
    }

    private suspend fun insertInternal(note: Note, vaultDoc: DocumentFile): Long {
        val targetFolder = if (note.folder.isBlank()) GENERAL_DIR else note.folder.trim()
        val targetDir = vaultManager.getOrCreateDirRecursive(vaultDoc, targetFolder) ?: vaultDoc

        val sanitizedTitle = VaultManager.sanitizeFilename(note.title.ifBlank { "Untitled Note" })
        val finalFileName = vaultManager.resolveDuplicateFileName(targetDir, sanitizedTitle, ".md")
        val cleanTitle = finalFileName.removeSuffix(".md")

        val file = vaultManager.writeTextAtomic(
            parentDir = targetDir,
            fileName = finalFileName,
            text = note.content,
            mimeType = "text/markdown"
        ) ?: return 0L

        val relPath = if (targetFolder.isEmpty() || targetFolder == ".") finalFileName else "$targetFolder/$finalFileName"
        val id = getOrCreateIdForPath(relPath)

        val meta = NoteMetadata(
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            isDeleted = note.isDeleted,
            deletedAt = note.deletedAt,
            createdAt = note.createdAt,
            updatedAt = System.currentTimeMillis(),
            languageCode = note.languageCode,
            highlightColor = note.highlightColor,
            fontSizeModifier = note.fontSizeModifier,
            tags = note.tags,
            sessionDurationSec = note.sessionDurationSec
        )
        metadataMap[relPath] = meta

        val createdNote = note.copy(
            id = id,
            title = cleanTitle,
            folder = targetFolder,
            updatedAt = meta.updatedAt
        )
        val entry = CachedNoteEntry(
            note = createdNote,
            file = file,
            parentDir = targetDir,
            relPath = relPath,
            lastModified = file.lastModified()
        )
        cache[relPath] = entry

        saveMetadata()
        saveWorkspace(lastOpenedRelPath = relPath, lastFolder = targetFolder)
        recomputeFlows()
        return id
    }

    suspend fun insertAll(notes: List<Note>): List<Long> = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext emptyList()
        val ids = mutableListOf<Long>()
        mutex.withLock {
            for (n in notes) {
                ids.add(insertInternal(n, vaultDoc))
            }
        }
        ids
    }

    suspend fun update(note: Note) = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext
        mutex.withLock {
            updateInternal(note, vaultDoc)
        }
    }

    private suspend fun updateInternal(note: Note, vaultDoc: DocumentFile) {
        val oldRelPath = idToPathMap[note.id]
        var entry = oldRelPath?.let { cache[it] }

        // If not in cache by ID mapping, search cache values by note.id
        if (entry == null) {
            entry = cache.values.firstOrNull { it.note.id == note.id }
        }

        // If not in cache by ID, search by title and folder
        if (entry == null) {
            val guessedRel1 = "${note.folder}/${note.title}.md".trimStart('/')
            val guessedRel2 = "${note.folder}/${note.title}".trimStart('/')
            val guessedRel3 = if (note.folder.equals(GENERAL_DIR, ignoreCase = true)) "${note.title}.md" else null
            entry = cache[guessedRel1] ?: cache[guessedRel2] ?: (guessedRel3?.let { cache[it] })
        }

        if (entry == null) {
            entry = cache.values.firstOrNull {
                it.note.title.equals(note.title, ignoreCase = true) &&
                it.note.folder.equals(note.folder, ignoreCase = true)
            }
        }

        if (entry == null) {
            if (note.isDeleted) {
                // If the note was already removed or cannot be found on disk, do NOT recreate or insert a duplicate file!
                return
            }
            // If completely missing, treat as insert
            insertInternal(note, vaultDoc)
            return
        }

        var currentFile = entry.file
        var currentParent = entry.parentDir
        var currentRelPath = entry.relPath
        val targetFolder = note.folder.trim().ifBlank { GENERAL_DIR }

        // 1. Check folder change (Move)
        val existingFolder = if (currentRelPath.contains("/")) currentRelPath.substringBeforeLast("/") else GENERAL_DIR
        if (!existingFolder.equals(targetFolder, ignoreCase = true)) {
            val newParentDir = vaultManager.getOrCreateDirRecursive(vaultDoc, targetFolder)
            if (newParentDir != null) {
                val movedFile = vaultManager.move(currentFile, currentParent, newParentDir)
                if (movedFile != null) {
                    currentFile = movedFile
                    currentParent = newParentDir
                    val fileName = movedFile.name ?: "${note.title}.md"
                    val newRel = "$targetFolder/$fileName"
                    metadataMap.remove(currentRelPath)?.let { metadataMap[newRel] = it }
                    cache.remove(currentRelPath)
                    updateIdPath(currentRelPath, newRel)
                    currentRelPath = newRel
                }
            }
        }

        // 2. Check title change (Rename)
        val expectedFileName = "${VaultManager.sanitizeFilename(note.title)}.md"
        if (currentFile.name != null && !currentFile.name.equals(expectedFileName, ignoreCase = true)) {
            val uniqueName = vaultManager.resolveDuplicateFileName(currentParent, note.title, ".md")
            val renamed = vaultManager.rename(currentFile, uniqueName)
            if (renamed) {
                val parentPath = if (currentRelPath.contains("/")) currentRelPath.substringBeforeLast("/") else ""
                val newRel = if (parentPath.isEmpty()) uniqueName else "$parentPath/$uniqueName"
                metadataMap.remove(currentRelPath)?.let { metadataMap[newRel] = it }
                cache.remove(currentRelPath)
                updateIdPath(currentRelPath, newRel)
                currentRelPath = newRel
            }
        }

        // Keep workspace.json in sync if this note was the last opened note or folder changed
        if (entry.relPath != currentRelPath) {
            if (workspaceData.lastOpenedNoteRelPath == entry.relPath) {
                saveWorkspace(lastOpenedRelPath = currentRelPath, lastFolder = targetFolder)
            }
        }

        // 3. Write markdown content atomically if changed
        if (entry.note.content != note.content) {
            val fileName = currentFile.name ?: "${VaultManager.sanitizeFilename(note.title)}.md"
            val writtenFile = vaultManager.writeTextAtomic(
                parentDir = currentParent,
                fileName = fileName,
                text = note.content,
                mimeType = "text/markdown"
            )
            if (writtenFile != null) {
                currentFile = writtenFile
            } else {
                _lastSaveError.value = "Failed to write note to disk"
            }
        }

        // 4. Update metadata in notes-meta.json
        val now = System.currentTimeMillis()
        val meta = NoteMetadata(
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            isDeleted = note.isDeleted,
            deletedAt = note.deletedAt,
            createdAt = note.createdAt,
            updatedAt = now,
            languageCode = note.languageCode,
            highlightColor = note.highlightColor,
            fontSizeModifier = note.fontSizeModifier,
            tags = note.tags,
            sessionDurationSec = note.sessionDurationSec
        )
        metadataMap[currentRelPath] = meta

        val updatedNote = note.copy(
            id = entry.note.id,
            title = currentFile.name?.removeSuffix(".md") ?: note.title,
            folder = targetFolder,
            updatedAt = now
        )
        val newEntry = CachedNoteEntry(
            note = updatedNote,
            file = currentFile,
            parentDir = currentParent,
            relPath = currentRelPath,
            lastModified = currentFile.lastModified()
        )
        cache[currentRelPath] = newEntry

        saveMetadata()
        recomputeFlows()
    }

    suspend fun updateAll(notes: List<Note>) = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext
        mutex.withLock {
            for (n in notes) {
                updateInternal(n, vaultDoc)
            }
        }
    }

    suspend fun delete(note: Note) = withContext(Dispatchers.IO) {
        mutex.withLock {
            deleteInternal(note)
        }
    }

    private suspend fun deleteInternal(note: Note) {
        val relPath = idToPathMap[note.id] ?: cache.entries.firstOrNull { it.value.note.id == note.id }?.key
        if (relPath != null) {
            val entry = cache[relPath]
            if (entry != null) {
                vaultManager.delete(entry.file)
                cache.remove(relPath)
                removeIdForPath(relPath)
                metadataMap.remove(relPath)
                if (workspaceData.lastOpenedNoteRelPath == relPath) {
                    saveWorkspace(lastOpenedRelPath = null)
                }
            }
        }
        saveMetadata()
        recomputeFlows()
    }

    suspend fun deleteAll(notes: List<Note>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            for (n in notes) {
                deleteInternal(n)
            }
        }
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        val note = getNoteOnce(id) ?: return@withContext
        delete(note)
    }

    suspend fun getAllNotesList(): List<Note> {
        return allNotes.value
    }

    suspend fun deleteAllNotes(): Int = withContext(Dispatchers.IO) {
        val list = allNotes.value
        deleteAll(list)
        list.size
    }

    suspend fun autoCleanTrash(olderThanTimestamp: Long): Int = withContext(Dispatchers.IO) {
        val toClean = trashedNotes.value.filter {
            val delAt = it.deletedAt
            delAt != null && delAt < olderThanTimestamp
        }
        deleteAll(toClean)
        toClean.size
    }

    suspend fun emptyTrash(): Int = withContext(Dispatchers.IO) {
        val trashed = trashedNotes.value
        deleteAll(trashed)
        trashed.size
    }

    // =========================================================================
    // Folder Operations on Disk
    // =========================================================================

    suspend fun createFolderOnDisk(folderPath: String): Boolean = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext false
        val dir = vaultManager.getOrCreateDirRecursive(vaultDoc, folderPath)
        dir != null
    }

    suspend fun renameFolderOnDisk(oldFolder: String, newFolder: String): Boolean = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext false
        val oldDir = vaultManager.findDirRecursive(vaultDoc, oldFolder) ?: return@withContext false
        val newClean = VaultManager.sanitizeFilename(newFolder.substringAfterLast("/"))
        val renamed = vaultManager.rename(oldDir, newClean)
        if (renamed) {
            // Re-index vault so all paths and metadata update smoothly
            scanVaultRecursive(vaultDoc)
        }
        renamed
    }

    suspend fun deleteFolderOnDisk(folder: String, deleteContainedNotes: Boolean): Boolean = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext false
        val dir = vaultManager.findDirRecursive(vaultDoc, folder) ?: return@withContext false
        if (!deleteContainedNotes) {
            // Move contained notes to General first
            val generalDir = vaultManager.getOrCreateDirRecursive(vaultDoc, GENERAL_DIR) ?: vaultDoc
            val children = vaultManager.listChildren(dir)
            for (ch in children) {
                if (ch.isFile && ch.name?.endsWith(".md") == true) {
                    vaultManager.move(ch, dir, generalDir)
                }
            }
        }
        val deleted = vaultManager.delete(dir)
        if (deleted) {
            scanVaultRecursive(vaultDoc)
        }
        deleted
    }

    // =========================================================================
    // Template Synchronization (<vault>/Templates/<template>.md)
    // =========================================================================

    suspend fun saveTemplateFile(template: Stamp): Boolean = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext false
        val templatesDir = vaultManager.getOrCreateDirRecursive(vaultDoc, TEMPLATES_DIR) ?: return@withContext false
        val cleanLabel = VaultManager.sanitizeFilename(template.label.ifBlank { "Template" })
        val fileName = "$cleanLabel.md"
        val written = vaultManager.writeTextAtomic(templatesDir, fileName, template.templateText, "text/markdown")
        written != null
    }

    suspend fun deleteTemplateFile(template: Stamp): Boolean = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext false
        val templatesDir = vaultManager.findDirRecursive(vaultDoc, TEMPLATES_DIR) ?: return@withContext false
        val fileName = "${VaultManager.sanitizeFilename(template.label)}.md"
        val file = templatesDir.findFile(fileName) ?: return@withContext false
        vaultManager.delete(file)
    }

    suspend fun syncTemplatesFromVault(
        existingTemplates: List<Stamp>,
        onNewTemplateDiscovered: suspend (Stamp) -> Unit
    ) = withContext(Dispatchers.IO) {
        val vaultDoc = currentVaultDoc ?: return@withContext
        val templatesDir = vaultManager.findDirRecursive(vaultDoc, TEMPLATES_DIR) ?: return@withContext
        val files = vaultManager.listChildren(templatesDir)
        var order = existingTemplates.size
        for (f in files) {
            if (f.isFile && f.name?.endsWith(".md") == true) {
                val label = f.name!!.removeSuffix(".md")
                if (existingTemplates.none { it.label.equals(label, ignoreCase = true) }) {
                    val content = vaultManager.readText(f)
                    val stamp = Stamp(
                        label = label,
                        templateText = content,
                        orderIndex = order++,
                        isPinned = false
                    )
                    onNewTemplateDiscovered(stamp)
                }
            }
        }
        // Bidirectional sync: make sure existing DB templates exist on disk
        for (st in existingTemplates) {
            val fileName = "${VaultManager.sanitizeFilename(st.label)}.md"
            if (templatesDir.findFile(fileName) == null) {
                saveTemplateFile(st)
            }
        }
    }

    fun getRelPathForNote(note: Note): String {
        return idToPathMap[note.id] ?: "${note.folder}/${note.title}.md".trimStart('/')
    }
}
