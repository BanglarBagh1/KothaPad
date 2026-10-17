package com.example.ui.notes

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.data.model.Stamp
import com.example.ui.dialogs.EditTemplateDialog
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.SaffronPrimary
import com.example.util.NoteTimeFormatter
import java.util.Date
import kotlin.math.ceil

enum class NoteSortOption(val displayName: String) {
    MODIFIED_DESC("Modified (Newest first)"),
    MODIFIED_ASC("Modified (Oldest first)"),
    CREATED_DESC("Created (Newest first)"),
    CREATED_ASC("Created (Oldest first)"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    WORD_COUNT_DESC("Word Count (Longest)")
}

enum class VaultView(val title: String) {
    ACTIVE("KothaPad Notes"),
    ARCHIVE("Archive"),
    TRASH("Trash")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotesManagerScreen(
    activeNotes: List<Note>,
    archivedNotes: List<Note>,
    trashedNotes: List<Note>,
    templates: List<Stamp>,
    currentNoteId: Long,
    trashAutoEmptyDays: Int,
    defaultFolder: String,
    showNoteMetrics: Boolean = true,
    noteMetricsStyle: String = "COMPACT",
    noteMetricsOrder: String = com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING,
    noteMetricsHiddenItems: Set<String> = emptySet(),
    expandedFolders: Set<String> = setOf("General", "Templates"),
    customFolders: Set<String> = emptySet(),
    onOpenSettings: () -> Unit = {},
    onToggleFolderExpanded: (String, Boolean) -> Unit = { _, _ -> },
    onSetAllFoldersExpanded: (Boolean, Set<String>) -> Unit = { _, _ -> },
    onAddCustomFolder: (String) -> Unit = {},
    onRenameFolder: (String, String) -> Unit = { _, _ -> },
    onDeleteFolder: (String, Boolean) -> Unit = { _, _ -> },
    onDuplicateNote: (Note) -> Unit = {},
    onRenameNote: (Note, String) -> Unit = { _, _ -> },
    onNoteSelect: (Note) -> Unit,
    askBeforeCreating: Boolean = false,
    askChooseTemplateToggle: Boolean = true,
    askStartRecordingToggle: Boolean = false,
    newNoteNamingRule: String = "UNTITLED",
    newNoteLocationMode: String = "LAST_WORKED_ON",
    availableFolders: List<String> = emptyList(),
    onNewNote: (folder: String?, template: Stamp?, customTitle: String?, startRecording: Boolean) -> Unit,
    onPinToggle: (Note) -> Unit,
    onTrashNote: (Note) -> Unit,
    onTrashNotes: (List<Note>) -> Unit,
    onRestoreNote: (Note) -> Unit,
    onPermanentlyDeleteNote: (Note) -> Unit,
    onPermanentlyDeleteNotes: (List<Note>) -> Unit,
    onEmptyTrash: () -> Unit,
    onArchiveToggle: (Note) -> Unit,
    onArchiveNotes: (List<Note>, Boolean) -> Unit,
    onMergeNotes: (List<Note>, String) -> Unit,
    onMoveNotesToFolder: (List<Note>, String) -> Unit,
    onAddTagsToNotes: (List<Note>, List<String>) -> Unit,
    onSaveTemplate: (Stamp) -> Unit,
    onDeleteTemplate: (Stamp) -> Unit,
    onPinTemplate: (Stamp) -> Unit,
    onReorderTemplates: (List<Stamp>) -> Unit,
    onUpdateTrashDays: (Int) -> Unit,
    onClose: () -> Unit,
    onReloadVault: () -> Unit = {},
    isVaultScanning: Boolean = false,
    vaultScanProgress: String? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // View mode: Active notes, Archive, Trash
    var currentVaultView by remember { mutableStateOf(VaultView.ACTIVE) }
    var isVaultMenuOpen by remember { mutableStateOf(false) }

    // Create Note Popup States
    var showCreateNotePopup by remember { mutableStateOf(false) }
    var popupTitleInput by remember { mutableStateOf("") }
    var popupSelectedFolder by remember { mutableStateOf(defaultFolder) }
    var popupSelectedTemplate by remember { mutableStateOf<Stamp?>(null) }
    var popupStartRecording by remember { mutableStateOf(false) }
    var popupShowFolderDropdown by remember { mutableStateOf(false) }
    var popupShowTemplateDropdown by remember { mutableStateOf(false) }
    var newFolderInputForPopup by remember { mutableStateOf("") }
    var showNewFolderInputInPopup by remember { mutableStateOf(false) }

    // Subfolder creation dialog states
    var showNewSubfolderDialog by remember { mutableStateOf(false) }
    var subfolderParentFolder by remember { mutableStateOf("") }
    var newSubfolderNameInput by remember { mutableStateOf("") }

    val handlePlusClick: (String?) -> Unit = { targetFolder ->
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (newNoteLocationMode == "ASK_POPUP" || askBeforeCreating || newNoteNamingRule == "ASK_ME") {
            popupTitleInput = when (newNoteNamingRule) {
                "DATE_TIME" -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                "DATE_ONLY" -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                else -> ""
            }
            popupSelectedFolder = targetFolder ?: defaultFolder
            popupSelectedTemplate = null
            popupStartRecording = false
            showNewFolderInputInPopup = false
            showCreateNotePopup = true
        } else {
            onNewNote(targetFolder ?: defaultFolder, null, null, false)
        }
    }

    // Search and Sort states
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(NoteSortOption.MODIFIED_DESC) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(setOf<Long>()) }

    // Dialog states
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    var folderToRename by remember { mutableStateOf<String?>(null) }
    var renameFolderInput by remember { mutableStateOf("") }

    var folderToDelete by remember { mutableStateOf<String?>(null) }
    var deleteFolderNotesChoice by remember { mutableStateOf(false) } // false = move to General, true = trash notes

    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var renameNoteInput by remember { mutableStateOf("") }

    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var targetMoveFolderInput by remember { mutableStateOf("") }

