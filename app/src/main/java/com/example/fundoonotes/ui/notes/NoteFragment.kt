package com.example.fundoonotes.ui.notes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.ui.noteEdit.NoteEditActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle

class NoteFragment : Fragment(), MainActivity.LayoutToggleListener, NoteAdapter.OnNoteClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var notesDataBridge: NotesDataBridge

    private var isGridLayout = true
    private var displayMode = DISPLAY_NOTES // Default mode
    private var currentLabel: String? = null // For label filtering

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

        // Initialize repository
        notesDataBridge = NotesDataBridge(requireContext())
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

        // Initialize adapter
        noteAdapter = NoteAdapter(emptyList(), this)
        recyclerView.adapter = noteAdapter

        // Setup flow collection for notes - this is the key change
        setupNotesObserver()

        // Set up FAB click listener
        fabAddNote.setOnClickListener {
            val intent = Intent(activity, NoteEditActivity::class.java)
            startActivity(intent)
        }

        // Adjust FAB visibility based on display mode
        if (displayMode == DISPLAY_BIN) {
            fabAddNote.visibility = View.GONE
        } else {
            fabAddNote.visibility = View.VISIBLE
        }

        return view
    }

    private fun setupNotesObserver() {
        // This ensures proper collection lifecycle management
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notesDataBridge.notesState.collect { notes ->
                    val filteredNotes = getFilteredNotes(notes)
                    noteAdapter.updateNotes(filteredNotes)
                }
            }
        }
    }

    private fun setupLayoutManager() {
        val spanCount = if (isGridLayout) 2 else 1
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun getFilteredNotes(notes: List<Note>): List<Note> {
        return when (displayMode) {
            DISPLAY_NOTES -> notes.filter { !it.archived && !it.deleted }
            DISPLAY_REMINDERS -> notes.filter { !it.archived && !it.deleted && it.reminderTime != null }
            DISPLAY_ARCHIVE -> notes.filter { it.archived && !it.deleted }
            DISPLAY_BIN -> notes.filter { it.deleted }
            DISPLAY_LABELS -> {
                currentLabel?.let { label ->
                    notes.filter { !it.archived && !it.deleted && it.labels.contains(label) }
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    override fun onLayoutToggle(isGridLayout: Boolean) {
        this.isGridLayout = isGridLayout
        setupLayoutManager()
    }

    // NoteAdapter.OnNoteClickListener implementation
    override fun onNoteClick(note: Note) {
        val intent = Intent(activity, NoteEditActivity::class.java)
        intent.putExtra("NOTE_ID", note.id)
        startActivity(intent)
    }

    override fun onNoteLongClick(note: Note, position: Int): Boolean {
        // Implement long click actions if needed
        return true
    }

    // Remove the onResume method collection as it's redundant and causing issues
    // The setupNotesObserver() method now handles this properly
}