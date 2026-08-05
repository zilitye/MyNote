package com.example.mynote.ui.todos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.mynote.data.Todo
import kotlin.math.roundToInt

private enum class DragValue { Center, ActionsRevealed, Dismissed }

private val archiveColor = Color(0xFF2196F3)
private val deleteColor = Color(0xFFF44336)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableTodoItem(
    todo: Todo,
    isOpened: Boolean,
    onOpened: () -> Unit,
    onClosed: () -> Unit,
    onToggleDone: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onMoveToFolder: (Todo) -> Unit,
    onEdit: (Todo) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Matching NoteListScreen: (2 * 40 + 1 * 8 + 12 + 12).dp (simplified for 2 buttons)
    val actionsWidth = (2 * 40 + 8 + 12 + 12).dp
    val actionsWidthPx = with(density) { actionsWidth.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()

    val state = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Center,
            anchors = DraggableAnchors {
                DragValue.Center at 0f
                DragValue.ActionsRevealed at -actionsWidthPx
                DragValue.Dismissed at -screenWidthPx
            },
            positionalThreshold = { distance: Float -> distance * 0.3f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = decayAnimationSpec,
            confirmValueChange = {
                if (it == DragValue.Dismissed) {
                    onDelete(todo)
                }
                true
            }
        )
    }

    androidx.compose.runtime.LaunchedEffect(isOpened) {
        if (!isOpened && state.currentValue != DragValue.Center) {
            state.animateTo(DragValue.Center)
        }
    }

    androidx.compose.runtime.LaunchedEffect(state.targetValue) {
        if (state.targetValue != DragValue.Center) {
            onOpened()
        } else {
            onClosed()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        val offset = state.requireOffset()
        val isThresholdReached = offset < -actionsWidthPx

        // Full Swipe Delete Background (Red feedback)
        val bgColor by animateColorAsState(
            targetValue = if (isThresholdReached) deleteColor.copy(alpha = 0.8f) else Color.Transparent,
            label = "fullSwipeBg"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(bgColor, RoundedCornerShape(16.dp)),
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

        // Background Actions (Buttons)
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
                ActionButton(archiveColor, Icons.Filled.Folder, "Move to Folder") {
                    onMoveToFolder(todo)
                }
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(deleteColor, Icons.Filled.Delete, "Delete") {
                    onDelete(todo)
                }
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = state.requireOffset().roundToInt(),
                        y = 0
                    )
                }
                .anchoredDraggable(state, Orientation.Horizontal)
        ) {
            TodoRow(todo = todo, onToggleDone = onToggleDone, onEdit = onEdit)
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
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
