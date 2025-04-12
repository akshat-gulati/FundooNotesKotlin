package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.interfaces.NotesInterface
import com.example.fundoonotes.data.repository.sqlite.SQLiteNoteRepository
import com.example.fundoonotes.data.repository.firebase.FirestoreNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("SpellCheckingInspection")
class NotesDataBridge(private val context: Context) : NotesInterface {

    companion object{
        private const val TAG = "NotesDataBridge"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()
    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private val sqliteNoteRepository: SQLiteNoteRepository = SQLiteNoteRepository(context)


    init {
        observeFirestoreNotes()
    }

    private fun observeFirestoreNotes() {
        coroutineScope.launch {
            firestoreRepository.notesState.collect { notes ->
                _notesState.value = notes
                Log.d(TAG, "New notes received: ${notes.size}")
            }
        }
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        firestoreRepository.fetchNoteById(noteId, onSuccess)
        sqliteNoteRepository.fetchNoteById(noteId, onSuccess)
    }

    override fun fetchNotes() {
            firestoreRepository.fetchNotes()
    }

    override fun addNewNote(title: String, description: String, reminderTime: Long?): String {
        return firestoreRepository.addNewNote(title, description, reminderTime)
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        firestoreRepository.updateNote(noteId, title, description, reminderTime)
//        sqliteNoteRepository.updateNote(noteId, title, description, reminderTime)
    }

    override fun deleteNote(noteId: String) {
        firestoreRepository.deleteNote(noteId)
//        sqliteNoteRepository.deleteNote(noteId)
    }

    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    // Method to update a note's labels
    fun updateNoteLabels(noteId: String, labels: List<String>) {
        // First, get the current note to preserve its other properties
        fetchNoteById(noteId) { note ->
            // Extract the relevant properties we want to keep
            val updatedFields = mapOf("labels" to labels)
                firestoreRepository.updateNoteFields(noteId, updatedFields)
//                sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
        }
    }


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


                firestoreRepository.updateNoteFields(noteId, updatedFields)
//                sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
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

                firestoreRepository.updateNoteFields(noteId, updatedFields)
//              sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
        }
    }

    fun syncRepositories() {
        Log.d(TAG, "Repository sync not yet implemented")
    }

    // Method to clean up resources when no longer needed
    fun cleanup() {
            firestoreRepository.cleanup()
    }

    // This code is taken from Satck Overflow, needs to be looked at once
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        capabilities?.let {
            when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }



}