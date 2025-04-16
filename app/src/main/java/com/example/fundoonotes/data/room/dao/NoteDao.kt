package com.example.fundoonotes.data.repository.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("UPDATE notes SET title = :title, description = :description, reminderTime = :reminderTime WHERE id = :noteId")
    suspend fun updateNoteContent(noteId: String, title: String, description: String, reminderTime: Long?)

    @Query("UPDATE notes SET labels = :labels WHERE id = :noteId")
    suspend fun updateNoteLabels(noteId: String, labels: String)

    @Query("UPDATE notes SET deleted = :deleted, deletedTime = :deletedTime WHERE id = :noteId")
    suspend fun updateNoteDeletedStatus(noteId: String, deleted: Boolean, deletedTime: Long?)

    @Query("UPDATE notes SET archived = :archived WHERE id = :noteId")
    suspend fun updateNoteArchivedStatus(noteId: String, archived: Boolean)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}