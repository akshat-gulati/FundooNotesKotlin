package com.example.fundoonotes.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.adapters.NoteAdapter
import com.example.fundoonotes.data.model.Note
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteFragment : Fragment(), MainActivity.LayoutToggleListener, NoteAdapter.OnNoteClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var fabAddNote: FloatingActionButton

    private var isGridLayout = true
    private var displayMode = DISPLAY_NOTES // Default mode
    private var currentLabel: String? = null // For label filtering

    // Sample data - in a real app this would come from a database
    private val allNotes = mutableListOf(
        Note(id = "1", title = "Meeting", description = "Discuss project timeline with the team at 10 AM.",
            labels = listOf("Work")),
        Note(id = "2", title = "Grocery List", description = "Milk, Eggs, Bread, Butter, Coffee.",
            labels = listOf("Home")),
        Note(id = "3", title = "Workout Plan", description = "Morning: Cardio. Evening: Strength training.",
            labels = listOf("Health")),
        Note(id = "4", title = "Book to Read", description = "Start 'Atomic Habits' by James Clear.",
            labels = listOf("Personal")),
        Note(id = "5", title = "Weekend Plans", description = "Visit the beach and try out new café.",
            reminderTime = System.currentTimeMillis() + 86400000), // Tomorrow
        Note(id = "6", title = "Coding Task", description = "Implement RecyclerView adapter in FundooNotes.",
            labels = listOf("Work")),
        Note(id = "7", title = "Doctor's Appointment", description = "Check-up scheduled for 3:00 PM.",
            reminderTime = System.currentTimeMillis() + 172800000), // Day after tomorrow
        Note(id = "8", title = "Birthday Reminder", description = "John's birthday on March 25th, buy a gift.",
            reminderTime = System.currentTimeMillis() + 259200000), // Three days from now
        Note(id = "9", title = "Home Maintenance", description = "Fix the leaking tap in the kitchen.",
            isArchived = true, labels = listOf("Home")),
        Note(id = "10", title = "Travel Checklist", description = "Pack essentials for the upcoming trip.",
            isDeleted = true)
    )

    companion object {
        // Display mode constants
        const val DISPLAY_NOTES = 0
        const val DISPLAY_REMINDERS = 1
        const val DISPLAY_ARCHIVE = 2
        const val DISPLAY_BIN = 3
        const val DISPLAY_LABELS = 4

        const val ARG_DISPLAY_MODE = "display_mode"
        const val ARG_LABEL = "label"

        @JvmStatic
        fun newInstance(displayMode: Int, label: String? = null) =
            NoteFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DISPLAY_MODE, displayMode)
                    label?.let { putString(ARG_LABEL, it) }
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            displayMode = it.getInt(ARG_DISPLAY_MODE, DISPLAY_NOTES)
            currentLabel = it.getString(ARG_LABEL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        fabAddNote = view.findViewById(R.id.fab_add_note)

        // Set up the RecyclerView layout manager
        setupLayoutManager()

        // Initialize adapter with filtered notes based on display mode
        noteAdapter = NoteAdapter(getFilteredNotes(), this)
        recyclerView.adapter = noteAdapter

        // Set up FAB click listener
        fabAddNote.setOnClickListener {
            // In a real app, this would open a new activity or dialog to create a note
            Toast.makeText(context, "Add new note", Toast.LENGTH_SHORT).show()
        }

        // Adjust FAB visibility based on display mode
        if (displayMode == DISPLAY_BIN) {
            fabAddNote.visibility = View.GONE
        } else {
            fabAddNote.visibility = View.VISIBLE
        }

        return view
    }

    private fun setupLayoutManager() {
        val spanCount = if (isGridLayout) 2 else 1
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun getFilteredNotes(): List<Note> {
        return when (displayMode) {
            DISPLAY_NOTES -> allNotes.filter { !it.isArchived && !it.isDeleted }
            DISPLAY_REMINDERS -> allNotes.filter { !it.isArchived && !it.isDeleted && it.reminderTime != null }
            DISPLAY_ARCHIVE -> allNotes.filter { it.isArchived && !it.isDeleted }
            DISPLAY_BIN -> allNotes.filter { it.isDeleted }
            DISPLAY_LABELS -> {
                currentLabel?.let { label ->
                    allNotes.filter { !it.isArchived && !it.isDeleted && it.labels.contains(label) }
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    // Refresh the notes list when any changes occur
    fun refreshNotes() {
        noteAdapter.updateNotes(getFilteredNotes())
    }

    override fun onLayoutToggle(isGridLayout: Boolean) {
        this.isGridLayout = isGridLayout
        setupLayoutManager()
    }

    // NoteAdapter.OnNoteClickListener implementation
    override fun onNoteClick(note: Note) {
        // In a real app, this would open a note editor
        Toast.makeText(context, "Clicked on: ${note.title}", Toast.LENGTH_SHORT).show()
    }

    override fun onNoteLongClick(note: Note, position: Int): Boolean {
        // In a real app, this would show a context menu for options like delete, archive, etc.
        Toast.makeText(context, "Long press on: ${note.title}", Toast.LENGTH_SHORT).show()
        return true
    }
}