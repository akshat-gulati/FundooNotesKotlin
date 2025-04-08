package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge

class NoteLabelRepository(context: Context) {

    private val notesDataBridge = NotesDataBridge(context)
    private val labelDataBridge = LabelDataBridge(context)

    companion object {
        private const val TAG = "NoteLabelRepository"
    }

    fun addNewNoteWithLabels(
        title: String,
        description: String,
        reminderTime: Long?,
        labels: List<String>
    ): String {
        val noteId = notesDataBridge.addNewNote(title, description, reminderTime)

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

        // Update the note's labels
        updateNoteLabels(noteId, labels)
    }

    private fun updateNoteLabels(noteId: String, labels: List<String>) {
        // Get the current note
        notesDataBridge.fetchNoteById(noteId) { note ->
            // Create an updated note with new labels
            val updatedNote = note.copy(labels = labels)

            // Update the note using the DataBridge
            notesDataBridge.updateNoteLabels(noteId, labels)

            // Now update the bidirectional relationship
            updateLabelsWithNoteReference(noteId, labels)
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


    // This is a recursive function to ensure that the lable gets deleted only when there are no notes associated with it
    fun deleteLabel(labelId: String) {
        val associatedNoteIds = mutableListOf<Note>()
        getNotesWithLabel(labelId) { notes ->
            associatedNoteIds.addAll(notes)


            if (associatedNoteIds.isEmpty()) {
                labelDataBridge.deleteLabel(labelId)
            } else {
                associatedNoteIds.forEach { note ->
                    val noteId = note.id
                    notesDataBridge.fetchNoteById(noteId) {
                        val updatedLabels = note.labels.filter { it != labelId }
                        notesDataBridge.updateNoteLabels(noteId, updatedLabels)


                    }
                }
                labelDataBridge.deleteLabel(labelId)
            }
        }
    }

    fun deleteNote(noteId: String){
        notesDataBridge.fetchNoteById(noteId){ note ->
            val associatedLabelIds = note.labels

            associatedLabelIds.forEach{ labelId ->
                labelDataBridge.fetchLabelById(labelId){
                    val updatedNoteIds = it.noteIds.filter { it != noteId }
                    labelDataBridge.updateLabel(labelId, it.name, updatedNoteIds)
                }
            }
        }
    }


}