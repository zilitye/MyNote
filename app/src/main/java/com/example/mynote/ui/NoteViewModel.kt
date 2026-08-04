package com.example.mynote.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mynote.data.AppDatabase
import com.example.mynote.data.Note
import com.example.mynote.data.SettingsRepository
import com.example.mynote.data.SortOrder
import com.example.mynote.data.Todo
import com.example.mynote.data.ThemeMode
import com.example.mynote.data.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(
    private val database: AppDatabase,
    private val settings: SettingsRepository
) : ViewModel() {

    private val dao = database.noteDao()

    private val _sortOrder = MutableStateFlow(settings.sortOrder)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _viewMode = MutableStateFlow(settings.viewMode)
    val viewMode: StateFlow<ViewMode> = _viewMode

    private val _themeMode = MutableStateFlow(settings.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val rawNotes = dao.getAllNotesFlow()

    /** Active notes, sorted per [sortOrder], with favorites and pinned notes floated to the top. */
    val allNotes: StateFlow<List<Note>> = combine(rawNotes, _sortOrder) { notes, order ->
        val sorted = when (order) {
            SortOrder.DATE_MODIFIED -> notes.sortedByDescending { it.updatedAt }
            SortOrder.DATE_CREATED -> notes.sortedByDescending { it.createdAt }
            SortOrder.TITLE_ASC -> notes.sortedBy { it.title.ifBlank { "Untitled" }.lowercase() }
        }
        sorted.sortedByDescending { it.isFavorite }
            .sortedByDescending { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedNotes: StateFlow<List<Note>> = dao.getTrashedNotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = rawNotes.map { list ->
        list.mapNotNull { note -> note.folder?.trim()?.takeIf { it.isNotBlank() } }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTodos: StateFlow<List<Todo>> = dao.getAllTodosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Notes ----

    /** Inserts a brand-new note and reports the generated id back to the caller. */
    fun createNote(
        title: String,
        content: String,
        folder: String?,
        isFavorite: Boolean,
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = dao.insertNote(
                Note(
                    title = title,
                    content = content,
                    folder = folder,
                    isFavorite = isFavorite,
                    isPinned = isPinned,
                    isArchived = isArchived
                )
            )
            onCreated(id)
        }
    }

    /** Updates just the editable fields of an existing note - used by autosave and on exit. */
    fun saveNoteFields(
        id: Long,
        title: String,
        content: String,
        folder: String?,
        isFavorite: Boolean,
        isPinned: Boolean,
        isArchived: Boolean
    ) {
        viewModelScope.launch {
            dao.updateNoteFields(
                id,
                title,
                content,
                folder,
                isFavorite,
                isPinned,
                isArchived,
                System.currentTimeMillis()
            )
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { dao.setFavorite(note.id, !note.isFavorite) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { dao.setPinned(note.id, !note.isPinned) }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch { dao.setArchived(note.id, !note.isArchived) }
    }

    fun setFolder(note: Note, folder: String?) {
        viewModelScope.launch {
            dao.updateNoteFields(
                note.id,
                note.title,
                note.content,
                folder,
                note.isFavorite,
                note.isPinned,
                note.isArchived,
                System.currentTimeMillis()
            )
        }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch { dao.moveToTrash(id, System.currentTimeMillis()) }
    }

    fun moveToTrashBatch(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { dao.moveToTrashBatch(ids.toList(), System.currentTimeMillis()) }
    }

    fun restoreNote(id: Long) {
        viewModelScope.launch { dao.restoreNote(id) }
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch { dao.deleteNotePermanently(id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { dao.emptyTrash() }
    }

    // ---- To-dos ----

    fun addTodo(title: String, isImportant: Boolean, dueAt: Long?, folder: String? = null) {
        viewModelScope.launch {
            dao.insertTodo(Todo(title = title, isImportant = isImportant, dueAt = dueAt, folder = folder))
        }
    }

    fun toggleTodo(todo: Todo) {
        viewModelScope.launch {
            dao.updateTodo(todo.copy(isDone = !todo.isDone))
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            dao.updateTodo(todo)
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            dao.deleteTodo(id)
        }
    }

    fun deleteCompletedTodos() {
        viewModelScope.launch { dao.deleteCompletedTodos() }
    }

    // ---- Settings ----

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        settings.sortOrder = order
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        settings.viewMode = mode
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settings.themeMode = mode
    }
}

class NoteViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(
                AppDatabase.getDatabase(context),
                SettingsRepository(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
