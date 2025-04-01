package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("SpellCheckingInspection")
class NotesDataBridge(private val context: Context) : NotesRepository {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _notesState = MutableStateFlow<List<Note>>(emptyList())
    val notesState: StateFlow<List<Note>> = _notesState.asStateFlow()
    private val firestoreRepository: FirestoreNoteRepository = FirestoreNoteRepository(context)
    private var activeRepository: NotesRepository = firestoreRepository

    init {
        observeFirestoreNotes()
        fetchNotes()
    }

    private fun observeFirestoreNotes() {
        coroutineScope.launch {
            firestoreRepository.notesState.collect { notes ->
                _notesState.value = notes
            }
        }
    }

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

    fun switchRepository(repositoryType: RepositoryType) {
        activeRepository = when (repositoryType) {
            RepositoryType.FIRESTORE -> firestoreRepository
            RepositoryType.SQLITE -> {
                Log.d("NotesDataBridge", "SQLite repository not yet implemented")
                firestoreRepository
            }
        }
        fetchNotes()
    }

    fun syncRepositories() {
        Log.d("NotesDataBridge", "Repository sync not yet implemented")
    }

    enum class RepositoryType {
        FIRESTORE,
        SQLITE
    }
}
