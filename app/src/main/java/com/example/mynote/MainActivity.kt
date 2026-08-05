package com.example.mynote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mynote.data.Todo
import com.example.mynote.ui.NoteViewModel
import com.example.mynote.ui.NoteViewModelFactory
import com.example.mynote.ui.notes.NoteEditScreen
import com.example.mynote.ui.notes.NoteListScreen
import com.example.mynote.ui.notes.TrashScreen
import com.example.mynote.ui.settings.SettingsScreen
import com.example.mynote.ui.theme.AppBackground
import com.example.mynote.ui.theme.MyNoteTheme
import com.example.mynote.ui.todos.TodoInputContent
import com.example.mynote.ui.todos.TodoListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NoteViewModel = viewModel(factory = NoteViewModelFactory(LocalContext.current))
            val themeMode by viewModel.themeMode.collectAsState()
            MyNoteTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MyNoteApp(viewModel)
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object NoteList : Screen("noteList")
    data object TodoList : Screen("todoList")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
    data object NoteEditor : Screen("noteEditor?noteId={noteId}") {
        fun createRoute(noteId: Long?) = "noteEditor?noteId=${noteId ?: -1L}"
    }
}

private val slideDurationMs = 260
private val dimDuration = 300

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MyNoteApp(viewModel: NoteViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val notes by viewModel.allNotes.collectAsState()
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val todos by viewModel.allTodos.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var wasEditingBeforeDismiss by remember { mutableStateOf(false) }
    
    // Global Input State
    var inputTitle by remember { mutableStateOf("") }
    var inputIsImportant by remember { mutableStateOf(false) }
    var inputDueAt by remember { mutableStateOf<Long?>(null) }

    var isCategoryMenuVisible by remember { mutableStateOf(false) }

    val isTodoInputVisible = showAddTodoDialog || editingTodo != null

    LaunchedEffect(isTodoInputVisible) {
        if (showAddTodoDialog) {
            inputTitle = ""
            inputIsImportant = false
            inputDueAt = null
            wasEditingBeforeDismiss = false
        } else if (editingTodo != null) {
            inputTitle = editingTodo?.title ?: ""
            inputIsImportant = editingTodo?.isImportant ?: false
            inputDueAt = editingTodo?.dueAt
            wasEditingBeforeDismiss = true
        }
    }

    val dimColor by animateColorAsState(
        targetValue = if (isTodoInputVisible || isCategoryMenuVisible) Color.Black.copy(alpha = 0.4f) else Color.Transparent,
        animationSpec = tween(dimDuration),
        label = "dimOverlay"
    )

    val headerDimColor by animateColorAsState(
        targetValue = if (isTodoInputVisible) Color.Black.copy(alpha = 0.4f) else Color.Transparent,
        animationSpec = tween(dimDuration),
        label = "headerDimOverlay"
    )

    val onScrimClick = {
        isCategoryMenuVisible = false
        showAddTodoDialog = false
        editingTodo = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                    if (!isCategoryMenuVisible && !isTodoInputVisible && (currentRoute == Screen.NoteList.route || currentRoute == Screen.TodoList.route)) {
                        FloatingActionButton(
                            onClick = {
                                if (currentRoute == Screen.NoteList.route) {
                                    navController.navigate(Screen.NoteEditor.createRoute(null))
                                } else {
                                    showAddTodoDialog = true
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add")
                        }
                    }
                },
                bottomBar = {
                    if (currentRoute == Screen.NoteList.route || currentRoute == Screen.TodoList.route) {
                        Box {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.background,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == Screen.NoteList.route,
                                    onClick = {
                                        if (currentRoute != Screen.NoteList.route) {
                                            navController.navigate(Screen.NoteList.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                                    label = { Text("Notes") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentRoute == Screen.TodoList.route,
                                    onClick = {
                                        if (currentRoute != Screen.TodoList.route) {
                                            navController.navigate(Screen.TodoList.route) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                                    label = { Text("To-dos") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(dimColor)
                                    .then(if (isCategoryMenuVisible || isTodoInputVisible) Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onScrimClick()
                                    } else Modifier)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.NoteList.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(
                            Screen.NoteList.route,
                            enterTransition = { fadeIn(tween(slideDurationMs)) },
                            exitTransition = { fadeOut(tween(slideDurationMs)) }
                        ) {
                            NoteListScreen(
                                notes = notes,
                                folders = folders,
                                trashCount = trashedNotes.size,
                                viewMode = viewMode,
                                sortOrder = sortOrder,
                                onNoteClick = { note ->
                                    navController.navigate(Screen.NoteEditor.createRoute(note.id))
                                },
                                onToggleFavorite = { note -> viewModel.toggleFavorite(note) },
                                onTogglePin = { note -> viewModel.togglePin(note) },
                                onMoveToFolder = { note, folder -> viewModel.setFolder(note, folder) },
                                onDeleteNote = { note -> viewModel.moveToTrash(note.id) },
                                onDeleteNotes = { ids -> viewModel.moveToTrashBatch(ids) },
                                onSetViewMode = { viewModel.setViewMode(it) },
                                onSetSortOrder = { viewModel.setSortOrder(it) },
                                onOpenTrash = { navController.navigate(Screen.Trash.route) },
                                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                                isMenuVisible = isCategoryMenuVisible,
                                onToggleMenu = { isCategoryMenuVisible = !isCategoryMenuVisible },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable,
                                headerDimColor = headerDimColor,
                                dimColor = dimColor,
                                onScrimClick = onScrimClick
                            )
                        }
                        composable(
                            Screen.TodoList.route,
                            enterTransition = { fadeIn(tween(slideDurationMs)) },
                            exitTransition = { fadeOut(tween(slideDurationMs)) }
                        ) {
                            TodoListScreen(
                                todos = todos,
                                onToggleDone = { todo ->
                                    viewModel.toggleTodo(todo)
                                },
                                onUpdateTodo = { todo ->
                                    viewModel.updateTodo(todo)
                                },
                                onDeleteTodo = { todo ->
                                    viewModel.deleteTodo(todo.id)
                                },
                                onDeleteCompleted = { viewModel.deleteCompletedTodos() },
                                onAddTodo = { title, important, dueAt ->
                                    viewModel.addTodo(title, important, dueAt)
                                },
                                showAddDialog = showAddTodoDialog,
                                onDismissAddDialog = { showAddTodoDialog = false },
                                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                                isMenuVisible = isCategoryMenuVisible,
                                onToggleMenu = { isCategoryMenuVisible = !isCategoryMenuVisible },
                                headerDimColor = headerDimColor,
                                dimColor = dimColor,
                                onEditTodo = { editingTodo = it },
                                onScrimClick = onScrimClick
                            )
                        }
                        composable(
                            route = Screen.NoteEditor.route,
                            arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = -1L }),
                            enterTransition = {
                                slideInHorizontally(tween(slideDurationMs)) { it } + fadeIn(tween(slideDurationMs))
                            },
                            exitTransition = {
                                slideOutHorizontally(tween(slideDurationMs)) { -it / 4 } + fadeOut(tween(slideDurationMs))
                            },
                            popEnterTransition = {
                                slideInHorizontally(tween(slideDurationMs)) { -it / 4 } + fadeIn(tween(slideDurationMs))
                            },
                            popExitTransition = {
                                slideOutHorizontally(tween(slideDurationMs)) { it } + fadeOut(tween(slideDurationMs))
                            }
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                            val existingNote = if (noteId != -1L) notes.find { it.id == noteId } else null

                            NoteEditScreen(
                                existingNote = existingNote,
                                folders = folders,
                                onCreate = { title, content, folder, isFavorite, isPinned, isArchived, onCreated ->
                                    viewModel.createNote(title, content, folder, isFavorite, isPinned, isArchived, onCreated)
                                },
                                onAutosave = { id, title, content, folder, isFavorite, isPinned, isArchived ->
                                    viewModel.saveNoteFields(id, title, content, folder, isFavorite, isPinned, isArchived)
                                },
                                onBack = label@{ id, title, content, folder, isFavorite, isPinned, isArchived, isDiscard ->
                                    if (isDiscard) {
                                        // The editor autosaves in the background while the user types,
                                        // so by the time "Discard" is tapped the in-progress edits may
                                        // already be persisted. The values passed in here are the
                                        // pre-edit ("saved") state, so write them back to undo the
                                        // autosave. If the note was brand new (created only because
                                        // autosave kicked in) and had nothing in it before editing
                                        // started, remove it entirely instead of leaving an empty note.
                                        if (id != null) {
                                            if (title.isBlank() && content.isBlank()) {
                                                viewModel.moveToTrash(id)
                                            } else {
                                                viewModel.saveNoteFields(id, title, content, folder, isFavorite, isPinned, isArchived)
                                            }
                                        }
                                        navController.popBackStack()
                                        return@label
                                    }

                                    val isBlank = title.isBlank() && content.isBlank()
                                    val existing = if (id != null) notes.find { it.id == id } else null

                                    val hasChanged = existing == null ||
                                        title != existing.title ||
                                        content != existing.content ||
                                        folder != existing.folder ||
                                        isFavorite != existing.isFavorite ||
                                        isPinned != existing.isPinned ||
                                        isArchived != existing.isArchived

                                    when {
                                        id == null && !isBlank -> viewModel.createNote(title, content, folder, isFavorite, isPinned, isArchived) {}
                                        id != null && isBlank -> viewModel.moveToTrash(id)
                                        id != null && !isBlank && hasChanged -> viewModel.saveNoteFields(id, title, content, folder, isFavorite, isPinned, isArchived)
                                    }
                                    navController.popBackStack()
                                },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable
                            )
                        }
                        composable(
                            Screen.Trash.route,
                            enterTransition = {
                                slideInHorizontally(tween(slideDurationMs)) { it } + fadeIn(tween(slideDurationMs))
                            },
                            exitTransition = { fadeOut(tween(slideDurationMs)) },
                            popExitTransition = {
                                slideOutHorizontally(tween(slideDurationMs)) { it } + fadeOut(tween(slideDurationMs))
                            }
                        ) {
                            TrashScreen(
                                trashedNotes = trashedNotes,
                                onRestore = { note -> viewModel.restoreNote(note.id) },
                                onDeleteForever = { note -> viewModel.deleteForever(note.id) },
                                onEmptyTrash = { viewModel.emptyTrash() },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            Screen.Settings.route,
                            enterTransition = {
                                slideInHorizontally(tween(slideDurationMs)) { it } + fadeIn(tween(slideDurationMs))
                            },
                            exitTransition = { fadeOut(tween(slideDurationMs)) },
                            popExitTransition = {
                                slideOutHorizontally(tween(slideDurationMs)) { it } + fadeOut(tween(slideDurationMs))
                            }
                        ) {
                            SettingsScreen(
                                themeMode = themeMode,
                                viewMode = viewMode,
                                sortOrder = sortOrder,
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onSetViewMode = { viewModel.setViewMode(it) },
                                onSetSortOrder = { viewModel.setSortOrder(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        // Global Slide-up Todo Menu
        androidx.compose.animation.AnimatedVisibility(
            visible = isTodoInputVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = AppBackground,
                tonalElevation = 8.dp
            ) {
                TodoInputContent(
                    title = inputTitle,
                    onTitleChange = { inputTitle = it },
                    isImportant = inputIsImportant,
                    onImportantChange = { inputIsImportant = it },
                    dueAt = inputDueAt,
                    onDueAtChange = { inputDueAt = it },
                    onConfirm = {
                        if (showAddTodoDialog) {
                            viewModel.addTodo(inputTitle.trim(), inputIsImportant, inputDueAt)
                            showAddTodoDialog = false
                        } else if (editingTodo != null) {
                            viewModel.updateTodo(editingTodo!!.copy(
                                title = inputTitle.trim(),
                                isImportant = inputIsImportant,
                                dueAt = inputDueAt
                            ))
                            editingTodo = null
                        }
                    },
                    onDismiss = {
                        showAddTodoDialog = false
                        editingTodo = null
                    },
                    isEdit = if (isTodoInputVisible) editingTodo != null else wasEditingBeforeDismiss
                )
            }
        }
    }
}
