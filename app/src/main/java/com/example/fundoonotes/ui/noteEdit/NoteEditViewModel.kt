package com.example.fundoonotes.ui.noteEdit

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.NoteLabelDataBridge
import com.example.fundoonotes.data.repository.reminder.WorkManagerReminderScheduler
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.ui.notes.NoteViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.filter

class NoteEditViewModel(application: Application) : AndroidViewModel(application) {

    // ==============================================
    // Dependencies
    // ==============================================
    private val notesDataBridge: NotesDataBridge
    private val labelDataBridge: LabelDataBridge
    private val noteLabelDataBridge: NoteLabelDataBridge
    private val reminderScheduler: WorkManagerReminderScheduler


    // ==============================================
    // StateFlow Declarations
    // ==============================================
    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _noteLabels = MutableStateFlow<List<String>>(emptyList())
    val noteLabels: StateFlow<List<String>> = _noteLabels.asStateFlow()

    private val _isNewNote = MutableStateFlow(false)
    val isNewNote: StateFlow<Boolean> = _isNewNote.asStateFlow()

    private val _createNoteArchived = MutableStateFlow<Boolean>(false)
    val createNoteArchived: StateFlow<Boolean> = _createNoteArchived

    private val _reminderTime = MutableStateFlow<Long?>(null)
    val reminderTime: StateFlow<Long?> = _reminderTime.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    private val _availableLabels = MutableStateFlow<List<Label>>(emptyList())
    val availableLabels: StateFlow<List<Label>> = _availableLabels.asStateFlow()

    private var currentNoteId: String? = null

    // ==============================================
    // Initialization
    // ==============================================
    init {
        notesDataBridge = NotesDataBridge(application.applicationContext)
        labelDataBridge = LabelDataBridge(application.applicationContext)
        noteLabelDataBridge = NoteLabelDataBridge(application.applicationContext)
        reminderScheduler = WorkManagerReminderScheduler(application.applicationContext)

        // Fetch labels initially
        viewModelScope.launch {
            labelDataBridge.labelsState.collect { labels ->
                _availableLabels.value = labels
            }
        }
    }

    // ==============================================
    // Note Loading Methods
    // ==============================================
    fun loadNote(noteId: String?) {
        if (noteId != null) {
            _isNewNote.value = false
            currentNoteId = noteId
            loadNoteDetails(noteId)
        } else {
            _isNewNote.value = true
            currentNoteId = UUID.randomUUID().toString()
            _noteLabels.value = emptyList()
        }
    }

    private fun loadNoteDetails(noteId: String) {
        val note = notesDataBridge.getNoteById(noteId)

        if (note != null) {
            _currentNote.value = note
            _noteLabels.value = note.labels
            _reminderTime.value = note.reminderTime
        } else {
            notesDataBridge.fetchNoteById(noteId) { fetchedNote ->
                _currentNote.value = fetchedNote
                _noteLabels.value = fetchedNote.labels
                _reminderTime.value = fetchedNote.reminderTime
            }
        }
    }

    // ==============================================
    // Reminder Methods
    // ==============================================
    fun setReminderTime(time: Long) {
        if (time <= System.currentTimeMillis()) {
            _errorMessage.value = "Cannot set reminder in the past"
            return
        }
        _reminderTime.value = time
    }
    fun cancelReminder() {
        // If this is an existing note with a reminder
        currentNoteId?.let { id ->
            val existingNote = notesDataBridge.getNoteById(id)

            if (existingNote != null && existingNote.reminderTime != null) {
                // Cancel the scheduled reminder
                reminderScheduler.cancelReminder(id)
            }
        }

        // Clear the reminder time
        _reminderTime.value = null
    }

