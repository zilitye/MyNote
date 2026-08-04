package com.example.mynote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single note.
 *
 * [folder] is a free-form label; a note with a null/blank folder is
 * "Uncategorized". [isFavorite] surfaces the note under My Favorites.
 * [isDeleted]/[deletedAt] implement a soft-delete "Recently Deleted" trash,
 * matching how most notepad apps let you undo an accidental delete.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val folder: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * A short one-line preview of the note body, used on the notes grid,
     * similar to how most notepad apps show a snippet under the title.
     */
    fun preview(maxLength: Int = 40): String {
        val singleLine = content.replace("\n", " ").trim()
        return if (singleLine.length > maxLength) {
            singleLine.take(maxLength) + "…"
        } else {
            singleLine
        }
    }
}
