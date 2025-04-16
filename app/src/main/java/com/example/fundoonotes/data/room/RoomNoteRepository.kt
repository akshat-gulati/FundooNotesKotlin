package com.example.fundoonotes.data.room

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import com.example.fundoonotes.data.room.entity.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomNoteRepository(context: Context) {
    private val noteDao = AppDatabase.getDatabase(context).noteDao()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    private var notesJob: Job? = null

    init {
        fetchNotes()
    }

    suspend fun fetchNoteById(
        noteId: String,
        onSuccess: (Note) -> Unit,
        onError: (Exception) -> Unit = { Log.e("RoomNoteRepository", "Error fetching note: ${it.message}") }
    ) {
        try {
            val noteEntity = noteDao.getNoteById(noteId)
            noteEntity?.let {
                onSuccess(it.toNote())
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun fetchNotes() {
        notesJob?.cancel()
        notesJob = coroutineScope.launch {
            try {
                noteDao.getAllNotes().collect { notes ->
                    _notesState.value = notes.map { it.toNote() }
                }
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error fetching notes: ${e.message}")
            }
        }
    }

    suspend fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String {
        return withContext(Dispatchers.IO) {
            try {
                val note = NoteEntity(
                    id = noteId,
                    title = title,
                    description = description,
                    timestamp = System.currentTimeMillis(),
                    labels = "",
                    deleted = false,
                    archived = false,
                    reminderTime = reminderTime,
                    deletedTime = null
                )
                noteDao.insert(note)
                noteId
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error adding note: ${e.message}")
                throw e
            }
        }
    }

    suspend fun updateNote(
        noteId: String,
        title: String,
        description: String,
        reminderTime: Long?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val note = noteDao.getNoteById(noteId)
                note?.let {
                    noteDao.update(
                        it.copy(
                            title = title,
                            description = description,
                            reminderTime = reminderTime
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error updating note: ${e.message}")
                throw e
            }
        }
    }

    suspend fun deleteNote(noteId: String) {
        withContext(Dispatchers.IO) {
            try {
                noteDao.getNoteById(noteId)?.let {
                    noteDao.delete(it)
                }
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error deleting note: ${e.message}")
                throw e
            }
        }
    }

    suspend fun updateNoteFields(noteId: String, fields: Map<String, Any?>) {
        withContext(Dispatchers.IO) {
            try {
                fields.forEach { (key, value) ->
                    when (key) {
                        "title" -> noteDao.updateTitle(noteId, value as String)
                        "description" -> noteDao.updateDescription(noteId, value as String)
                        "reminderTime" -> noteDao.updateReminderTime(noteId, value as Long?)
                        "labels" -> noteDao.updateLabels(noteId, value as String)
                        "deleted" -> noteDao.updateDeleted(noteId, value as Boolean)
                        "archived" -> noteDao.updateArchived(noteId, value as Boolean)
                        "deletedTime" -> noteDao.updateDeletedTime(noteId, value as Long?)
                    }
                }
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error updating note fields: ${e.message}")
                throw e
            }
        }
    }

    suspend fun getNoteById(noteId: String): Note? {
        return withContext(Dispatchers.IO) {
            try {
                noteDao.getNoteById(noteId)?.toNote()
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error getting note by ID: ${e.message}")
                null
            }
        }
    }

    fun cleanup() {
        notesJob?.cancel()
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            try {
                noteDao.clearAllNotes()
            } catch (e: Exception) {
                Log.e("RoomNoteRepository", "Error clearing notes: ${e.message}")
                throw e
            }
        }
    }

    fun getArchivedNotes(): Flow<List<Note>> {
        return noteDao.getArchivedNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }
}