package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.fundoonotes.core.NetworkManager
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import com.example.fundoonotes.data.repository.room.RoomNoteRepository
import com.example.fundoonotes.data.repository.firebase.FirestoreNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("SpellCheckingInspection")
class NotesDataBridge(private val context: Context) : NotesInterface {

    // ==============================================
    // Companion Object (Constants)
    // ==============================================
    companion object {
        private const val TAG = "NotesDataBridge"
    }

    // ==============================================
    // Properties and Initialization
    // ==============================================
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private val roomNoteRepository: RoomNoteRepository = RoomNoteRepository(context)
    private val networkManager = NetworkManager(context)
    val networkState: StateFlow<Boolean> = networkManager.networkState

    init {
        observeNotes()
    }

    // ==============================================
    // Data Observation
    // ==============================================
    private fun observeNotes() {
        // Observe network state changes
        coroutineScope.launch {
            networkState.collect { isOnline ->
                if (isOnline) {
                    _notesState.value = firestoreRepository.notesState.value
                } else {
                    _notesState.value = roomNoteRepository.notesState.value
                }
            }
        }

        // Collect from both repositories
        coroutineScope.launch {
            launch {
                roomNoteRepository.notesState.collect { roomNotes ->
                    if (!networkManager.isOnline()) {
                        _notesState.value = roomNotes
                        Log.d(TAG, "New Room notes received: ${roomNotes.size}")
                    }
                }
            }

            launch {
                firestoreRepository.notesState.collect { firestoreNotes ->
                    if (networkManager.isOnline()) {
                        _notesState.value = firestoreNotes
                        Log.d(TAG, "New Firestore notes received: ${firestoreNotes.size}")
                    }
                }
            }
        }
    }

    // ==============================================
    // Core CRUD Operations
    // ==============================================
    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        if (networkManager.isOnline()) {
            firestoreRepository.fetchNoteById(noteId, onSuccess)
        } else {
            roomNoteRepository.fetchNoteById(noteId, onSuccess)
        }
    }

    override fun fetchNotes() {
        if (networkManager.isOnline()) {
            firestoreRepository.fetchNotes()
        } else {
            roomNoteRepository.fetchNotes()
        }
    }

    override fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String {
        val localNoteId = roomNoteRepository.addNewNote(noteId, title, description, reminderTime)
        firestoreRepository.addNewNote(noteId, title, description, reminderTime)
        return localNoteId
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        roomNoteRepository.updateNote(noteId, title, description, reminderTime)
        firestoreRepository.updateNote(noteId, title, description, reminderTime)
    }

    override fun deleteNote(noteId: String) {
        roomNoteRepository.deleteNote(noteId)
        firestoreRepository.deleteNote(noteId)
    }

    // ==============================================
    // Note State Management
    // ==============================================
    fun toggleNoteToTrash(noteId: String) {
        fetchNoteById(noteId) { note ->
            val updatedFields = if (note.deleted == true) {
                mapOf(
                    "deleted" to false,
                    "deletedTime" to null
                )
            } else {
                mapOf(
                    "deleted" to true,
                    "deletedTime" to System.currentTimeMillis()
                )
            }
            roomNoteRepository.updateNoteFields(noteId, updatedFields)
            firestoreRepository.updateNoteFields(noteId, updatedFields)
        }
    }

    fun toggleNoteToArchive(noteId: String) {
        fetchNoteById(noteId) { note ->
            val updatedFields = if (note.archived == true) {
                mapOf(
                    "archived" to false,
                )
            } else {
                mapOf(
                    "archived" to true,
                )
            }
            roomNoteRepository.updateNoteFields(noteId, updatedFields)
            firestoreRepository.updateNoteFields(noteId, updatedFields)
        }
    }

    fun updateNoteLabels(noteId: String, labels: List<String>) {
        fetchNoteById(noteId) { note ->
            val updatedFields = mapOf("labels" to labels)
            roomNoteRepository.updateNoteFields(noteId, updatedFields)
            firestoreRepository.updateNoteFields(noteId, updatedFields)
        }
    }

    // ==============================================
    // Utility Methods
    // ==============================================
    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    fun cleanup() {
        if (networkManager.isOnline()) {
            firestoreRepository.cleanup()
        }
    }

    fun initializeDatabase() {
        fetchNotes()
    }
}