package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesRepository
import com.example.fundoonotes.data.repository.SQLiteNoteRepository
import com.example.fundoonotes.data.repository.firebase.FirestoreNoteRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("SpellCheckingInspection")
class NotesDataBridge(private val context: Context) : NotesRepository {

    companion object{
        private const val TAG = "NotesDataBridge"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()
    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private val sqliteNoteRepository: SQLiteNoteRepository = SQLiteNoteRepository(context)

    private var activeRepository: NotesRepository = firestoreRepository

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

    // Add these methods to NotesDataBridge.kt

    fun addNewNoteWithLabels(title: String, description: String, reminderTime: Long?, labels: List<String>): String {
        val noteId = activeRepository.addNewNote(title, description, reminderTime)

        // Update the note with labels
        if (noteId.isNotEmpty() && labels.isNotEmpty()) {
            updateNoteLabels(noteId, labels)
        }

        return noteId
    }

    fun updateNoteWithLabels(noteId: String, title: String, description: String, reminderTime: Long?, labels: List<String>) {
        activeRepository.updateNote(noteId, title, description, reminderTime)

        // Update the note's labels
        updateNoteLabels(noteId, labels)
    }

    private fun updateNoteLabels(noteId: String, labels: List<String>) {
        // Get the current note
        val currentNote = getNoteById(noteId)

        // If the note exists locally, update it with new labels
        currentNote?.let { note ->
            val updatedNote = note.copy(labels = labels)

            // Update the note in Firestore
            if (activeRepository is FirestoreNoteRepository) {
                val db = Firebase.firestore
                db.collection("notes").document(noteId)
                    .update("labels", labels)
                    .addOnSuccessListener {
                        Log.d("NotesDataBridge", "Note labels updated successfully for $noteId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotesDataBridge", "Error updating note labels", e)
                    }
            }
        }
    }
}