package com.example.mynote.ui.todos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.mynote.data.Todo
import com.example.mynote.ui.components.CircularCheckbox
import com.example.mynote.ui.components.CustomHeader
import com.example.mynote.ui.components.rememberElasticOverscrollEffect
import com.example.mynote.ui.theme.AppBackground
import androidx.compose.ui.graphics.graphicsLayer
import com.example.mynote.util.formatDateTime

/** What subset of to-dos the list is currently showing, chosen from the category sheet. */
sealed class TodoFilter {
    data object All : TodoFilter()
    data object Important : TodoFilter()
    data class Folder(val name: String) : TodoFilter()
}

@Composable
fun TodoListScreen(
    todos: List<Todo>,
    onToggleDone: (Todo) -> Unit,
    onUpdateTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onDeleteCompleted: () -> Unit,
    onAddTodo: (title: String, important: Boolean, dueAt: Long?) -> Unit,
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    isMenuVisible: Boolean,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onInputVisibilityChange: (Boolean) -> Unit = {},
    headerDimColor: Color = Color.Transparent,
    dimColor: Color = Color.Transparent,
    onEditTodo: (Todo) -> Unit = {},
    onScrimClick: () -> Unit = {}
) {
    var hideCompleted by remember { mutableStateOf(false) }
    var showDeleteCompletedConfirm by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<TodoFilter>(TodoFilter.All) }

    val folders = remember(todos) {
        todos.mapNotNull { it.folder?.trim()?.takeIf { name -> name.isNotBlank() } }
            .distinct()
            .sorted()
    }

    val scopedTodos = remember(todos, filter) {
        when (val f = filter) {
            TodoFilter.All -> todos
            TodoFilter.Important -> todos.filter { it.isImportant }
            is TodoFilter.Folder -> todos.filter { it.folder == f.name }
        }
    }

    val pending = remember(scopedTodos) { scopedTodos.filter { !it.isDone } }
    val completed = remember(scopedTodos) { scopedTodos.filter { it.isDone } }

    val headerTitle = when (val f = filter) {
        TodoFilter.All -> "All To-dos"
        TodoFilter.Important -> "Important"
        is TodoFilter.Folder -> f.name
    }

    // Reserve exactly as much space as the header actually renders (status bar inset +
    // content), instead of a hardcoded guess, so the header's dim overlay and the list's
    // dim overlay below always share the same boundary with no seam/gap between them.
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Space for the header on top
            Box(modifier = Modifier.height(headerHeightDp))

            Box(modifier = Modifier.weight(1f)) {
                if (todos.isEmpty()) {
                    Text(
                        text = "Nothing to do yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else if (scopedTodos.isEmpty()) {
                    Text(
                        text = "No to-dos in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = rememberElasticOverscrollEffect(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pending, key = { it.id }) { todo ->
                            TodoItemWithDismiss(
                                todo,
                                onToggleDone,
                                onDeleteTodo,
                                onEdit = { onEditTodo(it) },
                                modifier = Modifier.animateItem()
                            )
                        }

                        if (!hideCompleted && completed.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }
                            items(completed, key = { it.id }) { todo ->
                                TodoItemWithDismiss(
                                    todo,
                                    onToggleDone,
                                    onDeleteTodo,
                                    onEdit = { onEditTodo(it) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }

                // Body Scrim (Active for both menus)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dimColor)
                        .then(if (isMenuVisible || dimColor != Color.Transparent) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onScrimClick() } else Modifier)
                )
            }
        }

        // Slide-down Category Menu (Behind Header). Offset by the header's actual
        // measured height (which already includes the status bar inset) so the sheet
        // starts exactly where the header ends, no matter the device or font scale.
        androidx.compose.animation.AnimatedVisibility(
            visible = isMenuVisible,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeightDp),
            enter = slideInVertically { -it } + expandVertically(expandFrom = Alignment.Top),
            exit = slideOutVertically { -it } + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                color = AppBackground,
                tonalElevation = 3.dp
            ) {
                TodoCategorySheetContent(
                    totalCount = todos.size,
                    importantCount = todos.count { it.isImportant && !it.isDone },
                    folders = folders,
                    selectedFilter = filter,
                    onSelectFilter = {
                        filter = it
                        onToggleMenu()
                    }
                )
            }
        }

        // Header (Always on Top)
        CustomHeader(
            title = headerTitle,
            subtitle = "${pending.size} pending" + if (completed.isNotEmpty()) " · ${completed.size} done" else "",
            onTitleClick = onToggleMenu,
            menuItems = listOfNotNull(
                if (hideCompleted) "Show Completed" else "Hide Completed",
                if (completed.isNotEmpty()) "Batch Delete" else null,
                "Settings"
            ),
            onMenuItemClick = { item ->
                when (item) {
                    "Hide Completed" -> hideCompleted = true
                    "Show Completed" -> hideCompleted = false
                    "Batch Delete" -> showDeleteCompletedConfirm = true
                    "Settings" -> onOpenSettings()
                }
            },
            dimColor = headerDimColor,
            onDimClick = onScrimClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(AppBackground)
                .onGloballyPositioned { headerHeightPx = it.size.height }
        )
    }

    if (showDeleteCompletedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteCompletedConfirm = false },
            title = { Text("Delete completed to-dos?") },
            text = { Text("${completed.size} completed to-do(s) will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCompleted()
                    showDeleteCompletedConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompletedConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TodoCategorySheetContent(
    totalCount: Int,
    importantCount: Int,
    folders: List<String>,
    selectedFilter: TodoFilter,
    onSelectFilter: (TodoFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        TodoCategoryItem(
            Icons.Filled.ListAlt,
            "All To-dos",
            totalCount,
            selectedFilter == TodoFilter.All
        ) { onSelectFilter(TodoFilter.All) }
        TodoCategoryItem(
            Icons.Filled.PriorityHigh,
            "Important",
            importantCount,
            selectedFilter == TodoFilter.Important
        ) { onSelectFilter(TodoFilter.Important) }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Folders",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        if (folders.isEmpty()) {
            Text(
                text = "To-dos don't have a folder yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        } else {
            folders.forEach { name ->
                TodoCategoryItem(
                    Icons.Filled.Folder,
                    name,
                    count = null,
                    isSelected = selectedFilter is TodoFilter.Folder && selectedFilter.name == name
                ) { onSelectFilter(TodoFilter.Folder(name)) }
            }
        }
    }
}

@Composable
private fun TodoCategoryItem(
    icon: ImageVector,
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoItemWithDismiss(
    todo: Todo,
    onToggleDone: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onEdit: (Todo) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(todo)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.5f)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
                    .background(color, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        TodoRow(todo = todo, onToggleDone = onToggleDone, onDelete = onDelete, onEdit = onEdit)
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    onToggleDone: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onEdit: (Todo) -> Unit
) {
    val isOverdue = !todo.isDone && todo.dueAt != null && todo.dueAt < System.currentTimeMillis()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "todoScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = { onEdit(todo) },
                indication = null,
                interactionSource = interactionSource
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            CircularCheckbox(checked = todo.isDone, onCheckedChange = { onToggleDone(todo) })
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todo.isImportant && !todo.isDone) {
                        Text(
                            text = "! ",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                        color = if (todo.isDone) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                if (todo.dueAt != null) {
                    Text(
                        text = (if (isOverdue) "Overdue: " else "") + formatDateTime(todo.dueAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = { onDelete(todo) }) {
                Icon(Icons.Filled.Close, contentDescription = "Delete to-do")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoInputContent(
    title: String,
    onTitleChange: (String) -> Unit,
    isImportant: Boolean,
    onImportantChange: (Boolean) -> Unit,
    dueAt: Long?,
    onDueAtChange: (Long?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isEdit: Boolean
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueAt)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDueAtChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 0.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEdit) "Edit to-do" else "New to-do",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("What needs to be done?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Mark as important", modifier = Modifier.weight(1f))
            Switch(checked = isImportant, onCheckedChange = onImportantChange)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                text = if (dueAt != null) "Due: ${formatDateTime(dueAt)}" else "No due date",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Pick Date")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.Button(
            onClick = onConfirm,
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isEdit) "Save" else "Add")
        }
        
        // Add some bottom padding for edge-to-edge
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TodoListScreenPreview() {
    MaterialTheme {
        TodoListScreen(
            todos = listOf(
                Todo(id = 1, title = "Buy groceries", isImportant = true, dueAt = System.currentTimeMillis() + 86400000),
                Todo(id = 2, title = "Call mom", isDone = true),
                Todo(id = 3, title = "Finish project", dueAt = System.currentTimeMillis() - 3600000)
            ),
            onToggleDone = {},
            onUpdateTodo = {},
            onDeleteTodo = {},
            onDeleteCompleted = {},
            onAddTodo = { _, _, _ -> },
            showAddDialog = false,
            onDismissAddDialog = {},
            onOpenSettings = {},
            isMenuVisible = false,
            onToggleMenu = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TodoInputContentPreview() {
    MaterialTheme {
        TodoInputContent(
            title = "Sample To-do",
            onTitleChange = {},
            isImportant = true,
            onImportantChange = {},
            dueAt = System.currentTimeMillis(),
            onDueAtChange = {},
            onConfirm = {},
            onDismiss = {},
            isEdit = false
        )
    }
}
