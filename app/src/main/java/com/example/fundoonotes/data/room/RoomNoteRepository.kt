package com.example.fundoonotes.data.repository.room

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomNoteRepository(context: Context) : NotesInterface {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val noteDao = AppDatabase.getDatabase(context).noteDao()
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    companion object {
        private const val TAG = "RoomNoteRepository"
    }

    init {
        // Start collecting notes from Room database
        setupRealtimeUpdates()
    }

    private fun setupRealtimeUpdates() {
        coroutineScope.launch {
            noteDao.getAllNotes()
                .map { entityList -> entityList.map { it.toNote() } }
                .collect { notes ->
                    _notesState.value = notes
                    Log.d(TAG, "Real-time update received: ${notes.size} notes")
                }
        }
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        coroutineScope.launch {
            val noteEntity = noteDao.getNoteById(noteId)
            noteEntity?.let {
                val note = it.toNote()
                // Switch to main thread to deliver the result
                withContext(Dispatchers.Main) {
                    onSuccess(note)
                }
            }
        }
    }

    override fun fetchNotes() {
        // No need to explicitly fetch notes since we're using Flow
        // The StateFlow is automatically updated by the Room Flow collection
    }

    override fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String {
        val note = Note(
            id = noteId,
            title = title,
            description = description,
            reminderTime = reminderTime,
            timestamp = System.currentTimeMillis()
        )

        coroutineScope.launch {
            try {
                val noteEntity = NoteEntity.fromNote(note)
                noteDao.insertNote(noteEntity)
                Log.d(TAG, "Note successfully added to Room with ID: $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding note to Room: ${e.message}")
            }
        }

        return noteId
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        coroutineScope.launch {
            try {
                noteDao.updateNoteContent(noteId, title, description, reminderTime)
                Log.d(TAG, "Note updated successfully: $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating note: ${e.message}")
            }
        }
    }

    override fun deleteNote(noteId: String) {
        coroutineScope.launch {
            try {
                noteDao.deleteNote(noteId)
                Log.d(TAG, "Note deleted successfully: $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note: ${e.message}")
            }
        }
    }

    fun updateNoteFields(noteId: String, fields: Map<String, Any?>) {
        coroutineScope.launch {
            try {
                // Get the current note entity
                val currentNoteEntity = noteDao.getNoteById(noteId)
                if (currentNoteEntity != null) {
                    val currentNote = currentNoteEntity.toNote()

                    // Process each field
                    fields.forEach { (key, value) ->
                        when (key) {
                            "title" -> updateNoteContent(noteId, value as String, currentNote.description, currentNote.reminderTime)
                            "description" -> updateNoteContent(noteId, currentNote.title, value as String, currentNote.reminderTime)
                            "labels" -> value?.let {
                                val labelsStr = (it as List<String>).joinToString(",")
                                noteDao.updateNoteLabels(noteId, labelsStr)
                            }
                            "deleted" -> noteDao.updateNoteDeletedStatus(noteId, value as Boolean,
                                if (value) System.currentTimeMillis() else null)
                            "archived" -> noteDao.updateNoteArchivedStatus(noteId, value as Boolean)
                            "reminderTime" -> updateNoteContent(noteId, currentNote.title, currentNote.description, value as Long?)
                            "deletedTime" -> if (currentNote.deleted) {
                                noteDao.updateNoteDeletedStatus(noteId, true, value as Long?)
                            }
                            "timestamp" -> {
                                // Handling timestamp updates would require a custom query or updating the entire entity
                                val updatedNote = currentNote.copy(timestamp = value as Long)
                                noteDao.insertNote(NoteEntity.fromNote(updatedNote))
                            }
                        }
                    }

                    Log.d(TAG, "Note fields updated successfully: $noteId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating note fields: ${e.message}")
            }
        }
    }

    private fun updateNoteContent(noteId: String, title: String, description: String, reminderTime: Long?) {
        coroutineScope.launch {
            try {
                noteDao.updateNoteContent(noteId, title, description, reminderTime)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating note content: ${e.message}")
            }
        }
    }

    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    fun clearAllData() {
        coroutineScope.launch {
            try {
                noteDao.clearAllNotes()
                Log.d(TAG, "All data cleared from Room database")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing data: ${e.message}")
            }
        }
    }
}