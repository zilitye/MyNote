package com.example.mynote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashedNotesFlow(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query(
        "UPDATE notes SET title = :title, content = :content, folder = :folder, " +
            "isFavorite = :isFavorite, isPinned = :isPinned, isArchived = :isArchived, " +
            "updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateNoteFields(
        id: Long,
        title: String,
        content: String,
        folder: String?,
        isFavorite: Boolean,
        isPinned: Boolean,
        isArchived: Boolean,
        updatedAt: Long
    )

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE notes SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun moveToTrashBatch(ids: List<Long>, deletedAt: Long)

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNotePermanently(id: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM todos WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllTodosFlow(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashedTodosFlow(): Flow<List<Todo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long

    @Update
    suspend fun updateTodo(todo: Todo)

    @Query("UPDATE todos SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrashTodo(id: Long, deletedAt: Long)

    @Query("UPDATE todos SET isDeleted = 1, deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun moveToTrashTodoBatch(ids: List<Long>, deletedAt: Long)

    @Query("UPDATE todos SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTodo(id: Long)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoPermanently(id: Long)

    @Query("DELETE FROM todos WHERE isDeleted = 1")
    suspend fun emptyTodoTrash()

    @Query("DELETE FROM todos WHERE isDone = 1 AND isDeleted = 0")
    suspend fun deleteCompletedTodos()
}
