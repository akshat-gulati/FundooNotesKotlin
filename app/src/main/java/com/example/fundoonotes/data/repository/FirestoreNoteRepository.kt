package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreNoteRepository(private val context: Context): NotesRepository {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    // For real-time updates
    private var notesListener: ListenerRegistration? = null

    // Shared Preferences to get user ID
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    init {
        setupRealtimeUpdates()
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        db.collection("notes").document(noteId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val note = document.toObject(Note::class.java)?.copy(id = document.id)
                    note?.let { onSuccess(it) }
                } else {
                    Log.d("NoteRepository", "No such document with ID: $noteId")
                }
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error getting note", e)
            }
    }

    override fun fetchNotes() {
        // This method is now mainly used for initial data loading or manual refresh
        // Real-time updates will happen through the listener
        setupRealtimeUpdates()
    }

    private fun setupRealtimeUpdates() {
        // Remove any existing listener
        notesListener?.remove()

        val userId = getUserId() ?: return

        // Set up real-time listener for notes collection
        notesListener = db.collection("notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("NoteRepository", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notes = snapshot.documents.map { document ->
                        document.toObject(Note::class.java)?.copy(id = document.id) ?: Note()
                    }
                    _notesState.value = notes
                    Log.d("NoteRepository", "Real-time update received: ${notes.size} notes")
                }
            }
    }

    override fun addNewNote(title: String, description: String, reminderTime: Long?): String {
        val userId = getUserId()
        Log.d("NoteRepository", "User ID: $userId")

        if (userId == null) {
            Log.e("NoteRepository", "No user ID found. Cannot add note.")
            return ""
        }

        // Use Firestore's auto-generated ID
        val noteRef = db.collection("notes").document()
        val note = Note(
            id = noteRef.id,
            userId = userId,
            title = title,
            description = description,
            reminderTime = reminderTime
        )

        Log.d("NoteRepository", "Attempting to add note: $note")

        noteRef.set(note)
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note added successfully: ${note.id}")
                // No need to manually fetch notes here, the listener will handle it
            }
            .addOnFailureListener { e ->
                Log.e("NoteRepository", "Error adding note", e)
            }

        return noteRef.id
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        val updatedNote = mapOf(
            "title" to title,
            "description" to description,
            "reminderTime" to reminderTime
        )

        db.collection("notes").document(noteId)
            .update(updatedNote)
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note updated successfully: $noteId")
                // No need to manually fetch notes here, the listener will handle it
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error updating note", e)
            }
    }

    override fun deleteNote(noteId: String) {
        db.collection("notes").document(noteId)
            .delete()
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note deleted successfully: $noteId")
                // No need to manually fetch notes here, the listener will handle it
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error deleting note", e)
            }
    }

    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    private fun getUserId(): String? {
        val userId = sharedPreferences.getString("userId", null)
        Log.d("NoteRepository", "Retrieved User ID: $userId")
        return userId
    }

    // Clean up method to be called when the repository is no longer needed
    fun cleanup() {
        notesListener?.remove()
    }
}