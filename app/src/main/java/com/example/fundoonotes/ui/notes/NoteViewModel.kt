package com.example.fundoonotes.ui.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.NoteLabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class NoteViewModel(context: Context) : ViewModel() {

    // ==============================================
    // Data Repositories
    // ==============================================
    private val notesDataBridge = NotesDataBridge(context)
    internal val labelDataBridge = LabelDataBridge(context)
    private val noteLabelDataBridge = NoteLabelDataBridge(context)

    // ==============================================
    // UI State Flows
    // ==============================================
    private val _displayMode = MutableStateFlow(NoteFragment.DISPLAY_NOTES)
    val displayMode: StateFlow<Int> = _displayMode

    private val _currentLabelId = MutableStateFlow<String?>(null)
    val currentLabelId: StateFlow<String?> = _currentLabelId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isGridLayout = MutableStateFlow(true)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout

    private val _filteredNotes = MutableStateFlow<List<Note>>(emptyList())
    val filteredNotes: StateFlow<List<Note>> = _filteredNotes

    private val _selectedNotes = MutableStateFlow<Set<Note>>(emptySet())
    val selectedNotes: StateFlow<Set<Note>> = _selectedNotes

    // ==============================================
    // Initialization
    // ==============================================
    init {
        setupNoteFiltering()
    }

    private fun setupNoteFiltering() {
        viewModelScope.launch {
            combine(
                notesDataBridge.notesState,
                displayMode,
                currentLabelId,
                searchQuery
            ) { notes, mode, labelId, query ->
                filterNotes(notes, mode, labelId, query)
            }.collect { filtered ->
                _filteredNotes.value = filtered
            }
        }
    }

    // ==============================================
    // Note Filtering Logic
    // ==============================================
    private fun filterNotes(
        notes: List<Note>,
        displayMode: Int,
        labelId: String?,
        searchQuery: String
    ): List<Note> {
        // First apply display mode filter
        val displayModeFiltered = when (displayMode) {
            NoteFragment.DISPLAY_NOTES -> notes.filter { !it.archived && !it.deleted }
            NoteFragment.DISPLAY_REMINDERS -> notes.filter { !it.archived && !it.deleted && it.reminderTime != null }
            NoteFragment.DISPLAY_ARCHIVE -> notes.filter { it.archived && !it.deleted }
            NoteFragment.DISPLAY_BIN -> notes.filter { it.deleted }
            NoteFragment.DISPLAY_LABELS -> {
                labelId?.let { id ->
                    notes.filter { !it.archived && !it.deleted && it.labels.contains(id) }
                } ?: emptyList()
            }
            else -> emptyList()
        }

        // Then apply search filter if any
        return if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase().trim()
            displayModeFiltered.filter { note ->
                note.title.lowercase().contains(query) ||
                        note.description.lowercase().contains(query)
            }
        } else {
            displayModeFiltered
        }
    }

    // ==============================================
    // UI State Updates
    // ==============================================
    fun updateDisplayMode(newMode: Int, labelId: String? = null) {
        _displayMode.value = newMode
        _currentLabelId.value = labelId
        _searchQuery.value = "" // Reset search when changing modes
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLayoutMode(isGrid: Boolean) {
        _isGridLayout.value = isGrid
    }

    // ==============================================
    // Note Selection Management
    // ==============================================
    fun setSelectedNotes(notes: Set<Note>) {
        _selectedNotes.value = notes
    }

    fun clearSelection() {
        _selectedNotes.value = emptySet()
    }

    // ==============================================
    // Note Operations
    // ==============================================
    fun deleteSelectedNotes(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            selectedNotes.value.forEach { note ->
                notesDataBridge.toggleNoteToTrash(note.id)
            }
        }
    }

    fun permanentlyDeleteSelectedNotes(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            selectedNotes.value.forEach { note ->
                noteLabelDataBridge.deleteNote(note.id)
            }
        }
    }

    fun archiveSelectedNotes(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            selectedNotes.value.forEach { note ->
                notesDataBridge.toggleNoteToArchive(note.id)
            }
        }
    }

    // ==============================================
    // Label Operations
    // ==============================================
    fun fetchLabels() {
        labelDataBridge.fetchLabels()
    }

    fun addNewLabel(labelName: String): String {
        val labelId = UUID.randomUUID().toString()
        return labelDataBridge.addNewLabel(labelId, labelName)
    }

    fun updateSelectedNotesLabels(
        checkedLabelIds: List<String>,
        uncheckedLabelIds: List<String>,
        newLabelId: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            for (note in selectedNotes.value) {
                // Start with the current labels of the note
                val currentLabels = note.labels.toMutableList()

                // Remove labels that were unchecked
                currentLabels.removeAll(uncheckedLabelIds)

                // Add labels that were checked (and weren't there before)
                val updatedLabels = (currentLabels + checkedLabelIds).distinct()

                // Add the newly created label if there is one
                val finalLabels = if (newLabelId.isNotEmpty()) {
                    (updatedLabels + newLabelId).distinct()
                } else {
                    updatedLabels
                }

                // Update the note with the final set of labels
                noteLabelDataBridge.updateNoteLabels(note.id, finalLabels)
                noteLabelDataBridge.updateLabelsWithNoteReference(note.id, finalLabels)
            }
        }
    }
}