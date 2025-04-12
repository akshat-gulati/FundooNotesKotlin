package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
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
import java.util.UUID

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
        observeNotes()
    }


    private fun observeNotes() {
        // Observe network state changes
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Set up network callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // When network becomes available, switch to Firestore data and sync
                coroutineScope.launch {
                    _notesState.value = firestoreRepository.notesState.value
                    syncSQLiteToFirestore() // Sync any changes made while offline
                }
            }

            override fun onLost(network: Network) {
                // When network is lost, switch to SQLite data
                coroutineScope.launch {
                    _notesState.value = sqliteNoteRepository.notesState.value
                }
            }
        }

        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Start collecting from both repositories
        coroutineScope.launch {
            // Always collect from SQLite for local changes
            launch {
                sqliteNoteRepository.notesState.collect { sqliteNotes ->
                    if (!isOnline(context)) {
                        _notesState.value = sqliteNotes
                        Log.d(TAG, "New SQLite notes received: ${sqliteNotes.size}")
                    }
                }
            }

            // Always collect from Firestore for remote changes
            launch {
                firestoreRepository.notesState.collect { firestoreNotes ->
                    if (isOnline(context)) {
                        _notesState.value = firestoreNotes
                        Log.d(TAG, "New Firestore notes received: ${firestoreNotes.size}")

                        // Update SQLite with the latest Firestore data to keep local copy updated
                        updateSQLiteFromFirestore(firestoreNotes)
                    }
                }
            }
        }
    }

    // Helper method to sync SQLite changes to Firestore when back online
    private fun syncSQLiteToFirestore() {
        // Implementation would compare timestamps or use a "dirty" flag
        // to sync any changes made while offline
        Log.d(TAG, "Syncing SQLite changes to Firestore")
        // Implementation details would go here
    }


    // Helper method to update SQLite with latest Firestore data
    private fun updateSQLiteFromFirestore(firestoreNotes: List<Note>) {
        // For each Firestore note, update or insert into SQLite
        for (note in firestoreNotes) {
            // You might want to check timestamps to avoid overwriting newer local changes
            val sqliteNote = sqliteNoteRepository.getNoteById(note.id)
            if (sqliteNote == null || sqliteNote.timestamp < note.timestamp) {
                // Update SQLite with this note
                val fields = mapOf(
                    "title" to note.title,
                    "description" to note.description,
                    "labels" to note.labels,
                    "deleted" to note.deleted,
                    "archived" to note.archived,
                    "reminderTime" to note.reminderTime,
                    "deletedTime" to note.deletedTime,
                    "timestamp" to note.timestamp
                )
                sqliteNoteRepository.updateNoteFields(note.id, fields)
            }
        }
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        if (isOnline(context)) {
            firestoreRepository.fetchNoteById(noteId, onSuccess)
        }
        else{sqliteNoteRepository.fetchNoteById(noteId, onSuccess)}
    }

    override fun fetchNotes() {
        if (isOnline(context)){
            firestoreRepository.fetchNotes()
        }
        else{
            sqliteNoteRepository.fetchNotes()
        }

    }

    // No need of this function
    override fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String {

        // Always save to SQLite regardless of online status
        val sqliteSuccess = sqliteNoteRepository.addNewNote(noteId, title, description, reminderTime)

        if (sqliteSuccess.isNotEmpty()) {
            Toast.makeText(context, "Note saved locally", Toast.LENGTH_SHORT).show()
        }

        // If online, also save to Firestore
        firestoreRepository.addNewNote(noteId, title, description, reminderTime)

        return noteId
    }

    // Update other methods similarly to add toast notifications
    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        // Always update SQLite
        sqliteNoteRepository.updateNote(noteId, title, description, reminderTime)
        Toast.makeText(context, "Note updated locally", Toast.LENGTH_SHORT).show()
            firestoreRepository.updateNote(noteId, title, description, reminderTime)
    }

    override fun deleteNote(noteId: String) {
        firestoreRepository.deleteNote(noteId)
        sqliteNoteRepository.deleteNote(noteId)
    }

    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    // Method to update a note's labels
    fun updateNoteLabels(noteId: String, labels: List<String>) {
        // First, get the current note to preserve its other properties
        fetchNoteById(noteId) { note ->
            val updatedFields = mapOf("labels" to labels)
            firestoreRepository.updateNoteFields(noteId, updatedFields)
            sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
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
                sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
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
              sqliteNoteRepository.updateNoteFields(noteId, updatedFields)
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
    fun initializeDatabase() {
        // Force the SQLiteOpenHelper to create/open the database
        val db = sqliteNoteRepository.writableDatabase
        db.close()

        // Fetch initial data
        fetchNotes()
    }



}