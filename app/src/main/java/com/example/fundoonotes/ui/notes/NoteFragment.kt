package com.example.fundoonotes.ui.notes

import android.app.AlertDialog
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
import com.example.fundoonotes.ui.noteEdit.NoteEditActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintSet.VISIBLE
import com.example.fundoonotes.ui.LayoutToggleListener

class NoteFragment : Fragment(), LayoutToggleListener, NoteAdapter.OnNoteClickListener {

    private val viewModel: NoteViewModel by lazy {
        NoteViewModel(requireContext())
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var fabAddNote: FloatingActionButton

    // Multi-selection action mode
    private var actionMode: ActionMode? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_note_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Update UI based on display mode
            if (viewModel.displayMode.value == DISPLAY_BIN) {
                menu.findItem(R.id.action_permanenlt_delete).isVisible = true
                menu.findItem(R.id.action_archive).isVisible = false
                menu.findItem(R.id.action_labels).isVisible = false

                val menuItem = menu.findItem(R.id.action_delete)
                menuItem.setIcon(R.drawable.restore)
            }

            if (viewModel.displayMode.value == DISPLAY_ARCHIVE) {
                val menuItem = menu.findItem(R.id.action_archive)
                menuItem.setIcon(R.drawable.unarchive)
            }

            mode.title = "${viewModel.selectedNotes.value.size} selected"
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {

                R.id.action_permanenlt_delete ->{
                    lifecycleScope.launch {
                        val job = viewModel.permanentlyDeleteSelectedNotes()
                        job.join()
                        mode.finish()
                    }
                    true
                }


                R.id.action_delete -> {
                    lifecycleScope.launch {
                        val job = viewModel.deleteSelectedNotes()
                        job.join()
                        mode.finish()
                    }
                    true
                }
                R.id.action_archive -> {
                    lifecycleScope.launch {
                        val job = viewModel.archiveSelectedNotes()
                        job.join()
                        mode.finish()
                    }
                    true
                }
                R.id.action_labels -> {
                    showLabelDialog(mode)
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
            viewModel.clearSelection()
            fabAddNote.visibility = View.VISIBLE

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
//        viewModel.fetchLabels()

        // Initialize ViewModel

        // Set initial display mode from arguments
        arguments?.let {
            val initialDisplayMode = it.getInt("initial_display_mode", DISPLAY_NOTES)
            viewModel.updateDisplayMode(initialDisplayMode)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        fabAddNote = view.findViewById(R.id.fab_add_note)

        // Initialize adapter
        noteAdapter = NoteAdapter(emptyList(), this)
        recyclerView.adapter = noteAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()


        // Set up observers
        setupObservers()

        // Set up FAB click listener
        fabAddNote.setOnClickListener {
            val intent = Intent(activity, NoteEditActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe filtered notes
                launch {
                    viewModel.filteredNotes.collect { notes ->
                        noteAdapter.updateNotes(notes)
                    }
                }

                // Observe layout mode
                launch {
                    viewModel.isGridLayout.collect { isGrid ->
                        setupLayoutManager(isGrid)
                    }
                }

                // Observe display mode for FAB visibility
                launch {
                    viewModel.displayMode.collect { mode ->
                        updateFabVisibility(mode)
                    }
                }

                // Observe selection changes
                launch {
                    viewModel.selectedNotes.collect { selectedNotes ->
                        if (actionMode != null) {
                            actionMode?.title = "${selectedNotes.size} selected"
                        }
                    }
                }
            }
        }
    }

    private fun setupLayoutManager(isGridLayout: Boolean) {
        val spanCount = if (isGridLayout) 2 else 1
        recyclerView.layoutManager = StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun updateFabVisibility(displayMode: Int) {
        fabAddNote.visibility = if (displayMode == DISPLAY_BIN) View.GONE else View.VISIBLE
    }

    override fun onLayoutToggle(isGridLayout: Boolean) {
        viewModel.toggleLayoutMode(isGridLayout)
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
    }

    override fun onSelectionChanged(selectedNotes: Set<Note>) {
        viewModel.setSelectedNotes(selectedNotes)
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
        viewModel.setSearchQuery(query)
    }

    // New method to update display mode
    fun updateDisplayMode(newMode: Int, labelId: String? = null) {
        viewModel.updateDisplayMode(newMode, labelId)
    }

    fun showLabelDialog(mode: ActionMode) {
        if (context == null) return

        // Create the dialog builder
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Manage Labels")

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 16, 24, 0)

        // Add loading indicator initially
        val loadingText = EditText(context)
        loadingText.isEnabled = false
        loadingText.setText("Loading labels...")
        layout.addView(loadingText)

        scrollView.addView(layout)
        dialogBuilder.setView(scrollView)

        // Create text input for new label
        val editText = EditText(context)
        editText.hint = "Create a New Label"

        // Set up the buttons
        dialogBuilder.setPositiveButton("OK") { _, _ ->
            // This will be implemented after we show the dialog
        }
        dialogBuilder.setNegativeButton("Cancel") { _, _ -> }

        // Create and show the dialog
        val dialog = dialogBuilder.create()
        dialog.show()

        // Store checkboxes mapping
        val checkBoxes = mutableMapOf<CheckBox, String>()
        val initialLabelStates = mutableMapOf<String, Boolean>()

        // Fetch labels first, then update the dialog
        viewModel.fetchLabels()

        // Now observe the labels and update the dialog when they're available
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.labelDataBridge.labelsState.collect { labels ->
                // Only update if dialog is showing
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
                        // Process and display labels
                        labels.forEach { label ->
                            // Check if any selected note has this label
                            val anyNoteHasLabel = viewModel.selectedNotes.value.any { it.labels.contains(label.id) }
                            initialLabelStates[label.id] = anyNoteHasLabel

                            val checkBox = CheckBox(context)
                            checkBox.text = label.name
                            checkBox.isChecked = anyNoteHasLabel
                            checkBoxes[checkBox] = label.id
                            layout.addView(checkBox)
                        }
                    }

                    // Add padding and the edit text
                    val paddingView = View(context)
                    paddingView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        24
                    )
                    layout.addView(paddingView)
                    layout.addView(editText)

                    // Now that we have labels, override the OK button action
                    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        // Track which labels are checked and which are unchecked
                        val checkedLabelIds = checkBoxes.filter { it.key.isChecked }.map { it.value }
                        val uncheckedLabelIds = checkBoxes.filter { !it.key.isChecked }.map { it.value }

                        // Handle new label creation if text is entered
                        val newLabelName = editText.text.toString().trim()
                        var newLabelId = ""

                        if (newLabelName.isNotEmpty()) {
                            // Create new label
                            newLabelId = viewModel.addNewLabel(newLabelName)
                        }

                        // Apply changes to all selected notes
                        viewModel.updateSelectedNotesLabels(checkedLabelIds, uncheckedLabelIds, newLabelId)
                        dialog.dismiss()
                        mode.finish()
                    }
                }
            }
        }
    }
}