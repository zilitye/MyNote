package com.example.mynote.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mynote.data.Note
import com.example.mynote.util.formatDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

private enum class SaveState { IDLE, SAVING, SAVED }

private data class HistoryItem(val title: String, val content: String)

/**
 * Full-screen editor for creating or updating a single note.
 */
@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.FlowPreview::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun NoteEditScreen(
    existingNote: Note?,
    folders: List<String>,
    onCreate: (title: String, content: String, folder: String?, isFavorite: Boolean, isPinned: Boolean, isArchived: Boolean, onCreated: (Long) -> Unit) -> Unit,
    onAutosave: (id: Long, title: String, content: String, folder: String?, isFavorite: Boolean, isPinned: Boolean, isArchived: Boolean) -> Unit,
    onBack: (id: Long?, title: String, content: String, folder: String?, isFavorite: Boolean, isPinned: Boolean, isArchived: Boolean) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    var noteId by remember { mutableStateOf(existingNote?.id) }
    var title by remember { mutableStateOf(existingNote?.title.orEmpty()) }
    var content by remember { mutableStateOf(existingNote?.content.orEmpty()) }
    var folder by remember { mutableStateOf(existingNote?.folder) }
    var isFavorite by remember { mutableStateOf(existingNote?.isFavorite ?: false) }
    var isPinned by remember { mutableStateOf(existingNote?.isPinned ?: false) }
    var isArchived by remember { mutableStateOf(existingNote?.isArchived ?: false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var saveState by remember { mutableStateOf(SaveState.IDLE) }
    var isEditing by remember { mutableStateOf(existingNote == null) }

    // --- Undo/Redo Logic ---
    val history = remember { mutableStateListOf<HistoryItem>() }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var isUndoRedoing by remember { mutableStateOf(false) }

    LaunchedEffect(existingNote) {
        if (history.isEmpty()) {
            history.add(HistoryItem(existingNote?.title.orEmpty(), existingNote?.content.orEmpty()))
            historyIndex = 0
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { HistoryItem(title, content) }
            .debounce(500)
            .collectLatest { item ->
                if (!isUndoRedoing) {
                    if (historyIndex < history.size - 1) {
                        val toRemove = history.size - 1 - historyIndex
                        repeat(toRemove) { history.removeAt(history.size - 1) }
                    }
                    if (history.isEmpty() || history[historyIndex] != item) {
                        history.add(item)
                        if (history.size > 50) {
                            history.removeAt(0)
                        } else {
                            historyIndex++
                        }
                    }
                }
                isUndoRedoing = false
            }
    }

    fun undo() {
        if (historyIndex > 0) {
            isUndoRedoing = true
            historyIndex--
            val prev = history[historyIndex]
            title = prev.title
            content = prev.content
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            isUndoRedoing = true
            historyIndex++
            val next = history[historyIndex]
            title = next.title
            content = next.content
        }
    }

    LaunchedEffect(title, content, folder, isFavorite, isPinned, isArchived) {
        val unchanged = title == existingNote?.title.orEmpty() &&
            content == existingNote?.content.orEmpty() &&
            folder == existingNote?.folder &&
            isFavorite == (existingNote?.isFavorite ?: false) &&
            isPinned == (existingNote?.isPinned ?: false) &&
            isArchived == (existingNote?.isArchived ?: false)
        if (unchanged) return@LaunchedEffect
        if (title.isBlank() && content.isBlank()) return@LaunchedEffect

        saveState = SaveState.SAVING
        delay(1000)
        val id = noteId
        if (id == null) {
            onCreate(title, content, folder, isFavorite, isPinned, isArchived) { newId -> noteId = newId }
        } else {
            onAutosave(id, title, content, folder, isFavorite, isPinned, isArchived)
        }
        saveState = SaveState.SAVED
        delay(1500)
        if (saveState == SaveState.SAVED) saveState = SaveState.IDLE
    }

    BackHandler {
        onBack(noteId, title, content, folder, isFavorite, isPinned, isArchived)
    }

    if (showFolderDialog) {
        FolderPickerDialog(
            current = folder,
            folders = folders,
            onSelect = {
                folder = it
                showFolderDialog = false
            },
            onDismiss = { showFolderDialog = false }
        )
    }

    with(sharedTransitionScope) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.sharedElement(
                rememberSharedContentState(key = "note_${existingNote?.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.White,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets,
                    title = {
                        AnimatedVisibility(
                            visible = saveState != SaveState.IDLE,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = if (saveState == SaveState.SAVING) "Saving…" else "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { onBack(noteId, title, content, folder, isFavorite, isPinned, isArchived) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isEditing) {
                            IconButton(onClick = ::undo, enabled = historyIndex > 0) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            IconButton(onClick = ::redo, enabled = historyIndex < history.size - 1) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }
                            IconButton(onClick = { isEditing = false }) {
                                Icon(Icons.Filled.Check, contentDescription = "Done editing")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            ) {
                if (isEditing) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    "Title",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    )
                } else {
                    Text(
                        text = title.ifBlank { "Title" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clickable { isEditing = true },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (title.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(existingNote?.updatedAt ?: System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        modifier = Modifier
                            .clickable { showFolderDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = folder?.takeIf { it.isNotBlank() } ?: "Uncategorized",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (isEditing) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("Start writing…") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.White,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                } else {
                    Text(
                        text = content.ifBlank { "Start writing…" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { isEditing = true },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            color = if (content.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FolderPickerDialog(
    current: String?,
    folders: List<String>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var custom by remember { mutableStateOf(current?.takeIf { it !in folders } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    placeholder = { Text("New folder name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (folders.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            "Existing folders",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    folders.forEach { name ->
                        TextButton(onClick = { onSelect(name) }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(custom.trim().takeIf { it.isNotBlank() }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSelect(null) }) { Text("No folder") }
        }
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun NoteEditPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                NoteEditScreen(
                    existingNote = Note(
                        id = 1,
                        title = "Meeting Notes",
                        content = "Plan for the next sprint...",
                        folder = "Work",
                        isFavorite = true
                    ),
                    folders = listOf("Work", "Personal", "Ideas"),
                    onCreate = { _, _, _, _, _, _, _ -> },
                    onAutosave = { _, _, _, _, _, _, _ -> },
                    onBack = { _, _, _, _, _, _, _ -> },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
