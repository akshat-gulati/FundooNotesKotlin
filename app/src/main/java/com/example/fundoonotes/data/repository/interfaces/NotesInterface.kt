package com.example.fundoonotes.data.repository.interfaces

import com.example.fundoonotes.data.model.Note
import java.util.UUID

interface NotesInterface {

    fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit)
    fun fetchNotes()
    fun addNewNote(noteId: String, title: String, description: String, reminderTime: Long?): String
    fun updateNote(noteId: String, title: String, description: String, reminderTime: Long?)
    fun deleteNote(noteId: String) {}

}