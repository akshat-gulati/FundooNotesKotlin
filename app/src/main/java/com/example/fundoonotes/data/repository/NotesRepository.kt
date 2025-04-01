package com.example.fundoonotes.data.repository

import com.example.fundoonotes.data.model.Note

interface NotesRepository {

    fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit)
    fun fetchNotes()
    fun addNewNote(title: String, description: String): String
    fun updateNote(noteId: String, title: String, description: String)
    fun deleteNote(noteId: String) {}


}