    var noteToDeleteConfirm by remember { mutableStateOf<Note?>(null) }

    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeTitleInput by remember { mutableStateOf("") }

    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var batchMoveFolderInput by remember { mutableStateOf("") }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<Stamp?>(null) }
    var isCreatingTemplate by remember { mutableStateOf(false) }

    // Compile complete set of folders for active vault
    val allActiveFolders = remember(activeNotes, customFolders) {
        val folders = linkedSetOf<String>()
        // General always first if present
        folders.add("General")
        // Custom folders created by user
        folders.addAll(customFolders)
        // Folders from active notes
        activeNotes.forEach { note ->
            val f = note.folder.trim()
            if (f.isNotBlank()) folders.add(f)
        }
        folders.toList()
    }

    val folderTree = remember(allActiveFolders) {
        buildFolderTree(allActiveFolders)
    }

    val allTreePaths = remember(allActiveFolders) {
        val paths = mutableSetOf<String>()
        allActiveFolders.forEach { f ->
            val segs = f.split("/").filter { it.isNotBlank() }
            var p = ""
            segs.forEach { s ->
                p = if (p.isEmpty()) s else "$p/$s"
                paths.add(p)
            }
        }
        paths
    }

    // Filter notes based on current vault view and search query
    val sourceNotes = when (currentVaultView) {
        VaultView.ACTIVE -> activeNotes
        VaultView.ARCHIVE -> archivedNotes
        VaultView.TRASH -> trashedNotes
    }