    // ==============================================
    // Archive Methods
    // ==============================================
    fun toggleArchive() {
        currentNoteId?.let { id ->
            val note = notesDataBridge.getNoteById(id)
            if (note != null) {
                viewModelScope.launch {
                    try {
                        notesDataBridge.toggleNoteToArchive(id)
                        if (note.archived){

                            Toast.makeText(getApplication(), "This Note have been Unarchived", Toast.LENGTH_LONG).show()
                        }
                        else{
                            Toast.makeText(getApplication(), "This Note have been Archived", Toast.LENGTH_LONG).show()
                        }

                    } catch (e: Exception) {
                        _errorMessage.value = "Error toggling note to archive: ${e.message}"
                    }
                }
            } else {
                _createNoteArchived.value = true
            }
        }
    }

    // ==============================================
    // Label Management Methods
    // ==============================================
    fun addLabel(label: Label) {
        val currentLabels = _noteLabels.value?.toMutableList() ?: mutableListOf()
        if (!currentLabels.contains(label.id)) {
            currentLabels.add(label.id)
            _noteLabels.value = currentLabels

            // If note exists, update the relationship
            currentNoteId?.let { id ->
                addNoteToLabel(id, label.id)
            }
        }
    }

    fun removeLabel(labelId: String) {
        val currentLabels = _noteLabels.value?.toMutableList() ?: mutableListOf()
        currentLabels.remove(labelId)
        _noteLabels.value = currentLabels

        // If note exists, update the relationship
        currentNoteId?.let { id ->
            removeNoteFromLabel(id, labelId)
        }
    }

    private fun addNoteToLabel(noteId: String, labelId: String) {
        labelDataBridge.fetchLabelById(labelId) { label ->
            val updatedNoteIds = if (!label.noteIds.contains(noteId)) {
                label.noteIds + noteId
            } else {
                label.noteIds
            }
            labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
        }
    }

    private fun removeNoteFromLabel(noteId: String, labelId: String) {
        labelDataBridge.fetchLabelById(labelId) { label ->
            val updatedNoteIds = label.noteIds.filter { it != noteId }
            labelDataBridge.updateLabel(labelId, label.name, updatedNoteIds)
        }
    }

    // ==============================================
    // Note Save Methods
    // ==============================================
    fun saveNote(title: String, description: String) {
        if (title.isBlank() && description.isBlank()) {
            Log.d(TAG, "No content to save")
            return
        }

        currentNoteId?.let { id ->
            val existingNote = notesDataBridge.getNoteById(id)

            if (existingNote != null) {
                // Update existing note
                Log.d(TAG, "Updating existing note: $id")
                noteLabelDataBridge.updateNoteWithLabels(
                    id,
                    title,
                    description,
                    _reminderTime.value,
                    _noteLabels.value ?: emptyList()
                )

                // Schedule reminder if needed
                _reminderTime.value?.let { time ->
                    Log.d(TAG, "Scheduling reminder for existing note at time: $time")
                    reminderScheduler.scheduleReminder(existingNote.copy(reminderTime = time), time)
                }
            } else {
                // Add new note
                Log.d(TAG, "Adding new note with ID: $id")
                noteLabelDataBridge.addNewNoteWithLabels(
                    id,
                    title,
                    description,
                    _reminderTime.value,
                    _noteLabels.value ?: emptyList()
                )

                // Schedule reminder if needed
                _reminderTime.value?.let { time ->
                    val newNote = Note(
                        id = id,
                        title = title,
                        description = description,
                        reminderTime = time,
                        labels = _noteLabels.value ?: emptyList()
                    )
                    Log.d(TAG, "Scheduling reminder for new note at time: $time")
                    reminderScheduler.scheduleReminder(newNote, time)
                }

                if (_createNoteArchived.value == true) {
                    notesDataBridge.toggleNoteToArchive(id)
                }
            }

            _success.value = true
        }

    }

    // ==============================================
    // Label Utility Methods
    // ==============================================
    fun getAvailableLabelsForDialog(): List<Label> {
        return _availableLabels.value?.filter { it.id !in (_noteLabels.value ?: emptyList()) } ?: emptyList()
    }

    // ==============================================
    // Companion Object
    // ==============================================
    companion object {
        private const val TAG = "NoteEditViewModel"
    }
}