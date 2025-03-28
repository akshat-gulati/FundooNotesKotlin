package com.example.fundoonotes.data.repository

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.fundoonotes.data.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.java

class FirebaseDatabaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    init {
        fetchNotes()
    }

    private fun fetchNotes() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("notes").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { result ->
                val notes = result.map { document -> document.toObject(Note::class.java) }
                _notesState.value = notes
            }
            .addOnFailureListener { e -> Log.w("Firestore", "Error getting notes", e) }
    }

    fun addNote(note: Note) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val noteWithUserId = note.copy(userId = userId)
        db.collection("notes").document(note.id).set(noteWithUserId)
            .addOnSuccessListener { fetchNotes() }
            .addOnFailureListener { e -> Log.w("Firestore", "Error adding note", e) }
    }

    fun deleteNote(noteId: String) {
        db.collection("notes").document(noteId).delete()
            .addOnSuccessListener { fetchNotes() }
            .addOnFailureListener { e -> Log.w("Firestore", "Error deleting note", e) }
    }

    fun updateNote(updatedNote: Note) {
        db.collection("notes").document(updatedNote.id).set(updatedNote)
            .addOnSuccessListener { fetchNotes() }
            .addOnFailureListener { e -> Log.w("Firestore", "Error updating note", e) }
    }
}