    val filteredNotes = remember(sourceNotes, searchQuery) {
        if (searchQuery.isBlank()) {
            sourceNotes
        } else {
            sourceNotes.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true) ||
                        note.tags.contains(searchQuery, ignoreCase = true) ||
                        note.folder.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filter templates for search if active
    val filteredTemplates = remember(templates, searchQuery) {
        if (searchQuery.isBlank()) {
            templates
        } else {
            templates.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.templateText.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Helper to sort notes in a folder
    fun getSortedNotesForFolder(notesInFolder: List<Note>): List<Note> {
        return when (sortOption) {
            NoteSortOption.MODIFIED_DESC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
            NoteSortOption.MODIFIED_ASC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.updatedAt })
            NoteSortOption.CREATED_DESC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt })
            NoteSortOption.CREATED_ASC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.createdAt })
            NoteSortOption.NAME_ASC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { getDisplayTitle(it).lowercase() })
            NoteSortOption.NAME_DESC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { getDisplayTitle(it).lowercase() })
            NoteSortOption.WORD_COUNT_DESC -> notesInFolder.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending {
                if (it.content.isBlank()) 0 else it.content.trim().split("\\s+".toRegex()).size
            })
        }
    }

    // Check if all folders are currently expanded
    val allFolderKeys = remember(allActiveFolders) {
        (allActiveFolders + "Templates").toSet()
    }
    val areAllExpanded = allFolderKeys.isNotEmpty() && allFolderKeys.all { expandedFolders.contains(it) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("fullscreen_notes_manager")
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -35f) {
                        change.consume()
                        onClose()
                    }
                }
            },
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text(
                            text = "${selectedNoteIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SaffronPrimary
                            )
                        )
                    } else {
                        // Obsidian-like header: Vault Title with dropdown chevron + live counts subtitle
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isVaultMenuOpen = true }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentVaultView.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch view",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            val fileCount = filteredNotes.size
                            val folderCount = allActiveFolders.size + 1 // + Templates
                            val subtitleText = when (currentVaultView) {
                                VaultView.ACTIVE -> "$fileCount files, $folderCount folders"
                                VaultView.ARCHIVE -> "$fileCount archived files"
                                VaultView.TRASH -> "$fileCount trashed files"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }

                        // Vault selection dropdown
                        DropdownMenu(
                            expanded = isVaultMenuOpen,
                            onDismissRequest = { isVaultMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = SaffronPrimary) },
                                text = { Text("KothaPad Notes (${activeNotes.size})") },
                                onClick = {
                                    currentVaultView = VaultView.ACTIVE
                                    isVaultMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                text = { Text("Archive (${archivedNotes.size})") },
                                onClick = {
                                    currentVaultView = VaultView.ARCHIVE
                                    isVaultMenuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Trash (${trashedNotes.size})") },
                                onClick = {
                                    currentVaultView = VaultView.TRASH
                                    isVaultMenuOpen = false
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                text = { Text("New Folder...") },
                                onClick = {
                                    newFolderNameInput = ""
                                    showNewFolderDialog = true
                                    isVaultMenuOpen = false
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isMultiSelectMode) {
                                isMultiSelectMode = false
                                selectedNoteIds = emptySet()
                            } else {
                                onClose()
                            }
                        },
                        modifier = Modifier.testTag("notes_manager_back_button")
                    ) {
                        Icon(
                            imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isMultiSelectMode) "Cancel selection" else "Back to note"
                        )
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            val allIds = filteredNotes.map { it.id }.toSet()
                            selectedNoteIds = if (selectedNoteIds.size == allIds.size) emptySet() else allIds
                        }) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Select all")
                        }
                    } else {
                        // Reload vault button
                        IconButton(
                            onClick = onReloadVault,
                            modifier = Modifier.testTag("notes_manager_reload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload vault",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Settings (gear) icon on the right side of the header
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("notes_manager_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Multi-Select Action Bar (Merge, Move, Tag, Archive, Delete)
            AnimatedVisibility(visible = isMultiSelectMode && selectedNoteIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmoledSurfaceVariant,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, AmoledBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedNotes = filteredNotes.filter { selectedNoteIds.contains(it.id) }

                        // Merge (available when 2 or more selected)
                        IconButton(
                            onClick = {
                                mergeTitleInput = "Merged: " + selectedNotes.take(2).joinToString(" & ") { it.title }
                                showMergeDialog = true
                            },
                            enabled = selectedNotes.size >= 2
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Merge,
                                    contentDescription = "Merge",
                                    tint = if (selectedNotes.size >= 2) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Merge", fontSize = 10.sp)
                            }
                        }

                        // Move to Folder
                        IconButton(onClick = {
                            batchMoveFolderInput = selectedNotes.firstOrNull()?.folder ?: "General"
                            showBatchMoveDialog = true
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = "Move",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Move", fontSize = 10.sp)
                            }
                        }

                        // Tag
                        IconButton(onClick = {
                            newTagInput = ""
                            showAddTagDialog = true
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = "Tag",
                                    tint = SaffronPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Tag", fontSize = 10.sp)
                            }
                        }

                        // Archive
                        if (currentVaultView == VaultView.ACTIVE) {
                            IconButton(onClick = {
                                onArchiveNotes(selectedNotes, true)
                                isMultiSelectMode = false
                                selectedNoteIds = emptySet()
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = "Archive",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Archive", fontSize = 10.sp)
                                }
                            }
                        }

                        // Delete / Trash
                        IconButton(onClick = {
                            if (currentVaultView == VaultView.TRASH) {
                                onPermanentlyDeleteNotes(selectedNotes)
                            } else {
                                onTrashNotes(selectedNotes)
                            }
                            isMultiSelectMode = false
                            selectedNoteIds = emptySet()
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Delete", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = {
                        handlePlusClick(defaultFolder)
                    },
                    containerColor = SaffronPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("notes_manager_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create new note")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Vault Background Indexing Progress Banner
            AnimatedVisibility(visible = isVaultScanning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = vaultScanProgress ?: "Indexing notes...",
                        style = MaterialTheme.typography.bodySmall,
                        color = SaffronPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = SaffronPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // QUICK-ACTION ICON ROW (Obsidian-mobile style, evenly spaced)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(0.5.dp, AmoledBorder.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. New note
                    IconButton(
                        onClick = {
                            handlePlusClick(defaultFolder)
                        },
                        modifier = Modifier.testTag("quick_action_new_note")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NoteAdd,
                            contentDescription = "New note",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 2. New folder
                    IconButton(
                        onClick = {
                            newFolderNameInput = ""
                            showNewFolderDialog = true
                        },
                        modifier = Modifier.testTag("quick_action_new_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreateNewFolder,
                            contentDescription = "New folder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3. Sort order
                    Box {
                        IconButton(
                            onClick = { isSortMenuOpen = true },
                            modifier = Modifier.testTag("quick_action_sort")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort order",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            NoteSortOption.values().forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt.displayName,
                                            fontWeight = if (opt == sortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (opt == sortOption) SaffronPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        sortOption = opt
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Expand / Collapse all toggle
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onSetAllFoldersExpanded(!areAllExpanded, allFolderKeys)
                        },
                        modifier = Modifier.testTag("quick_action_expand_toggle")
                    ) {
                        Icon(
                            imageVector = if (areAllExpanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                            contentDescription = if (areAllExpanded) "Collapse all" else "Expand all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 5. Search toggle
                    IconButton(
                        onClick = {
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) {
                                searchQuery = ""
                            }
                        },
                        modifier = Modifier.testTag("quick_action_search")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search notes",
                            tint = if (isSearchVisible || searchQuery.isNotEmpty()) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 6. Reload vault from disk
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onReloadVault()
                        },
                        modifier = Modifier.testTag("quick_action_reload")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload vault",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // SEARCH BAR (Toggleable, keeps full search across title, content, tags, folder)
            AnimatedVisibility(visible = isSearchVisible || searchQuery.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("notes_manager_search"),
                    placeholder = { Text("Search title, content, tags, folder...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SaffronPrimary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = AmoledBorder
                    )
                )
            }

            // MAIN AREA: VERTICAL FILE EXPLORER TREE (Supports Pull-To-Refresh)
            PullToRefreshBox(
                isRefreshing = isVaultScanning,
                onRefresh = onReloadVault,
                modifier = Modifier.fillMaxSize()
            ) {
                if (currentVaultView == VaultView.TRASH) {
                    // TRASH VIEW BANNER & LIST
                    TrashHeaderBanner(
                        trashedCount = trashedNotes.size,
                    trashAutoEmptyDays = trashAutoEmptyDays,
                    onEmptyTrashClick = { showEmptyTrashConfirmDialog = true },
                    onUpdateTrashDays = onUpdateTrashDays
                )

                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Trash is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(getSortedNotesForFolder(filteredNotes), key = { it.id }) { note ->
                            NoteTreeItem(
                                note = note,
                                isCurrentlyOpen = false,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = selectedNoteIds.contains(note.id),
                                showMetrics = showNoteMetrics,
                                metricsStyle = noteMetricsStyle,
                                noteMetricsOrder = noteMetricsOrder,
                                noteMetricsHiddenItems = noteMetricsHiddenItems,
                                isTrash = true,
                                onNoteClick = { },
                                onToggleSelect = {
                                    selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                },
                                onRestore = { onRestoreNote(note) },
                                onDeletePermanent = { onPermanentlyDeleteNote(note) }
                            )
                        }
                    }
                }
            } else if (currentVaultView == VaultView.ARCHIVE) {
                // ARCHIVE VIEW
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Archive is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(getSortedNotesForFolder(filteredNotes), key = { it.id }) { note ->
                            NoteTreeItem(
                                note = note,
                                isCurrentlyOpen = note.id == currentNoteId,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = selectedNoteIds.contains(note.id),
                                showMetrics = showNoteMetrics,
                                metricsStyle = noteMetricsStyle,
                                noteMetricsOrder = noteMetricsOrder,
                                noteMetricsHiddenItems = noteMetricsHiddenItems,
                                isArchive = true,
                                onNoteClick = {
                                    if (isMultiSelectMode) {
                                        selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    } else {
                                        onNoteSelect(note)
                                        onClose()
                                    }
                                },
                                onToggleSelect = {
                                    selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                },
                                onUnarchive = { onArchiveToggle(note) },
                                onDelete = { onTrashNote(note) }
                            )
                        }
                    }
                }
            } else {
                // ACTIVE UNIFIED FILE-EXPLORER TREE (Obsidian Style)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. "Templates" Folder (Pinned near top with distinct template icon)
                    item(key = "folder_Templates") {
                        val isExpanded = expandedFolders.contains("Templates")
                        FolderTreeHeader(
                            folderName = "Templates",
                            count = filteredTemplates.size,
                            isExpanded = isExpanded,
                            customIcon = "📋",
                            onToggleExpand = {
                                onToggleFolderExpanded("Templates", !isExpanded)
                            },
                            onNewNoteHere = {
                                editingTemplate = Stamp(
                                    label = "",
                                    templateText = "{cursor}",
                                    orderIndex = templates.size,
                                    isPinned = false
                                )
                                isCreatingTemplate = true
                            },
                            onRenameFolder = null,
                            onDeleteFolder = null
                        )
                    }

                    // Templates items when expanded
                    val isTemplatesExpanded = expandedFolders.contains("Templates")
                    if (isTemplatesExpanded) {
                        if (filteredTemplates.isEmpty()) {
                            item(key = "empty_templates") {
                                IndentedGuideLineContainer {
                                    Text(
                                        text = "No templates yet. Tap '+' to create.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(filteredTemplates, key = { _, t -> "template_${t.id}" }) { index, template ->
                                IndentedGuideLineContainer {
                                    TemplateTreeRow(
                                        template = template,
                                        onUse = {
                                            onNewNote(null, template, null, false)
                                            onClose()
                                        },
                                        onEdit = {
                                            editingTemplate = template
                                            isCreatingTemplate = false
                                        },
                                        onPin = { onPinTemplate(template) },
                                        onDelete = { onDeleteTemplate(template) },
                                        onMoveUp = if (index > 0) {
                                            {
                                                val mutable = templates.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                onReorderTemplates(mutable)
                                            }
                                        } else null,
                                        onMoveDown = if (index < templates.size - 1) {
                                            {
                                                val mutable = templates.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                onReorderTemplates(mutable)
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }



                    folderTree.forEach { rootNode ->
                        renderFolderTreeNode(
                            node = rootNode,
                            filteredNotes = filteredNotes,
                            expandedFolders = expandedFolders,
                            currentNoteId = currentNoteId,
                            isMultiSelectMode = isMultiSelectMode,
                            selectedNoteIds = selectedNoteIds,
                            showMetrics = showNoteMetrics,
                            noteMetricsStyle = noteMetricsStyle,
                            noteMetricsOrder = noteMetricsOrder,
                            noteMetricsHiddenItems = noteMetricsHiddenItems,
                            onToggleFolderExpanded = onToggleFolderExpanded,
                            handlePlusClick = handlePlusClick,
                            onCreateSubfolder = { parentPath ->
                                subfolderParentFolder = parentPath
                                newSubfolderNameInput = ""
                                showNewSubfolderDialog = true
                            },
                            onRenameFolder = { path, name ->
                                folderToRename = path
                                renameFolderInput = name
                            },
                            onDeleteFolder = { path ->
                                folderToDelete = path
                                deleteFolderNotesChoice = false
                            },
                            getSortedNotesForFolder = { notes -> getSortedNotesForFolder(notes) },
                            onNoteSelect = onNoteSelect,
                            onClose = onClose,
                            onPinToggle = onPinToggle,
                            onArchiveToggle = onArchiveToggle,
                            onDuplicateNote = onDuplicateNote,
                            onRenameNote = { note ->
                                noteToRename = note
                                renameNoteInput = note.title
                            },
                            onMoveNote = { note ->
                                noteToMove = note
                                targetMoveFolderInput = note.folder
                            },
                            onDeleteNoteConfirm = { note ->
                                noteToDeleteConfirm = note
                            },
                            onStartMultiSelect = { note ->
                                isMultiSelectMode = true
                                selectedNoteIds = setOf(note.id)
                            }
                        )
                    }


                    val rootNotes = filteredNotes.filter { note ->
                        val f = note.folder.trim()
                        f.isBlank() || (!allTreePaths.any { it.equals(f, ignoreCase = true) })
                    }
                    if (rootNotes.isNotEmpty()) {
                        val sortedRoot = getSortedNotesForFolder(rootNotes)
                        items(sortedRoot, key = { "root_${it.id}" }) { note ->
                            NoteTreeItem(
                                note = note,
                                isCurrentlyOpen = note.id == currentNoteId,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = selectedNoteIds.contains(note.id),
                                showMetrics = showNoteMetrics,
                                metricsStyle = noteMetricsStyle,
                                noteMetricsOrder = noteMetricsOrder,
                                noteMetricsHiddenItems = noteMetricsHiddenItems,
                                onNoteClick = {
                                    if (isMultiSelectMode) {
                                        selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    } else {
                                        onNoteSelect(note)
                                        onClose()
                                    }
                                },
                                onToggleSelect = {
                                    selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                },
                                onRename = {
                                    noteToRename = note
                                    renameNoteInput = note.title
                                },
                                onMoveToFolder = {
                                    noteToMove = note
                                    targetMoveFolderInput = note.folder
                                },
                                onPinToggle = { onPinToggle(note) },
                                onArchive = { onArchiveToggle(note) },
                                onDuplicate = { onDuplicateNote(note) },
                                onDelete = { noteToDeleteConfirm = note },
                                onStartMultiSelect = {
                                    isMultiSelectMode = true
                                    selectedNoteIds = setOf(note.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Create New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    placeholder = { Text("e.g. Work, Ideas, Study") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newFolderNameInput.trim()
                        if (trimmed.isNotBlank()) {
                            onAddCustomFolder(trimmed)
                        }
                        showNewFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 1b. Create Subfolder Dialog
    if (showNewSubfolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubfolderDialog = false },
            title = { Text("Create Subfolder in '$subfolderParentFolder'") },
            text = {
                OutlinedTextField(
                    value = newSubfolderNameInput,
                    onValueChange = { newSubfolderNameInput = it },
                    placeholder = { Text("e.g. Projects, Personal, Archive") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newSubfolderNameInput.trim().trim('/')
                        if (trimmed.isNotBlank()) {
                            val fullSubfolderPath = "$subfolderParentFolder/$trimmed"
                            onAddCustomFolder(fullSubfolderPath)
                            onToggleFolderExpanded(subfolderParentFolder, true)
                            onToggleFolderExpanded(fullSubfolderPath, true)
                        }
                        showNewSubfolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubfolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 2. Rename Folder Dialog
    folderToRename?.let { oldFolder ->
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = renameFolderInput,
                    onValueChange = { renameFolderInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameFolderInput.trim().trim('/')
                        if (trimmed.isNotBlank()) {
                            val newPath = if (oldFolder.contains("/")) {
                                val parent = oldFolder.substringBeforeLast('/')
                                "$parent/$trimmed"
                            } else trimmed
                            onRenameFolder(oldFolder, newPath)
                        }
                        folderToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 3. Delete Folder Dialog (asks what to do with contained notes)
    folderToDelete?.let { folder ->
        val count = activeNotes.count { it.folder.equals(folder, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder '$folder'?") },
            text = {
                Column {
                    Text("This folder contains $count notes. What would you like to do?")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteFolderNotesChoice = false }
                    ) {
                        RadioButton(
                            selected = !deleteFolderNotesChoice,
                            onClick = { deleteFolderNotesChoice = false },
                            colors = RadioButtonDefaults.colors(selectedColor = SaffronPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move notes to 'General'", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteFolderNotesChoice = true }
                    ) {
                        RadioButton(
                            selected = deleteFolderNotesChoice,
                            onClick = { deleteFolderNotesChoice = true },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move notes to Trash", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(folder, deleteFolderNotesChoice)
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // 4. Rename Note Dialog
    noteToRename?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToRename = null },
            title = { Text("Rename Note") },
            text = {
                OutlinedTextField(
                    value = renameNoteInput,
                    onValueChange = { renameNoteInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameNote(note, renameNoteInput)
                        noteToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 5. Move Single Note to Folder Dialog
    noteToMove?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToMove = null },
            title = { Text("Move Note to Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose or enter destination folder:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = targetMoveFolderInput,
                        onValueChange = { targetMoveFolderInput = it },
                        placeholder = { Text("Folder name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allActiveFolders.take(4).forEach { f ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (targetMoveFolderInput.equals(f, ignoreCase = true)) SaffronPrimary.copy(alpha = 0.2f) else AmoledSurfaceVariant,
                                border = BorderStroke(1.dp, if (targetMoveFolderInput.equals(f, ignoreCase = true)) SaffronPrimary else AmoledBorder),
                                modifier = Modifier.clickable { targetMoveFolderInput = f }
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 11.sp,
                                    color = if (targetMoveFolderInput.equals(f, ignoreCase = true)) SaffronPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = targetMoveFolderInput.trim().ifBlank { "General" }
                        onMoveNotesToFolder(listOf(note), folder)
                        noteToMove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToMove = null }) { Text("Cancel") }
            }
        )
    }

    // 6. Delete Note Confirmation Dialog
    noteToDeleteConfirm?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDeleteConfirm = null },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to move '${getDisplayTitle(note)}' to Trash?") },
            confirmButton = {
                Button(
                    onClick = {
                        onTrashNote(note)
                        noteToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    // 7. Multi-Select Batch Move Dialog
    if (showBatchMoveDialog) {
        AlertDialog(
            onDismissRequest = { showBatchMoveDialog = false },
            title = { Text("Move ${selectedNoteIds.size} Notes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose or enter destination folder:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = batchMoveFolderInput,
                        onValueChange = { batchMoveFolderInput = it },
                        placeholder = { Text("Folder name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = batchMoveFolderInput.trim().ifBlank { "General" }
                        val notesToMove = filteredNotes.filter { selectedNoteIds.contains(it.id) }
                        onMoveNotesToFolder(notesToMove, target)
                        showBatchMoveDialog = false
                        isMultiSelectMode = false
                        selectedNoteIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchMoveDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 8. Merge Dialog
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge Selected Notes") },
            text = {
                Column {
                    Text(
                        text = "This will merge ${selectedNoteIds.size} notes into one and move originals to Trash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mergeTitleInput,
                        onValueChange = { mergeTitleInput = it },
                        label = { Text("Merged Note Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notesToMerge = filteredNotes.filter { selectedNoteIds.contains(it.id) }
                        onMergeNotes(notesToMerge, mergeTitleInput)
                        showMergeDialog = false
                        isMultiSelectMode = false
                        selectedNoteIds = emptySet()
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 9. Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Tags to Notes") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("e.g. project, draft, important") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tagsList = newTagInput.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                        val notesToTag = filteredNotes.filter { selectedNoteIds.contains(it.id) }
                        if (tagsList.isNotEmpty()) {
                            onAddTagsToNotes(notesToTag, tagsList)
                        }
                        showAddTagDialog = false
                        isMultiSelectMode = false
                        selectedNoteIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Add Tags")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 12. Create Note Dialog (Ask Before Creating / Naming Rule Popup)
    if (showCreateNotePopup) {
        AlertDialog(
            onDismissRequest = { showCreateNotePopup = false },
            title = { Text("Create New Note") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = popupTitleInput,
                        onValueChange = { popupTitleInput = it },
                        label = { Text("Note Title (Optional)") },
                        placeholder = { Text("Enter title or leave blank") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_note_title_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Folder", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { popupShowFolderDropdown = !popupShowFolderDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = popupSelectedFolder)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = popupShowFolderDropdown,
                            onDismissRequest = { popupShowFolderDropdown = false }
                        ) {
                            val folders = (listOf("General", "Templates") + allActiveFolders + availableFolders).distinct()
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder) },
                                    onClick = {
                                        popupSelectedFolder = folder
                                        popupShowFolderDropdown = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null) },
                                text = { Text("Create new folder...") },
                                onClick = {
                                    showNewFolderInputInPopup = true
                                    popupShowFolderDropdown = false
                                }
                            )
                        }

                        if (showNewFolderInputInPopup) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newFolderInputForPopup,
                                    onValueChange = { newFolderInputForPopup = it },
                                    placeholder = { Text("Folder name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newFolderInputForPopup.isNotBlank()) {
                                            onAddCustomFolder(newFolderInputForPopup.trim())
                                            popupSelectedFolder = newFolderInputForPopup.trim()
                                            newFolderInputForPopup = ""
                                            showNewFolderInputInPopup = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }

                    if (askChooseTemplateToggle && templates.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Choose a Template", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { popupShowTemplateDropdown = !popupShowTemplateDropdown },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = popupSelectedTemplate?.label ?: "None (Blank Note)")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = popupShowTemplateDropdown,
                                onDismissRequest = { popupShowTemplateDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (Blank Note)") },
                                    onClick = {
                                        popupSelectedTemplate = null
                                        popupShowTemplateDropdown = false
                                    }
                                )
                                templates.forEach { tmpl ->
                                    DropdownMenuItem(
                                        text = { Text(tmpl.label) },
                                        onClick = {
                                            popupSelectedTemplate = tmpl
                                            popupShowTemplateDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (askStartRecordingToggle) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { popupStartRecording = !popupStartRecording }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Start recording immediately", style = MaterialTheme.typography.bodyMedium)
                            androidx.compose.material3.Switch(
                                checked = popupStartRecording,
                                onCheckedChange = { popupStartRecording = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = SaffronPrimary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onNewNote(
                            popupSelectedFolder,
                            popupSelectedTemplate,
                            if (popupTitleInput.isBlank()) null else popupTitleInput,
                            popupStartRecording
                        )
                        showCreateNotePopup = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier.testTag("create_note_confirm_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateNotePopup = false },
                    modifier = Modifier.testTag("create_note_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 10. Empty Trash Confirm Dialog
    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("This will permanently delete all trashed notes. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onEmptyTrash()
                        showEmptyTrashConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 11. Edit Template Dialog
    editingTemplate?.let { template ->
        EditTemplateDialog(
            template = template,
            isNew = isCreatingTemplate,
            onSave = { updated ->
                onSaveTemplate(updated)
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null }
        )
    }
}

// ==========================================
// TREE VIEW COMPONENTS (Obsidian Mobile Inspired)
// ==========================================

data class FolderTreeNode(
    val name: String,
    val fullPath: String,
    val depth: Int,
    val subfolders: MutableList<FolderTreeNode> = mutableListOf()
)

fun buildFolderTree(folderPaths: List<String>): List<FolderTreeNode> {
    val rootNodes = mutableListOf<FolderTreeNode>()
    val nodeMap = mutableMapOf<String, FolderTreeNode>()

    val validPaths = folderPaths.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    val allPaths = (listOf("General") + validPaths).distinct()

    for (rawPath in allPaths) {
        val segments = rawPath.split("/").map { it.trim() }.filter { it.isNotEmpty() }
        var currentPath = ""
        var parentNode: FolderTreeNode? = null

        for (i in segments.indices) {
            val segment = segments[i]
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"

            var node = nodeMap[currentPath]
            if (node == null) {
                node = FolderTreeNode(
                    name = segment,
                    fullPath = currentPath,
                    depth = i
                )
                nodeMap[currentPath] = node
                if (parentNode == null) {
                    if (rootNodes.none { it.fullPath.equals(currentPath, ignoreCase = true) }) {
                        rootNodes.add(node)
                    }
                } else {
                    if (parentNode.subfolders.none { it.fullPath.equals(currentPath, ignoreCase = true) }) {
                        parentNode.subfolders.add(node)
                    }
                }
            }
            parentNode = node
        }
    }

    rootNodes.sortWith(compareBy<FolderTreeNode> { if (it.fullPath.equals("General", ignoreCase = true)) "" else it.name.lowercase() })
    return rootNodes
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderFolderTreeNode(
    node: FolderTreeNode,
    filteredNotes: List<Note>,
    expandedFolders: Set<String>,
    currentNoteId: Long?,
    isMultiSelectMode: Boolean,
    selectedNoteIds: Set<Long>,
    showMetrics: Boolean,
    noteMetricsStyle: String,
    noteMetricsOrder: String,
    noteMetricsHiddenItems: Set<String>,
    onToggleFolderExpanded: (String, Boolean) -> Unit,
    handlePlusClick: (String?) -> Unit,
    onCreateSubfolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    getSortedNotesForFolder: (List<Note>) -> List<Note>,
    onNoteSelect: (Note) -> Unit,
    onClose: () -> Unit,
    onPinToggle: (Note) -> Unit,
    onArchiveToggle: (Note) -> Unit,
    onDuplicateNote: (Note) -> Unit,
    onRenameNote: (Note) -> Unit,
    onMoveNote: (Note) -> Unit,
    onDeleteNoteConfirm: (Note) -> Unit,
    onStartMultiSelect: (Note) -> Unit
) {
    val notesInFolder = filteredNotes.filter { it.folder.equals(node.fullPath, ignoreCase = true) }
    val isExpanded = expandedFolders.contains(node.fullPath)

    item(key = "folder_${node.fullPath}") {
        FolderTreeHeader(
            folderName = node.name,
            fullPath = node.fullPath,
            depth = node.depth,
            count = notesInFolder.size,
            isExpanded = isExpanded,
            customIcon = if (node.fullPath.equals("General", ignoreCase = true)) "📁" else null,
            onToggleExpand = {
                onToggleFolderExpanded(node.fullPath, !isExpanded)
            },
            onNewNoteHere = {
                handlePlusClick(node.fullPath)
            },
            onCreateSubfolder = {
                onCreateSubfolder(node.fullPath)
            },
            onRenameFolder = if (!node.fullPath.equals("General", ignoreCase = true)) {
                {
                    onRenameFolder(node.fullPath, node.name)
                }
            } else null,
            onDeleteFolder = if (!node.fullPath.equals("General", ignoreCase = true)) {
                {
                    onDeleteFolder(node.fullPath)
                }
            } else null
        )
    }

    if (isExpanded) {
        val sortedNotes = getSortedNotesForFolder(notesInFolder)
        if (sortedNotes.isEmpty() && node.subfolders.isEmpty()) {
            item(key = "empty_notes_${node.fullPath}") {
                IndentedGuideLineContainer(depth = node.depth + 1) {
                    androidx.compose.material3.Text(
                        text = "Empty folder",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            items(sortedNotes, key = { it.id }) { note ->
                IndentedGuideLineContainer(depth = node.depth + 1) {
                    NoteTreeItem(
                        note = note,
                        isCurrentlyOpen = note.id == currentNoteId,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = selectedNoteIds.contains(note.id),
                        showMetrics = showMetrics,
                        metricsStyle = noteMetricsStyle,
                        noteMetricsOrder = noteMetricsOrder,
                        noteMetricsHiddenItems = noteMetricsHiddenItems,
                        onNoteClick = {
                            if (isMultiSelectMode) {
                                // handled in screen
                            } else {
                                onNoteSelect(note)
                                onClose()
                            }
                        },
                        onToggleSelect = {},
                        onRename = { onRenameNote(note) },
                        onMoveToFolder = { onMoveNote(note) },
                        onPinToggle = { onPinToggle(note) },
                        onArchive = { onArchiveToggle(note) },
                        onDuplicate = { onDuplicateNote(note) },
                        onDelete = { onDeleteNoteConfirm(note) },
                        onStartMultiSelect = { onStartMultiSelect(note) }
                    )
                }
            }
        }

        node.subfolders.forEach { subNode ->
            renderFolderTreeNode(
                subNode, filteredNotes, expandedFolders, currentNoteId,
                isMultiSelectMode, selectedNoteIds, showMetrics,
                noteMetricsStyle, noteMetricsOrder, noteMetricsHiddenItems,
                onToggleFolderExpanded, handlePlusClick, onCreateSubfolder,
                onRenameFolder, onDeleteFolder, getSortedNotesForFolder,
                onNoteSelect, onClose, onPinToggle, onArchiveToggle,
                onDuplicateNote, onRenameNote, onMoveNote, onDeleteNoteConfirm,
                onStartMultiSelect
            )
        }
    }
}

/**
 * Indented container providing the thin vertical guide line on the left, exactly like Obsidian.
 */
@Composable
private fun IndentedGuideLineContainer(
    depth: Int = 1,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (14 * depth.coerceAtLeast(1)).dp)
    ) {
        // Thin vertical guide line
        Box(
            modifier = Modifier
                .width(1.5.dp)
                .fillMaxHeight()
                .padding(vertical = 2.dp)
                .background(AmoledBorder.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

/**
 * Folder Tree Header Row: chevron (▸ / ▾) + folder icon + name + count + context menu
 */
@Composable
private fun FolderTreeHeader(
    folderName: String,
    fullPath: String = folderName,
    depth: Int = 0,
    count: Int,
    isExpanded: Boolean,
    customIcon: String? = null,
    onToggleExpand: () -> Unit,
    onNewNoteHere: () -> Unit,
    onCreateSubfolder: (() -> Unit)? = null,
    onRenameFolder: (() -> Unit)? = null,
    onDeleteFolder: (() -> Unit)? = null
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevronRotation"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onToggleExpand)
            .testTag("folder_row_$fullPath")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotating Chevron (▸ / ▾)
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Folder Icon / Emoji
            if (customIcon != null) {
                Text(text = customIcon, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = SaffronPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Folder Name
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Item count badge
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Folder context menu button
            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Folder options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null, tint = SaffronPrimary) },
                        text = { Text("New note here") },
                        onClick = {
                            isMenuOpen = false
                            onNewNoteHere()
                        }
                    )
                    if (onCreateSubfolder != null) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = SaffronPrimary) },
                            text = { Text("New subfolder") },
                            onClick = {
                                isMenuOpen = false
                                onCreateSubfolder()
                            }
                        )
                    }
                    if (onRenameFolder != null) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Rename folder") },
                            onClick = {
                                isMenuOpen = false
                                onRenameFolder()
                            }
                        )
                    }
                    if (onDeleteFolder != null) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            text = { Text("Delete folder", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMenuOpen = false
                                onDeleteFolder()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Note Tree Row: compact row under folder with title, pin indicator, active highlight,
 * metrics line (WITHOUT WPM), and context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteTreeItem(
    note: Note,
    isCurrentlyOpen: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    showMetrics: Boolean,
    metricsStyle: String,
    noteMetricsOrder: String = com.example.ui.notes.NoteMetricsConfig.DEFAULT_ORDER_STRING,
    noteMetricsHiddenItems: Set<String> = emptySet(),
    isTrash: Boolean = false,
    isArchive: Boolean = false,
    onNoteClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onRename: (() -> Unit)? = null,
    onMoveToFolder: (() -> Unit)? = null,
    onPinToggle: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDeletePermanent: (() -> Unit)? = null,
    onStartMultiSelect: (() -> Unit)? = null
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val displayTitle = getDisplayTitle(note)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when {
            isSelected -> SaffronPrimary.copy(alpha = 0.2f)
            isCurrentlyOpen -> SaffronPrimary.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        border = BorderStroke(
            width = if (isCurrentlyOpen || isSelected) 1.dp else 0.5.dp,
            color = when {
                isSelected -> SaffronPrimary
                isCurrentlyOpen -> SaffronPrimary.copy(alpha = 0.45f)
                else -> AmoledBorder.copy(alpha = 0.25f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onNoteClick,
                onLongClick = {
                    if (!isTrash && !isArchive) {
                        isMenuOpen = true
                    }
                }
            )
            .testTag("note_row_${note.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multi-select checkbox
            if (isMultiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onToggleSelect)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Pin indicator / File icon
            if (note.isPinned && !isTrash) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = SaffronPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = if (isCurrentlyOpen) SaffronPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Title + Metrics Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentlyOpen) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.5.sp
                    ),
                    color = if (isCurrentlyOpen) SaffronPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Metric line with custom component selection, order & 2-line wrap
                if (showMetrics) {
                    val metricsText = com.example.ui.notes.NoteMetricsConfig.formatMetrics(
                        note = note,
                        style = metricsStyle,
                        orderStr = noteMetricsOrder,
                        hiddenItems = noteMetricsHiddenItems
                    )
                    if (metricsText.isNotBlank()) {
                        Text(
                            text = metricsText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                lineHeight = 13.5.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }

            // Quick actions / Context menu
            if (isTrash) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onRestore?.invoke() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onDeletePermanent?.invoke() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete permanently", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (isArchive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUnarchive?.invoke() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Unarchive, contentDescription = "Unarchive", tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onDelete?.invoke() },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Move to trash", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (!isMultiSelectMode) {
                // Context menu "⋮"
                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Note actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = SaffronPrimary) },
                            text = { Text("Open") },
                            onClick = {
                                isMenuOpen = false
                                onNoteClick()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Rename") },
                            onClick = {
                                isMenuOpen = false
                                onRename?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                            text = { Text("Move to folder") },
                            onClick = {
                                isMenuOpen = false
                                onMoveToFolder?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = if (note.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = SaffronPrimary
                                )
                            },
                            text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                            onClick = {
                                isMenuOpen = false
                                onPinToggle?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            text = { Text("Archive") },
                            onClick = {
                                isMenuOpen = false
                                onArchive?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            text = { Text("Duplicate") },
                            onClick = {
                                isMenuOpen = false
                                onDuplicate?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            text = { Text("Select notes") },
                            onClick = {
                                isMenuOpen = false
                                onStartMultiSelect?.invoke()
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMenuOpen = false
                                onDelete?.invoke()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Template item row inside Templates tree folder
 */
@Composable
private fun TemplateTreeRow(
    template: Stamp,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent,
        border = BorderStroke(0.5.dp, AmoledBorder.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onUse)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📋", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (template.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = SaffronPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = template.label.ifBlank { "Untitled Template" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = template.templateText.trim().replace("\n", " "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Template options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = SaffronPrimary) },
                        text = { Text("Use Template") },
                        onClick = {
                            isMenuOpen = false
                            onUse()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("Edit") },
                        onClick = {
                            isMenuOpen = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = if (template.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = SaffronPrimary
                            )
                        },
                        text = { Text(if (template.isPinned) "Unpin" else "Pin to top") },
                        onClick = {
                            isMenuOpen = false
                            onPin()
                        }
                    )
                    if (onMoveUp != null) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            onClick = {
                                isMenuOpen = false
                                onMoveUp()
                            }
                        )
                    }
                    if (onMoveDown != null) {
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            onClick = {
                                isMenuOpen = false
                                onMoveDown()
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            isMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Trash Header Banner with auto-empty settings and Clear action
 */
@Composable
private fun TrashHeaderBanner(
    trashedCount: Int,
    trashAutoEmptyDays: Int,
    onEmptyTrashClick: () -> Unit,
    onUpdateTrashDays: (Int) -> Unit
) {
    var showDaysDropdown by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = AmoledSurfaceVariant,
        border = BorderStroke(1.dp, AmoledBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Trash ($trashedCount notes)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Auto-empty after: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        Text(
                            text = "$trashAutoEmptyDays days ▼",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SaffronPrimary),
                            modifier = Modifier.clickable { showDaysDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showDaysDropdown,
                            onDismissRequest = { showDaysDropdown = false }
                        ) {
                            listOf(3, 7, 14, 30, 60).forEach { days ->
                                DropdownMenuItem(
                                    text = { Text("$days days") },
                                    onClick = {
                                        onUpdateTrashDays(days)
                                        showDaysDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (trashedCount > 0) {
                OutlinedButton(
                    onClick = onEmptyTrashClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Empty Trash", fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// HELPER UTILITIES
// ==========================================

/**
 * Sensible fallback title for notes:
 * Non-empty title -> title
 * First line of non-empty content -> first line
 * Otherwise -> "Untitled • MMM dd"
 */
private fun getDisplayTitle(note: Note): String {
    val title = note.title.trim()
    if (title.isNotBlank() && !title.equals("Untitled Note", ignoreCase = true) && !title.equals("Untitled", ignoreCase = true)) {
        return title
    }
    val firstLine = note.content.lines().map { it.trim() }.firstOrNull { it.isNotBlank() }
    if (!firstLine.isNullOrBlank()) {
        return firstLine.take(45)
    }
    val dateStr = DateFormat.format("MMM dd", Date(note.createdAt)).toString()
    return "Untitled • $dateStr"
}

