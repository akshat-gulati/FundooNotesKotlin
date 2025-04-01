package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.fundoonotes.data.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * NotesDataBridge serves as a single source of truth for notes data
 */
class NotesDataBridge(private val context: Context) : NotesRepository {

    // Create a coroutine scope for this repository
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Shared state for all notes
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()

    // Repository instances for data sources
    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private val sqliteRepository: SQLiteNoteRepository = SQLiteNoteRepository(context)

    private var activeRepository: NotesRepository = firestoreRepository

    private fun observeFirestoreNotes() {
        coroutineScope.launch {
            firestoreRepository.notesState.collect { notes ->
                _notesState.value = notes
            }
        }
    }

    // Interface implementation
    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit) {
        activeRepository.fetchNoteById(noteId, onSuccess)
    }

    override fun fetchNotes() {
        activeRepository.fetchNotes()
    }

    override fun addNewNote(title: String, description: String): String {
        return activeRepository.addNewNote(title, description)
    }

    override fun updateNote(noteId: String, title: String, description: String) {
        activeRepository.updateNote(noteId, title, description)
    }

    override fun deleteNote(noteId: String) {
        activeRepository.deleteNote(noteId)
    }


    fun getNoteById(noteId: String): Note? {
        return _notesState.value.find { it.id == noteId }
    }

//    fun switchRepository(repositoryType: RepositoryType) {
//        activeRepository = when (repositoryType) {
//            RepositoryType.FIRESTORE -> firestoreRepository
//            RepositoryType.SQLITE -> {
//                Log.d("NotesDataBridge", "SQLite repository not yet implemented")
//            }
//        }
//
//        fetchNotes()
//    }

    fun syncRepositories() {
        Log.d("NotesDataBridge", "Repository sync not yet implemented")
    }
}
