package com.example.fundoonotes.data.room.dao


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fundoonotes.data.room.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE deleted = 0")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE archived = 1 AND deleted = 0")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET title = :title WHERE id = :noteId")
    suspend fun updateTitle(noteId: String, title: String)

    @Query("UPDATE notes SET description = :description WHERE id = :noteId")
    suspend fun updateDescription(noteId: String, description: String)

    @Query("UPDATE notes SET reminderTime = :reminderTime WHERE id = :noteId")
    suspend fun updateReminderTime(noteId: String, reminderTime: Long?)

    @Query("UPDATE notes SET labels = :labels WHERE id = :noteId")
    suspend fun updateLabels(noteId: String, labels: String)

    @Query("UPDATE notes SET deleted = :deleted WHERE id = :noteId")
    suspend fun updateDeleted(noteId: String, deleted: Boolean)

    @Query("UPDATE notes SET archived = :archived WHERE id = :noteId")
    suspend fun updateArchived(noteId: String, archived: Boolean)

    @Query("UPDATE notes SET deletedTime = :deletedTime WHERE id = :noteId")
    suspend fun updateDeletedTime(noteId: String, deletedTime: Long?)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}