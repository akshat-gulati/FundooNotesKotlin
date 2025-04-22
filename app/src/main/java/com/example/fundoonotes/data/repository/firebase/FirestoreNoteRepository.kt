package com.example.fundoonotes.data.repository.firebase

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreNoteRepository(private val context: Context): NotesInterface {

    // ==============================================
    // Dependencies and Initialization
    // ==============================================
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()
    private var notesListener: ListenerRegistration? = null
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    init {
        setupRealtimeUpdates()
    }

    // ==============================================
    // Note Fetching Operations
    // ==============================================
    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        db.collection("notes").document(noteId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Note::class.java)?.copy(id = document.id)?.let(onSuccess)
                    ?: Log.d("NoteRepository", "No such document with ID: $noteId")
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error getting note", e)
            }
    }

    override fun fetchNotes() {
        setupRealtimeUpdates()
    }

    private fun setupRealtimeUpdates() {
        notesListener?.remove()
        getUserId()?.let { userId ->
            notesListener = db.collection("notes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("NoteRepository", "Listen failed.", error)
                        return@addSnapshotListener
                    }
                    snapshot?.documents?.let { documents ->
                        _notesState.value = documents.map { doc ->
                            doc.toObject(Note::class.java)?.copy(id = doc.id) ?: Note()
                        }
                        Log.d("NoteRepository", "Real-time update received: ${documents.size} notes")
                    }
                }
        }
    }

    // ==============================================
    // Note CRUD Operations
    // ==============================================
    override fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String {
        val userId = getUserId() ?: run {
            Log.e("NoteRepository", "No user ID found")
            return ""
        }

        val noteRef = db.collection("notes").document(noteId)
        val note = Note(
            id = noteId,
            userId = userId,
            title = title,
            description = description,
            reminderTime = reminderTime
        )

        noteRef.set(note)
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note added successfully: ${note.id}")
            }
            .addOnFailureListener { e ->
                Log.e("NoteRepository", "Error adding note", e)
            }

        return noteRef.id
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        db.collection("notes").document(noteId)
            .update(mapOf(
                "title" to title,
                "description" to description,
                "reminderTime" to reminderTime
            ))
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note updated successfully: $noteId")
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error updating note", e)
            }
    }

    fun updateNoteFields(noteId: String, fields: Map<String, Any?>) {
        db.collection("notes").document(noteId)
            .update(fields)
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note fields updated successfully: $noteId")
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error updating note fields", e)
            }
    }

    override fun deleteNote(noteId: String) {
        db.collection("notes").document(noteId)
            .delete()
            .addOnSuccessListener {
                Log.d("NoteRepository", "Note deleted successfully: $noteId")
            }
            .addOnFailureListener { e ->
                Log.w("NoteRepository", "Error deleting note", e)
            }
    }

    // ==============================================
    // Utility Methods
    // ==============================================

    private fun getUserId(): String? {
        return sharedPreferences.getString("userId", null).also {
            Log.d("NoteRepository", "Retrieved User ID: $it")
        }
    }
}