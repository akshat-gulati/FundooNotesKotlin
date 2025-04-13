package com.example.fundoonotes.ui.notes

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.ui.noteEdit.NoteEditActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.fundoonotes.data.repository.NoteLabelRepository
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteFragment : Fragment(), MainActivity.LayoutToggleListener, NoteAdapter.OnNoteClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var notesDataBridge: NotesDataBridge
    private lateinit var labelDataBridge: LabelDataBridge
    private lateinit var noteLabelRepository: NoteLabelRepository

    private var isGridLayout = true
    private var displayMode = DISPLAY_NOTES // Default mode
    private var currentLabelId: String? = null // For label filtering
    private var searchQuery: String = "" // For search filtering

    // Multi-selection action mode
    private var actionMode: ActionMode? = null
    private var selectedNotes: Set<Note> = emptySet()

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_note_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Update action mode title with count of selected items
            if (displayMode == DISPLAY_BIN) {
                menu.findItem(R.id.action_archive).isVisible = false
                menu.findItem(R.id.action_labels).isVisible = false

                val menuItem = menu.findItem(R.id.action_delete)
                menuItem.setIcon(R.drawable.restore)
            }

            if (displayMode == DISPLAY_ARCHIVE){
                val menuItem = menu.findItem(R.id.action_archive)
                menuItem.setIcon(R.drawable.unarchive)
            }

            mode.title = "${selectedNotes.size} selected"
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    val noteIds = selectedNotes.map { it.id }
                    noteIds.forEach { noteId ->
                        notesDataBridge.toggleNoteToTrash(noteId)
                    }
                    mode.finish()
                    true
                }
                R.id.action_archive -> {
                    val noteIds = selectedNotes.map { it.id }
                    noteIds.forEach { noteId ->
                        notesDataBridge.toggleNoteToArchive(noteId)
                    }
                    mode.finish()
                    true
                }
                R.id.action_labels -> {
                    showLabelDialog()
                    mode.finish()
                    true
                }
                R.id.action_select_all -> {
                    noteAdapter.selectAll()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            noteAdapter.exitSelectionMode()

            // Ensure toolbar is visible when action mode is destroyed
            (activity as MainActivity).setToolbarVisibility(true)
        }
    }

    companion object {
        // Display mode constants
        const val DISPLAY_NOTES = 0
        const val DISPLAY_REMINDERS = 1
        const val DISPLAY_ARCHIVE = 2
        const val DISPLAY_BIN = 3
        const val DISPLAY_LABELS = 4

        @JvmStatic
        fun newInstance(displayMode: Int) = NoteFragment().apply {
            arguments = Bundle().apply {
                putInt("initial_display_mode", displayMode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            displayMode = it.getInt("initial_display_mode", DISPLAY_NOTES)
        }

        // Initialize repository
        notesDataBridge = NotesDataBridge(requireContext())
        noteLabelRepository = NoteLabelRepository(requireContext())
        labelDataBridge = LabelDataBridge(requireContext())
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

        // Initial FAB visibility based on display mode
        updateFabVisibility()

        return view
    }

    private fun updateFabVisibility() {
        fabAddNote.visibility = if (displayMode == DISPLAY_BIN) View.GONE else View.VISIBLE
    }

    private fun setupNotesObserver() {
        // Ensure the coroutine is tied to the view's lifecycle rather than the Fragment's lifecycle
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notesDataBridge.notesState.collect { notes ->
                    withContext(Dispatchers.Main) {
                        val filteredNotes = getFilteredNotes(notes)
                        noteAdapter.updateNotes(filteredNotes)

                        // If we're in selection mode, update the selection count
                        if (actionMode != null) {
                            actionMode?.title = "${selectedNotes.size} selected"
                        }
                    }
                }
            }
        }
    }

    private fun setupLayoutManager() {
        val spanCount = if (isGridLayout) 2 else 1
        recyclerView.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun getFilteredNotes(notes: List<Note>): List<Note> {
        // First apply display mode filter
        val displayModeFiltered = when (displayMode) {
            DISPLAY_NOTES -> notes.filter { !it.archived && !it.deleted }
            DISPLAY_REMINDERS -> notes.filter { !it.archived && !it.deleted && it.reminderTime != null }
            DISPLAY_ARCHIVE -> notes.filter { it.archived && !it.deleted }
            DISPLAY_BIN -> notes.filter { it.deleted }
            DISPLAY_LABELS -> {
                currentLabelId?.let { labelId ->
                    notes.filter { !it.archived && !it.deleted && it.labels.contains(labelId) }
                } ?: emptyList()
            }
            else -> emptyList()
        }

        // Then apply search filter if any
        return if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase().trim()
            displayModeFiltered.filter { note ->
                note.title.lowercase().contains(query) ||
                        note.description.lowercase().contains(query)
            }
        } else {
            displayModeFiltered
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

    // New callbacks for multi-selection
    override fun onSelectionModeStarted() {
        // Start action mode when selection begins
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }

        // Hide FAB during selection mode
        fabAddNote.visibility = View.GONE

        // Hide the toolbar
        (activity as MainActivity).setToolbarVisibility(false)
    }

    override fun onSelectionModeEnded() {
        // End action mode when selection ends
        actionMode?.finish()

        // Show FAB if we're not in trash
        updateFabVisibility()

        // Show the toolbar again
        (activity as MainActivity).setToolbarVisibility(true)
    }

    override fun onSelectionChanged(selectedNotes: Set<Note>) {
        this.selectedNotes = selectedNotes
        actionMode?.invalidate() // Update action mode UI
    }

    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()

        // Ensure toolbar visibility is restored
        if (activity != null) {
            (activity as MainActivity).setToolbarVisibility(true)
        }
    }

    // Public method to filter notes by search query
    fun filterNotes(query: String) {
        this.searchQuery = query
        refreshNotes()
    }

    // New method to update display mode
    fun updateDisplayMode(newMode: Int, labelId: String? = null) {
        displayMode = newMode
        currentLabelId = labelId
        searchQuery = "" // Reset search when changing modes
        updateFabVisibility()
        refreshNotes()
    }

    // Helper method to refresh notes based on current filters
    private fun refreshNotes() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val currentNotes = notesDataBridge.notesState.value
            val filteredNotes = getFilteredNotes(currentNotes)

            withContext(Dispatchers.Main) {
                noteAdapter.updateNotes(filteredNotes)
            }
        }
    }

    fun showLabelDialog() {
        if (context == null) return

        // First, ensure we fetch the latest labels
        labelDataBridge.fetchLabels()

        // Create the dialog builder
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Manage Labels")

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 16, 24, 0)

        // We'll set up the checkboxes after we get the labels
        val checkBoxes = mutableMapOf<CheckBox, String>() // CheckBox to labelId mapping

        // Create text input for new label
        val editText = EditText(context)
        editText.hint = "Create a New Label"

        // Create and show the dialog with a loading state initially
        val loadingText = EditText(context)
        loadingText.isEnabled = false
        loadingText.setText("Loading labels...")
        layout.addView(loadingText)

        scrollView.addView(layout)
        dialogBuilder.setView(scrollView)

        val dialog = dialogBuilder.create()

        // Set up the buttons
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            // Track which labels are checked and which are unchecked
            val checkedLabelIds = checkBoxes.filter { it.key.isChecked }.map { it.value }
            val uncheckedLabelIds = checkBoxes.filter { !it.key.isChecked }.map { it.value }

            // Handle new label creation if text is entered
            val newLabelName = editText.text.toString().trim()
            var newLabelId = ""

            if (newLabelName.isNotEmpty()) {
                // Create new label
                newLabelId = labelDataBridge.addNewLabel(newLabelName)
                // Add the new label ID to the checked labels
                if (newLabelId.isNotEmpty()) {
                    checkedLabelIds.plus(newLabelId)
                }
            }

            // Apply changes to all selected notes on IO thread
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                // Apply changes to all selected notes
                for (note in selectedNotes) {
                    // Start with the current labels of the note
                    val currentLabels = note.labels.toMutableList()

                    // Remove labels that were unchecked
                    currentLabels.removeAll(uncheckedLabelIds)

                    // Add labels that were checked (and weren't there before)
                    val updatedLabels = (currentLabels + checkedLabelIds).distinct()

                    // Add the newly created label if there is one
                    val finalLabels = if (newLabelId.isNotEmpty()) {
                        (updatedLabels + newLabelId).distinct()
                    } else {
                        updatedLabels
                    }

                    // Update the note with the final set of labels
                    noteLabelRepository.updateNoteLabels(note.id, finalLabels)
                    noteLabelRepository.updateLabelsWithNoteReference(note.id, finalLabels)
                }
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ ->
            // No action needed
        }

        // Show the dialog
        dialog.show()

        // Store initial label states for all selected notes
        val initialLabelStates = mutableMapOf<String, Boolean>()

        // Now observe the labels and update the dialog
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            labelDataBridge.labelsState.collect { labels ->
                // Only update if we have the dialog showing
                withContext(Dispatchers.Main) {
                    if (dialog.isShowing) {
                        // Clear the current views
                        layout.removeAllViews()
                        checkBoxes.clear()

                        if (labels.isEmpty()) {
                            val noLabelsText = EditText(context)
                            noLabelsText.isEnabled = false
                            noLabelsText.setText("No existing labels. Create one below.")
                            layout.addView(noLabelsText)
                        } else {
                            // Initialize the label states map for all labels
                            labels.forEach { label ->
                                // A label is considered "on" if ANY selected note has it
                                val anyNoteHasLabel = selectedNotes.any { it.labels.contains(label.id) }
                                initialLabelStates[label.id] = anyNoteHasLabel
                            }

                            // Create checkboxes for existing labels
                            labels.forEach { label ->
                                val checkBox = CheckBox(context)
                                checkBox.text = label.name

                                // Set the initial checked state based on our map
                                checkBox.isChecked = initialLabelStates[label.id] ?: false

                                checkBoxes[checkBox] = label.id
                                layout.addView(checkBox)
                            }
                        }

                        // Add some padding before the new label section
                        val paddingView = View(context)
                        paddingView.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            24
                        )
                        layout.addView(paddingView)

                        // Add the edit text for creating new labels
                        layout.addView(editText)
                    }
                }
            }
        }
    }
}