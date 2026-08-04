package com.example.mynote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single to-do item, similar to the "待办" tab in the reference app.
 *
 * [dueAt] is nullable because a to-do doesn't have to have a due date/time.
 */
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val isDone: Boolean = false,
    val isImportant: Boolean = false,
    val dueAt: Long? = null,
    val folder: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
