@file:OptIn(ExperimentalFoundationApi::class)

package com.example.mynote.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mynote.data.Note
import com.example.mynote.data.SortOrder
import com.example.mynote.data.ViewMode
import com.example.mynote.ui.components.CircularCheckbox
import com.example.mynote.ui.components.CustomHeader
import com.example.mynote.ui.components.rememberElasticOverscrollEffect
import com.example.mynote.ui.theme.AppBackground
import androidx.compose.ui.graphics.graphicsLayer
import com.example.mynote.util.formatDate
import kotlin.math.roundToInt

/** What subset of notes the list is currently showing, chosen from the category sheet. */
sealed class NoteFilter {
    data object All : NoteFilter()
    data object Favorites : NoteFilter()
    data class Folder(val name: String) : NoteFilter()
}

private val dimDuration = 300

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun NoteListScreen(
    notes: List<Note>,
    folders: List<String>,
    trashCount: Int,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onNoteClick: (Note) -> Unit,
    onToggleFavorite: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onMoveToFolder: (Note, String?) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onDeleteNotes: (Set<Long>) -> Unit,
    onSetViewMode: (ViewMode) -> Unit,
    onSetSortOrder: (SortOrder) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    isMenuVisible: Boolean,
    onToggleMenu: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<NoteFilter>(NoteFilter.All) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectionModeActive by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedNoteForFolder by remember { mutableStateOf<Note?>(null) }

    val dimColor by animateColorAsState(
        targetValue = if (isMenuVisible) Color.Black.copy(alpha = 0.4f) else Color.Transparent,
        animationSpec = tween(dimDuration),
        label = "scrimColor"
    )

    fun exitSelection() {
        selectionModeActive = false
        selectedIds = emptySet()
    }

    val scopedNotes = remember(notes, filter) {
        when (val f = filter) {
            NoteFilter.All -> notes
            NoteFilter.Favorites -> notes.filter { it.isFavorite }
            is NoteFilter.Folder -> notes.filter { it.folder == f.name }
        }
    }

    val filteredNotes = remember(scopedNotes, query) {
        if (query.isBlank()) {
            scopedNotes
        } else {
            scopedNotes.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }
    }

    val headerTitle = when (val f = filter) {
        NoteFilter.All -> "All Notes"
        NoteFilter.Favorites -> "Favorites"
        is NoteFilter.Folder -> f.name
    }
    val isGridView = viewMode == ViewMode.GRID

    val density = LocalDensity.current
    val searchBarHeight = 72.dp
    val searchBarHeightPx = with(density) { searchBarHeight.toPx() }
    var searchBarOffsetHeightPx by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember(searchBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = searchBarOffsetHeightPx + delta
                searchBarOffsetHeightPx = newOffset.coerceIn(-searchBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        CustomHeader(
            title = headerTitle,
            subtitle = "${filteredNotes.size} notes",
            onTitleClick = onToggleMenu,
            menuItems = listOf(
                if (isGridView) "List View" else "Grid View",
                "Select Notes",
                "Sort By",
                "Settings"
            ),
            onMenuItemClick = { item ->
                when (item) {
                    "List View" -> onSetViewMode(ViewMode.LIST)
                    "Grid View" -> onSetViewMode(ViewMode.GRID)
                    "Select Notes" -> selectionModeActive = true
                    "Sort By" -> showSortDialog = true
                    "Settings" -> onOpenSettings()
                }
            },
            selectionCount = if (selectionModeActive) selectedIds.size else null,
            onCancelSelection = { exitSelection() },
            onDeleteSelection = {
                onDeleteNotes(selectedIds)
                exitSelection()
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(nestedScrollConnection)
                .clipToBounds()
        ) {
            val overscrollEffect = rememberElasticOverscrollEffect()
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGridView) 2 else 1),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = overscrollEffect,
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(searchBarHeight))
                }

                if (filteredNotes.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = if (query.isBlank()) "No notes yet" else "No matching notes",
                            subtitle = if (query.isBlank()) "Tap + to write your first note" else null
                        )
                    }
                } else {
                    items(filteredNotes, key = { it.id }) { note ->
                        SwipeToActionBox(
                            onPin = { onTogglePin(note) },
                            onMoveToFolder = { selectedNoteForFolder = note },
                            onFavorite = { onToggleFavorite(note) },
                            onDelete = { onDeleteNote(note) },
                            enabled = !selectionModeActive && !isGridView
                        ) {
                            NoteCard(
                                note = note,
                                isSelected = note.id in selectedIds,
                                selectionModeActive = selectionModeActive,
                                onClick = {
                                    if (selectionModeActive) {
                                        selectedIds = if (note.id in selectedIds) {
                                            selectedIds - note.id
                                        } else {
                                            selectedIds + note.id
                                        }
                                    } else {
                                        onNoteClick(note)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionModeActive) {
                                        selectionModeActive = true
                                        selectedIds = setOf(note.id)
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(note) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }

            // Floating Search Bar
            androidx.compose.animation.AnimatedVisibility(
                visible = !selectionModeActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (searchBarOffsetHeightPx + overscrollEffect.overscrollOffset.value).roundToInt()
                        )
                    }
                    .background(AppBackground)
                    .padding(horizontal = 24.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(searchBarHeight)
                        .padding(vertical = 8.dp),
                    placeholder = { Text("Search notes") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dimColor)
                    .then(if (isMenuVisible) Modifier.clickable { onToggleMenu() } else Modifier)
            )

            // Slide-down Menu
            androidx.compose.animation.AnimatedVisibility(
                visible = isMenuVisible,
                enter = slideInVertically { -it } + expandVertically(expandFrom = Alignment.Top),
                exit = slideOutVertically { -it } + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = AppBackground,
                    tonalElevation = 3.dp
                ) {
                    CategorySheetContent(
                        totalCount = notes.size,
                        favoritesCount = notes.count { it.isFavorite },
                        trashCount = trashCount,
                        folders = folders,
                        selectedFilter = filter,
                        onSelectFilter = {
                            filter = it
                            onToggleMenu()
                        },
                        onOpenTrash = {
                            onToggleMenu()
                            onOpenTrash()
                        }
                    )
                }
            }
        }
    }


    if (showSortDialog) {
        SortByDialog(
            current = sortOrder,
            onSelect = {
                onSetSortOrder(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (selectedNoteForFolder != null) {
        FolderPickerDialog(
            current = selectedNoteForFolder?.folder,
            folders = folders,
            onSelect = { folder ->
                selectedNoteForFolder?.let { onMoveToFolder(it, folder) }
                selectedNoteForFolder = null
            },
            onDismiss = { selectedNoteForFolder = null }
        )
    }
}

@Composable
private fun SortByDialog(
    current: SortOrder,
    onSelect: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        SortOrder.DATE_MODIFIED to "Date modified",
        SortOrder.DATE_CREATED to "Date created",
        SortOrder.TITLE_ASC to "Title (A-Z)"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column {
                options.forEach { (order, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onSelect(order) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = order == current, onClick = { onSelect(order) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun CategorySheetContent(
    totalCount: Int,
    favoritesCount: Int,
    trashCount: Int,
    folders: List<String>,
    selectedFilter: NoteFilter,
    onSelectFilter: (NoteFilter) -> Unit,
    onOpenTrash: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        CategoryItem(
            Icons.Filled.Description,
            "All Notes",
            totalCount,
            selectedFilter == NoteFilter.All
        ) { onSelectFilter(NoteFilter.All) }
        CategoryItem(
            Icons.Filled.Star,
            "My Favorites",
            favoritesCount,
            selectedFilter == NoteFilter.Favorites
        ) { onSelectFilter(NoteFilter.Favorites) }
        CategoryItem(
            Icons.Filled.Delete,
            "Recently Deleted",
            trashCount,
            false,
            onClick = onOpenTrash
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Folders",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        if (folders.isEmpty()) {
            Text(
                text = "Notes get a folder from the editor - add one there.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        } else {
            folders.forEach { name ->
                CategoryItem(
                    Icons.Filled.Folder,
                    name,
                    count = null,
                    isSelected = selectedFilter is NoteFilter.Folder && selectedFilter.name == name
                ) { onSelectFilter(NoteFilter.Folder(name)) }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun NoteCard(
    note: Note,
    isSelected: Boolean,
    selectionModeActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        label = "cardBorder"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        with(sharedTransitionScope) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(borderWidth)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .sharedElement(
                        rememberSharedContentState(key = "note_${note.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        indication = null,
                        interactionSource = interactionSource
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (note.isPinned && !selectionModeActive) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(16.dp)
                            )
                        }
                        if (selectionModeActive) {
                            SelectionCheck(isSelected)
                        } else {
                            FavoriteStar(isFavorite = note.isFavorite, onClick = onToggleFavorite)
                        }
                    }
                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.preview(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(note.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        if (!note.folder.isNullOrBlank()) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = note.folder,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteStar(isFavorite: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
        label = "favoriteColor"
    )
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = if (isFavorite) "Unfavorite" else "Mark as favorite",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SelectionCheck(isSelected: Boolean) {
    CircularCheckbox(
        checked = isSelected,
        onCheckedChange = {},
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun NoteListGridPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                NoteListScreen(
                    notes = listOf(
                        Note(id = 1, title = "Shopping List", content = "Eggs, Milk, Bread", isFavorite = true, isPinned = true),
                        Note(id = 2, title = "Work Project", content = "Finish the proposal by Friday", folder = "Work"),
                        Note(id = 3, title = "Ideas", content = "New app idea for note taking", folder = "Personal")
                    ),
                    folders = listOf("Work", "Personal"),
                    trashCount = 2,
                    viewMode = ViewMode.GRID,
                    sortOrder = SortOrder.DATE_MODIFIED,
                    onNoteClick = {},
                    onToggleFavorite = {},
                    onTogglePin = {},
                    onMoveToFolder = { _, _ -> },
                    onDeleteNote = {},
                    onDeleteNotes = {},
                    onSetViewMode = {},
                    onSetSortOrder = {},
onOpenTrash = {},
                    onOpenSettings = {},
                    isMenuVisible = false,
                    onToggleMenu = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun NoteListEmptyPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                NoteListScreen(
                    notes = emptyList(),
                    folders = emptyList(),
                    trashCount = 0,
                    viewMode = ViewMode.GRID,
                    sortOrder = SortOrder.DATE_MODIFIED,
                    onNoteClick = {},
                    onToggleFavorite = {},
                    onTogglePin = {},
                    onMoveToFolder = { _, _ -> },
                    onDeleteNote = {},
                    onDeleteNotes = {},
                    onSetViewMode = {},
                    onSetSortOrder = {},
onOpenTrash = {},
                    onOpenSettings = {},
                    isMenuVisible = false,
                    onToggleMenu = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun NoteCardPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                Box(modifier = Modifier.padding(16.dp)) {
                    NoteCard(
                        note = Note(
                            id = 1,
                            title = "Preview Note",
                            content = "This is a preview of the note content.",
                            folder = "Preview",
                            isFavorite = true,
                            isPinned = true
                        ),
                        isSelected = false,
                        selectionModeActive = false,
                        onClick = {},
                        onLongClick = {},
                        onToggleFavorite = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun NoteCardSelectedPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                Box(modifier = Modifier.padding(16.dp)) {
                    NoteCard(
                        note = Note(
                            id = 1,
                            title = "Selected Note",
                            content = "This note is currently selected in selection mode.",
                            folder = "Work",
                            isPinned = false
                        ),
                        isSelected = true,
                        selectionModeActive = true,
                        onClick = {},
                        onLongClick = {},
                        onToggleFavorite = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SortByDialogPreview() {
    MaterialTheme {
        SortByDialog(
            current = SortOrder.DATE_MODIFIED,
            onSelect = {},
            onDismiss = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CategorySheetContentPreview() {
    MaterialTheme {
        CategorySheetContent(
            totalCount = 15,
            favoritesCount = 5,
            trashCount = 3,
            folders = listOf("Work", "Personal", "Study"),
            selectedFilter = NoteFilter.All,
            onSelectFilter = {},
            onOpenTrash = {},
        )
    }
}

private enum class DragValue { Start, End, Dismiss }

private val pinColor = Color(0xFF4CAF50)
private val archiveColor = Color(0xFF2196F3)
private val favoriteColor = Color(0xFFFFC107)
private val deleteColor = Color(0xFFF44336)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeToActionBox(
    onPin: () -> Unit,
    onMoveToFolder: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }

    val density = LocalDensity.current
    val actionsWidth = with(density) { (4 * 56 + 12).dp.toPx() }
    val screenWidth = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()

    val state = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Start,
            anchors = DraggableAnchors {
                DragValue.Start at 0f
                DragValue.End at -actionsWidth
                DragValue.Dismiss at -screenWidth
            },
            positionalThreshold = { distance: Float -> distance * 0.3f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = decayAnimationSpec,
            confirmValueChange = {
                if (it == DragValue.Dismiss) {
                    onDelete()
                }
                true
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
    ) {
        // Full Swipe Delete Background
        val offset = state.requireOffset()
        val isThresholdReached = offset < -actionsWidth

        val bgColor by animateColorAsState(
            targetValue = if (isThresholdReached) deleteColor.copy(alpha = 0.8f) else Color.Transparent,
            label = "fullSwipeBg"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isThresholdReached) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .scale(1.2f)
                )
            }
        }

        // Background Actions
        AnimatedVisibility(
            visible = !isThresholdReached,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(pinColor, Icons.Filled.VerticalAlignTop, "Pin", onPin)
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(archiveColor, Icons.Filled.Folder, "Move to Folder", onMoveToFolder)
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(favoriteColor, Icons.Filled.Star, "Favorite", onFavorite)
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(deleteColor, Icons.Filled.Delete, "Delete", onDelete)
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
        ) {
            content()
        }
    }
}

@Composable
private fun ActionButton(
    color: Color,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
