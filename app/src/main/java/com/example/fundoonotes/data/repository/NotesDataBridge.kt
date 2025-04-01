package com.example.fundoonotes.data.repository

import com.example.fundoonotes.data.model.Note

class NotesDataBridge: NotesRepository {


    override fun fetchNoteById(noteId: String, onSuccess: (Note) -> Unit){}

    override fun fetchNotes() {}

    override fun addNewNote(title: String, description: String): String {
        return ""
    }

    override fun updateNote(noteId: String, title: String, description: String) {}

    override fun deleteNote(noteId: String) {}

//    ------------------------------------------

    fun syncNotes() {}

}