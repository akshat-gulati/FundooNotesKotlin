package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import com.example.fundoonotes.data.repository.SQLiteNoteRepository
import com.example.fundoonotes.data.repository.firebase.FirestoreNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("SpellCheckingInspection")
class NotesDataBridge(private val context: Context) : NotesInterface {

    companion object{
        private const val TAG = "NotesDataBridge"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()
    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private val sqliteNoteRepository: SQLiteNoteRepository = SQLiteNoteRepository(context)

    private var activeRepository: NotesInterface = firestoreRepository

    init {
        observeFirestoreNotes()
    }

    private fun observeFirestoreNotes() {
        coroutineScope.launch {
            firestoreRepository.notesState.collect { notes ->
                _notesState.value = notes
                Log.d(TAG, "New notes received: ${notes.size}")
            }
        }
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        activeRepository.fetchNoteById(noteId, onSuccess)
    }

    override fun fetchNotes() {
        activeRepository.fetchNotes()
    }

    override fun addNewNote(title: String, description: String, reminderTime: Long?): String {
        return activeRepository.addNewNote(title, description, reminderTime)
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        activeRepository.updateNote(noteId, title, description, reminderTime)
    }

    override fun deleteNote(noteId: String) {
        activeRepository.deleteNote(noteId)
    }

    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    // Method to update a note's labels
    fun updateNoteLabels(noteId: String, labels: List<String>) {
        // First, get the current note to preserve its other properties
        fetchNoteById(noteId) { note ->
            // Extract the relevant properties we want to keep
            val updatedFields = mapOf("labels" to labels)

            // Use the appropriate repository to update the note
            if (activeRepository == firestoreRepository) {
                firestoreRepository.updateNoteFields(noteId, updatedFields)
            }
//            else {
//                sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
//            }
        }
    }

    fun switchRepository(repositoryType: RepositoryType) {
        // Clean up the current repository if it's Firestore
        if (activeRepository == firestoreRepository) {
            firestoreRepository.cleanup()
        }

        activeRepository = when (repositoryType) {
            RepositoryType.FIRESTORE -> firestoreRepository
            RepositoryType.SQLITE -> {
                Log.d(TAG, "SQLite repository not yet implemented")
                firestoreRepository
            }
        }
        fetchNotes()
    }

    fun toggleNoteToTrash(noteId: String) {
        fetchNoteById(noteId) { note ->
            val updatedFields = if (note.deleted == true) {
                mapOf(
                    "deleted" to false,
                    "deletedTime" to null
                )
            } else {
                mapOf(
                    "deleted" to true,
                    "deletedTime" to System.currentTimeMillis()
                )
            }

            if (activeRepository == firestoreRepository) {
                firestoreRepository.updateNoteFields(noteId, updatedFields)
            }
            // Add SQLite implementation when needed
        }
    }

    fun moveNoteToArchive(noteId: String) {
        fetchNoteById(noteId) { note ->
            val updatedFields = mapOf(
                "archived" to true
            )

            if (activeRepository == firestoreRepository) {
                firestoreRepository.updateNoteFields(noteId, updatedFields)
            }
        }
    }

    fun syncRepositories() {
        Log.d(TAG, "Repository sync not yet implemented")
    }

    // Method to clean up resources when no longer needed
    fun cleanup() {
        if (activeRepository == firestoreRepository) {
            firestoreRepository.cleanup()
        }
    }

    enum class RepositoryType {
        FIRESTORE,
        SQLITE
    }
}