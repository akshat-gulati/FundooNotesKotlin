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

    companion object{
        private const val TAG = "NotesDataBridge"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()
    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private val roomNoteRepository: RoomNoteRepository = RoomNoteRepository(context)

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
                    syncRoomToFirestore() // Sync any changes made while offline
                }
            }

            override fun onLost(network: Network) {
                // When network is lost, switch to Room data
                coroutineScope.launch {
                    _notesState.value = roomNoteRepository.notesState.value
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
            // Always collect from Room for local changes
            launch {
                roomNoteRepository.notesState.collect { roomNotes ->
                    if (!isOnline(context)) {
                        _notesState.value = roomNotes
                        Log.d(TAG, "New Room notes received: ${roomNotes.size}")
                    }
                }
            }

            // Always collect from Firestore for remote changes
            launch {
                firestoreRepository.notesState.collect { firestoreNotes ->
                    if (isOnline(context)) {
                        _notesState.value = firestoreNotes
                        Log.d(TAG, "New Firestore notes received: ${firestoreNotes.size}")

                        // Update Room with the latest Firestore data to keep local copy updated
                        updateRoomFromFirestore(firestoreNotes)
                    }
                }
            }
        }
    }

    // Helper method to sync Room changes to Firestore when back online
    private fun syncRoomToFirestore() {
        // Implementation would compare timestamps or use a "dirty" flag
        // to sync any changes made while offline
        Log.d(TAG, "Syncing Room changes to Firestore")
        // Implementation details would go here
    }

    // Helper method to update Room with latest Firestore data
    private fun updateRoomFromFirestore(firestoreNotes: List<Note>) {
        // For each Firestore note, update or insert into Room
        for (note in firestoreNotes) {
            // You might want to check timestamps to avoid overwriting newer local changes
            val roomNote = roomNoteRepository.getNoteById(note.id)
            if (roomNote == null || roomNote.timestamp < note.timestamp) {
                // Update Room with this note
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
                roomNoteRepository.updateNoteFields(note.id, fields)
            }
        }
    }

    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        if (isOnline(context)) {
            firestoreRepository.fetchNoteById(noteId, onSuccess)
        }
        else {
            roomNoteRepository.fetchNoteById(noteId, onSuccess)
        }
    }

    override fun fetchNotes() {
        if (isOnline(context)){
            firestoreRepository.fetchNotes()
        }
        else{
            roomNoteRepository.fetchNotes()
        }
    }

    override fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String {
        // Always save to Room regardless of online status
        val localNoteId = roomNoteRepository.addNewNote(noteId, title, description, reminderTime)

        // If online, also save to Firestore
//        if (isOnline(context)) {
            firestoreRepository.addNewNote(noteId, title, description, reminderTime)
//        }

        return localNoteId
    }

    override fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?) {
        // Always update Room
        roomNoteRepository.updateNote(noteId, title, description, reminderTime)
        Toast.makeText(context, "Note updated locally", Toast.LENGTH_SHORT).show()

        // If online, also update Firestore
//        if (isOnline(context)) {
            firestoreRepository.updateNote(noteId, title, description, reminderTime)
//        }
    }

    override fun deleteNote(noteId: String) {
        roomNoteRepository.deleteNote(noteId)

//        if (isOnline(context)) {
            firestoreRepository.deleteNote(noteId)
//        }
    }

    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

    // Method to update a note's labels
    fun updateNoteLabels(noteId: String, labels: List<String>) {
        // First, get the current note to preserve its other properties
        fetchNoteById(noteId) { note ->
            val updatedFields = mapOf("labels" to labels)

            // Update in Room
            roomNoteRepository.updateNoteFields(noteId, updatedFields)

            // If online, update in Firestore
//            if (isOnline(context)) {
                firestoreRepository.updateNoteFields(noteId, updatedFields)
//            }
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

            // Update in Room
            roomNoteRepository.updateNoteFields(noteId, updatedFields)

            // If online, update in Firestore
//            if (isOnline(context)) {
                firestoreRepository.updateNoteFields(noteId, updatedFields)
//            }
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

            // Update in Room
            roomNoteRepository.updateNoteFields(noteId, updatedFields)

            // If online, update in Firestore
//            if (isOnline(context)) {
                firestoreRepository.updateNoteFields(noteId, updatedFields)
//            }
        }
    }

    fun syncRepositories() {
        Log.d(TAG, "Repository sync not yet implemented")
    }

    // Method to clean up resources when no longer needed
    fun cleanup() {
        if (isOnline(context)) {
            firestoreRepository.cleanup()
        }
    }

    // This code is taken from Stack Overflow, needs to be looked at once
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
        // The Room database is created automatically when accessed
        // Just fetch initial data
        fetchNotes()
    }
}