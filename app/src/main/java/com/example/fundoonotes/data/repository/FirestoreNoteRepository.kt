package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.google.firebase.auth.FirebaseAuth
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

    // Shared Preferences to get user ID
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    init {
        fetchNotes()
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
        val userId = getUserId() ?: return
        db.collection("notes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val notes = result.documents.map { document ->
                    document.toObject(Note::class.java)?.copy(id = document.id) ?: Note()
                }
                _notesState.value = notes
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error getting notes", e)
            }
    }

    override fun addNewNote(title: String, description: String): String {
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
            description = description
        )

        Log.d("NoteRepository", "Attempting to add note: $note")

        noteRef.set(note)
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note added successfully: ${note.id}")
                fetchNotes()
            }
            .addOnFailureListener { e ->
                Log.e("NoteRepository", "Error adding note", e)
            }

        return noteRef.id
    }

    override fun updateNote(noteId: String, title: String, description: String) {
        val userId = getUserId() ?: return

        val updatedNote = mapOf(
            "title" to title,
            "description" to description
        )

        db.collection("notes").document(noteId)
            .update(updatedNote)
            .addOnSuccessListener {
                fetchNotes()
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error updating note", e)
            }
    }

    override fun deleteNote(noteId: String) {
        db.collection("notes").document(noteId)
            .delete()
            .addOnSuccessListener {
                fetchNotes()
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
}