package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.data.repository.firebase.FirestoreNoteRepository
import com.example.fundoonotes.data.repository.interfaces.NotesRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NoteLabelRepository(context: Context) {

    private val notesDataBridge = NotesDataBridge(context)
    private val labelDataBridge = LabelDataBridge(context)

    companion object {
        private const val TAG = "NoteLabelRepository"
    }

    fun addNewNoteWithLabels(title: String, description: String, reminderTime: Long?, labels: List<String>): String {
        val noteId = notesDataBridge.addNewNote(title, description, reminderTime)

        // Update the note with labels
        if (noteId.isNotEmpty() && labels.isNotEmpty()) {
            updateNoteLabels(noteId, labels)
        }

        return noteId
    }

    fun updateNoteWithLabels(noteId: String, title: String, description: String, reminderTime: Long?, labels: List<String>) {
        notesDataBridge.updateNote(noteId, title, description, reminderTime)

        // Update the note's labels
        updateNoteLabels(noteId, labels)
    }

    private fun updateNoteLabels(noteId: String, labels: List<String>) {
        // Get the current note
        notesDataBridge.fetchNoteById(noteId) { note ->
            // Update the labels directly in Firestore
            // This is a simplified approach since we don't have direct access to update labels through NotesDataBridge
            val db = Firebase.firestore
            db.collection("notes").document(noteId)
                .update("labels", labels)
                .addOnSuccessListener {
                    Log.d(TAG, "Note labels updated successfully for $noteId")
                    // Now update the bidirectional relationship
                    updateLabelsWithNoteReference(noteId, labels)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating note labels", e)
                }
        }
    }

    private fun updateLabelsWithNoteReference(noteId: String, labelIds: List<String>) {
        // First, fetch all labels to find which ones were previously associated with this note
        labelDataBridge.fetchLabels()

        // We need to observe the labels state to get current labels
        val currentLabels = labelDataBridge.labelsState.value

        // Find labels that contain this note ID
        val labelsWithThisNote = currentLabels.filter { it.noteIds.contains(noteId) }

        // Remove the note ID from labels that are no longer associated
        labelsWithThisNote.forEach { label ->
            if (!labelIds.contains(label.id)) {
                val updatedNoteIds = label.noteIds.filter { it != noteId }
                labelDataBridge.updateLabel(label.id, label.name, updatedNoteIds)
            }
        }

        // Add the note ID to newly associated labels
        labelIds.forEach { labelId ->
            // Find if the label exists in our current labels
            val label = currentLabels.find { it.id == labelId }
            if (label != null) {
                // If the note is not already in this label's noteIds, add it
                if (!label.noteIds.contains(noteId)) {
                    val updatedNoteIds = label.noteIds + noteId
                    labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
                }
            }
        }
    }

    fun getNoteLabels(noteId: String): List<String> {
        val note = notesDataBridge.getNoteById(noteId)
        return note?.labels ?: emptyList()
    }

    fun getNotesWithLabel(labelId: String, onSuccess: (List<Note>) -> Unit) {
        labelDataBridge.fetchLabelById(labelId) { label ->
            val notesWithLabel = label.noteIds.mapNotNull { noteId ->
                notesDataBridge.getNoteById(noteId)
            }
            onSuccess(notesWithLabel)
        }
    }

    // Helper method to fetch a note by ID
    private fun fetchNoteById(noteId: String): Note? {
        return notesDataBridge.getNoteById(noteId)
    }
}