package com.example.data.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class VaultInfo(
    val name: String,
    val uriString: String
)

data class VaultState(
    val currentVault: VaultInfo? = null,
    val isAccessible: Boolean = false,
    val recentVaults: List<VaultInfo> = emptyList()
)

class VaultManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("kothapad_vault_storage", Context.MODE_PRIVATE)

    private val _vaultState = MutableStateFlow(loadInitialState())
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    val currentVault: StateFlow<VaultState> get() = vaultState

    companion object {
        private const val KEY_CURRENT_VAULT_URI = "current_vault_uri"
        private const val KEY_CURRENT_VAULT_NAME = "current_vault_name"
        private const val KEY_RECENT_VAULTS = "recent_vaults"
        const val RESTRICTED_FOLDER_HINT = "Create or choose a sub-folder (e.g. Documents/KothaVault)"

        fun sanitizeFilename(name: String, fallback: String = "Untitled"): String {
            val sanitized = name.replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_")
                .trim()
                .trimEnd('.')
            return if (sanitized.isBlank()) fallback else sanitized
        }
    }

    private fun loadInitialState(): VaultState {
        val uriStr = prefs.getString(KEY_CURRENT_VAULT_URI, null)
        val name = prefs.getString(KEY_CURRENT_VAULT_NAME, null)
        val recents = loadRecentVaults()
        if (uriStr != null && name != null) {
            val uri = Uri.parse(uriStr)
            val accessible = isUriAccessible(uri)
            return VaultState(
                currentVault = VaultInfo(name = name, uriString = uriStr),
                isAccessible = accessible,
                recentVaults = recents
            )
        }
        return VaultState(
            currentVault = null,
            isAccessible = false,
            recentVaults = recents
        )
    }

    fun isUriAccessible(uri: Uri): Boolean {
        return try {
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission && it.isWritePermission
            }
            if (!hasPerm) {
                // If not in persisted, check if DocumentFile can read & write
                val doc = DocumentFile.fromTreeUri(context, uri)
                doc != null && doc.exists() && doc.canRead() && doc.canWrite()
            } else {
                val doc = DocumentFile.fromTreeUri(context, uri)
                doc != null && doc.exists() && doc.canRead() && doc.canWrite()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isRestrictedFolder(uri: Uri): Boolean {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri) ?: ""
            // On Android 11+ (API 30+), root of storage e.g. "primary:" or "primary:Download"
            docId.trim() == "primary:" ||
                    docId.endsWith(":") ||
                    docId.equals("primary:Download", ignoreCase = true) ||
                    docId.startsWith("primary:Android/data") ||
                    docId.startsWith("primary:Android/obb")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun openVaultFromUri(treeUri: Uri, customName: String? = null): Result<VaultInfo> = withContext(Dispatchers.IO) {
        try {
            if (isRestrictedFolder(treeUri)) {
                return@withContext Result.failure(IllegalArgumentException(RESTRICTED_FOLDER_HINT))
            }

            takePersistableUriPermission(treeUri)

            val docFile = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open folder as DocumentFile"))

            if (!docFile.exists() || !docFile.canRead() || !docFile.canWrite()) {
                return@withContext Result.failure(IllegalStateException("Selected folder is not writable. $RESTRICTED_FOLDER_HINT"))
            }

            val resolvedName = customName?.takeIf { it.isNotBlank() }
                ?: docFile.name?.takeIf { it.isNotBlank() }
                ?: "KothaVault"

            val info = VaultInfo(name = resolvedName, uriString = treeUri.toString())
            setCurrentVault(info)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNewVaultInParent(parentTreeUri: Uri, vaultName: String): Result<VaultInfo> = withContext(Dispatchers.IO) {
        try {
            if (isRestrictedFolder(parentTreeUri)) {
                return@withContext Result.failure(IllegalArgumentException(RESTRICTED_FOLDER_HINT))
            }

            takePersistableUriPermission(parentTreeUri)

            val parentDoc = DocumentFile.fromTreeUri(context, parentTreeUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open parent folder"))

            if (!parentDoc.exists() || !parentDoc.canWrite()) {
                return@withContext Result.failure(IllegalStateException("Parent folder is not writable. $RESTRICTED_FOLDER_HINT"))
            }

            val cleanVaultName = sanitizeFilename(vaultName, "KothaVault")
            val subDir = createDir(parentDoc, cleanVaultName)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create subfolder '$cleanVaultName'"))

            val subDocId = DocumentsContract.getDocumentId(subDir.uri)
            val subTreeUri = DocumentsContract.buildTreeDocumentUri(parentTreeUri.authority, subDocId)

            // Try to take permission on the subfolder tree as well
            takePersistableUriPermission(subTreeUri)

            val info = VaultInfo(name = cleanVaultName, uriString = subTreeUri.toString())
            setCurrentVault(info)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun takePersistableUriPermission(uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
            // Already taken or not persistable
        }
    }

    fun setCurrentVault(info: VaultInfo) {
        val accessible = isUriAccessible(Uri.parse(info.uriString))
        prefs.edit()
            .putString(KEY_CURRENT_VAULT_URI, info.uriString)
            .putString(KEY_CURRENT_VAULT_NAME, info.name)
            .apply()

        val updatedRecents = addToRecentVaults(info)
        _vaultState.value = VaultState(
            currentVault = info,
            isAccessible = accessible,
            recentVaults = updatedRecents
        )
    }

    fun closeCurrentVault() {
        prefs.edit()
            .remove(KEY_CURRENT_VAULT_URI)
            .remove(KEY_CURRENT_VAULT_NAME)
            .apply()

        _vaultState.value = _vaultState.value.copy(
            currentVault = null,
            isAccessible = false
        )
    }

    fun refreshAccessibility(): Boolean {
        val curr = _vaultState.value.currentVault ?: return false
        val accessible = isUriAccessible(Uri.parse(curr.uriString))
        if (_vaultState.value.isAccessible != accessible) {
            _vaultState.value = _vaultState.value.copy(isAccessible = accessible)
        }
        return accessible
    }

    fun getCurrentVaultDocumentFile(): DocumentFile? {
        val uriStr = _vaultState.value.currentVault?.uriString ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val doc = DocumentFile.fromTreeUri(context, uri)
            if (doc != null && doc.exists() && doc.canRead() && doc.canWrite()) doc else null
        } catch (e: Exception) {
            null
        }
    }

    private fun loadRecentVaults(): List<VaultInfo> {
        val json = prefs.getString(KEY_RECENT_VAULTS, null) ?: return emptyList()
        val list = mutableListOf<VaultInfo>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name")
                val uri = obj.optString("uri")
                if (name.isNotBlank() && uri.isNotBlank()) {
                    list.add(VaultInfo(name, uri))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun addToRecentVaults(info: VaultInfo): List<VaultInfo> {
        val current = loadRecentVaults().toMutableList()
        current.removeAll { it.uriString == info.uriString }
        current.add(0, info)
        // Keep up to 10 recent vaults
        val trimmed = if (current.size > 10) current.subList(0, 10) else current

        val arr = JSONArray()
        for (v in trimmed) {
            val obj = JSONObject()
            obj.put("name", v.name)
            obj.put("uri", v.uriString)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_VAULTS, arr.toString()).apply()
        return trimmed
    }

    fun removeFromRecentVaults(uriString: String) {
        val current = loadRecentVaults().toMutableList()
        current.removeAll { it.uriString == uriString }
        val arr = JSONArray()
        for (v in current) {
            val obj = JSONObject()
            obj.put("name", v.name)
            obj.put("uri", v.uriString)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_VAULTS, arr.toString()).apply()
        _vaultState.value = _vaultState.value.copy(recentVaults = current)
    }

    // =========================================================================
    // Reusable, Safe IO Helpers on DocumentFile (Off Main Thread)
    // =========================================================================

    suspend fun listChildren(dir: DocumentFile): List<DocumentFile> = withContext(Dispatchers.IO) {
        try {
            if (!dir.exists() || !dir.isDirectory) emptyList()
            else dir.listFiles().toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readText(file: DocumentFile): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun writeText(file: DocumentFile, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                stream.bufferedWriter().apply {
                    write(text)
                    flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun writeTextAtomic(
        parentDir: DocumentFile,
        fileName: String,
        text: String,
        mimeType: String = "application/json"
    ): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val cleanName = sanitizeFilename(fileName)
            val tempName = "$cleanName.tmp_${System.currentTimeMillis()}"
            val tempFile = parentDir.createFile(mimeType, tempName) ?: return@withContext null

            val writeOk = writeText(tempFile, text)
            if (!writeOk) {
                tempFile.delete()
                return@withContext null
            }

            val existingFile = parentDir.findFile(cleanName)
            if (existingFile != null && existingFile.exists()) {
                val bakName = "$cleanName.bak"
                val existingBak = parentDir.findFile(bakName)
                if (existingBak != null && existingBak.exists()) {
                    existingBak.delete()
                }
                existingFile.renameTo(bakName)
            }

            val renameOk = tempFile.renameTo(cleanName)
            if (renameOk) {
                parentDir.findFile(cleanName) ?: tempFile
            } else {
                // If renameTo returned false, fallback to direct write on target or return tempFile
                val target = parentDir.findFile(cleanName) ?: parentDir.createFile(mimeType, cleanName)
                if (target != null) {
                    writeText(target, text)
                    tempFile.delete()
                    target
                } else {
                    tempFile
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createFile(
        parentDir: DocumentFile,
        displayName: String,
        mimeType: String = "text/plain"
    ): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val cleanName = sanitizeFilename(displayName)
            parentDir.createFile(mimeType, cleanName)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createDir(
        parentDir: DocumentFile,
        displayName: String
    ): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            val cleanName = sanitizeFilename(displayName)
            val existing = parentDir.findFile(cleanName)
            if (existing != null && existing.isDirectory) {
                return@withContext existing
            }
            parentDir.createDirectory(cleanName)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun rename(file: DocumentFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanName = sanitizeFilename(newName)
            file.renameTo(cleanName)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun move(
        source: DocumentFile,
        sourceParent: DocumentFile,
        targetParent: DocumentFile
    ): DocumentFile? = withContext(Dispatchers.IO) {
        try {
            // Attempt DocumentsContract.moveDocument if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val movedUri = DocumentsContract.moveDocument(
                        context.contentResolver,
                        source.uri,
                        sourceParent.uri,
                        targetParent.uri
                    )
                    if (movedUri != null) {
                        return@withContext DocumentFile.fromSingleUri(context, movedUri)
                    }
                } catch (_: Exception) {
                    // Fallback to copy + delete
                }
            }

            // Fallback: Copy stream to new file, then delete original
            val fileName = source.name ?: "file"
            val mimeType = source.type ?: "application/octet-stream"
            val targetFile = targetParent.createFile(mimeType, fileName) ?: return@withContext null

            val copySuccess = context.contentResolver.openInputStream(source.uri)?.use { input ->
                context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    input.copyTo(output)
                    output.flush()
                    true
                } ?: false
            } ?: false

            if (copySuccess) {
                source.delete()
                targetFile
            } else {
                targetFile.delete()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun delete(file: DocumentFile): Boolean = withContext(Dispatchers.IO) {
        try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exists(file: DocumentFile): Boolean = withContext(Dispatchers.IO) {
        try {
            file.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resolveDuplicateFileName(parent: DocumentFile, proposedName: String, extension: String = ""): String = withContext(Dispatchers.IO) {
        val dotExt = if (extension.isNotBlank()) {
            if (extension.startsWith(".")) extension else ".$extension"
        } else ""
        val cleanBase = sanitizeFilename(proposedName.removeSuffix(dotExt))
        var candidate = "$cleanBase$dotExt"
        var counter = 1
        val existingNames = listChildren(parent).mapNotNull { it.name }.toSet()
        while (existingNames.contains(candidate)) {
            candidate = "$cleanBase $counter$dotExt"
            counter++
        }
        candidate
    }
}
