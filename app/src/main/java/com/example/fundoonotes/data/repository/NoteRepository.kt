package com.example.fundoonotes.data.repository

import com.example.fundoonotes.data.SampleData
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.ui.noteEdit.NoteEditActivity

class NoteRepository(activity: NoteEditActivity) {

    private val sampleData = SampleData()
    private val allNotes = sampleData.getAllNotes().toMutableList()

    fun addNewNote(title: String, description: String) {
        val newNote = Note(
            id = (allNotes.size + 1).toString(),
            title = title,
            description = description
        )
        allNotes.add(newNote)
    }

    fun updateNote(id: String, title: String, description: String) {
        val note = allNotes.find { it.id == id }
        note?.let {
            it.title = title
            it.description = description
        }
    }

    fun getNoteById(id: String): Note? {
        return allNotes.find { it.id == id }
    }

    fun getAllNotes(): List<Note> {
        return allNotes
    }
}