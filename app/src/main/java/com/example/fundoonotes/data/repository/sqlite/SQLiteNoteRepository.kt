package com.example.fundoonotes.data.repository.sqlite

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SQLiteNoteRepository(private val context: Context): NotesInterface {
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    companion object{
        private const val TAG = "SQLiteRepository"
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {

        Log.d(TAG, "fetchNoteById not yet implemented")
    }

    override fun fetchNotes() {
        Log.d(TAG, "fetchNotes not yet implemented")
    }

    override fun addNewNote(title: String, description: String, reminderTime: Long?): String {
        Log.d(TAG, "addNewNote not yet implemented")
        return ""
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        Log.d(TAG, "updateNote not yet implemented")
    }

    override fun deleteNote(noteId: String) {
        Log.d(TAG, "deleteNote not yet implemented")
    }
}