package com.example.mynote.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * Header used on both list screens. Normally shows a title, subtitle, and an
 * overflow menu; when [selectionCount] is non-null it animates into a batch
 * selection bar instead ("N selected" + cancel/delete), matching the
 * "Batch Delete" menu action from either screen.
 */
@Composable
fun CustomHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onTitleClick: () -> Unit = {},
    menuItems: List<String> = emptyList(),
    onMenuItemClick: (String) -> Unit = {},
    selectionCount: Int? = null,
    onCancelSelection: () -> Unit = {},
    onDeleteSelection: () -> Unit = {},
    dimColor: Color = Color.Transparent,
    onDimClick: () -> Unit = {}
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = selectionCount,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "header",
            modifier = Modifier.statusBarsPadding()
        ) { count ->
            if (count != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancelSelection) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                    }
                    Text(
                        text = "$count selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDeleteSelection) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete selected",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                            IconButton(onClick = onTitleClick, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Change Category",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Column {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(x = (-12).dp, y = 0.dp)
                        ) {
                            menuItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        showMenu = false
                                        onMenuItemClick(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (dimColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(dimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDimClick
                    )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CustomHeaderPreview() {
    MaterialTheme {
        CustomHeader(
            title = "All Notes",
            subtitle = "12 notes",
            menuItems = listOf("Select Notes", "Settings")
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CustomHeaderSelectionPreview() {
    MaterialTheme {
        CustomHeader(
            title = "All Notes",
            subtitle = "12 notes",
            selectionCount = 3
        )
    }
}

