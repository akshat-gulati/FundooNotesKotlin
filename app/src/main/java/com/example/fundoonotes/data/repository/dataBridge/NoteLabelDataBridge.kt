package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import com.example.fundoonotes.data.model.Note

class NoteLabelDataBridge(context: Context) {

    // ==============================================
    // Dependencies and Initialization
    // ==============================================
    private val notesDataBridge = NotesDataBridge(context)
    private val labelDataBridge = LabelDataBridge(context)

    companion object { private const val TAG = "NoteLabelRepository" }

    // ==============================================
    // Core Note-Label Operations
    // ==============================================
    fun addNewNoteWithLabels(
        noteId: String,
        title: String,
        description: String,
        reminderTime: Long?,
        labels: List<String>
    ): String {
        val noteId = notesDataBridge.addNewNote(noteId, title, description, reminderTime)

        // Update the note with labels
        if (noteId.isNotEmpty() && labels.isNotEmpty()) {
            updateNoteLabels(noteId, labels)
        }

        return noteId
    }

    fun updateNoteWithLabels(
        noteId: String,
        title: String,
        description: String,
        reminderTime: Long?,
        labels: List<String>
    ) {
        notesDataBridge.updateNote(noteId, title, description, reminderTime)
        updateNoteLabels(noteId, labels)
    }

    // ==============================================
    // Label Management Methods
    // ==============================================
    internal fun updateNoteLabels(noteId: String, labels: List<String>) {
        notesDataBridge.fetchNoteById(noteId) { note ->
            notesDataBridge.updateNoteLabels(noteId, labels)
            updateLabelsWithNoteReference(noteId, labels)
        }
    }

    internal fun updateLabelsWithNoteReference(noteId: String, labelIds: List<String>) {
        labelDataBridge.fetchLabels()
        val currentLabels = labelDataBridge.labelsState.value

        // Remove note from old labels
        currentLabels.filter { it.noteIds.contains(noteId) }
            .forEach { label ->
                if (!labelIds.contains(label.id)) {
                    val updatedNoteIds = label.noteIds.filter { it != noteId }
                    labelDataBridge.updateLabel(label.id, label.name, updatedNoteIds)
                }
            }

        // Add note to new labels
        labelIds.forEach { labelId ->
            currentLabels.find { it.id == labelId }?.let { label ->
                if (!label.noteIds.contains(noteId)) {
                    val updatedNoteIds = label.noteIds + noteId
                    labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
                }
            }
        }
    }

    // ==============================================
    // Query Methods
    // ==============================================
    fun getNoteLabels(noteId: String): List<String> {
        return notesDataBridge.getNoteById(noteId)?.labels ?: emptyList()
    }

    fun getNotesWithLabel(labelId: String, onSuccess: (List<Note>) -> Unit) {
        labelDataBridge.fetchLabelById(labelId) { label ->
            val notesWithLabel = label.noteIds.mapNotNull { noteId ->
                notesDataBridge.getNoteById(noteId)
            }
            onSuccess(notesWithLabel)
        }
    }

    // ==============================================
    // Deletion Methods
    // ==============================================
    fun deleteLabel(labelId: String) {
        getNotesWithLabel(labelId) { notes ->
            if (notes.isEmpty()) {
                labelDataBridge.deleteLabel(labelId)
            } else {
                notes.forEach { note ->
                    val updatedLabels = note.labels.filter { it != labelId }
                    notesDataBridge.updateNoteLabels(note.id, updatedLabels)
                }
                labelDataBridge.deleteLabel(labelId)
            }
        }
    }

    fun deleteNote(noteId: String) {
        notesDataBridge.deleteNote(noteId)
        notesDataBridge.fetchNoteById(noteId) { note ->
            note.labels.forEach { labelId ->
                labelDataBridge.fetchLabelById(labelId) { label ->
                    val updatedNoteIds = label.noteIds.filter { it != noteId }
                    labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
                }
            }
        }
    }
}