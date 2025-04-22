package com.example.fundoonotes.ui.notes

import com.example.fundoonotes.data.model.Note

interface OnNoteClickListener {
    fun onNoteClick(note: Note)
    fun onSelectionModeStarted()
    fun onSelectionModeEnded()
    fun onSelectionChanged(selectedNotes: Set<Note>)
}