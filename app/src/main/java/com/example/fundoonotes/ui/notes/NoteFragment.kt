package com.example.fundoonotes.ui.notes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.fundoonotes.*
import com.example.fundoonotes.ui.noteEdit.NoteEditActivity
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.data.model.Note
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteFragment : Fragment(), MainActivity.LayoutToggleListener, OnNoteClickListener {

    // ==============================================
    // Constants
    // ==============================================
    companion object {
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

    // ==============================================
    // Properties
    // ==============================================
    private val viewModel: NoteViewModel by lazy { NoteViewModel(requireContext()) }
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var fabAddNote: FloatingActionButton
    private var actionMode: ActionMode? = null

    // ==============================================
    // Lifecycle Methods
    // ==============================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val initialDisplayMode = it.getInt("initial_display_mode", DISPLAY_NOTES)
            viewModel.updateDisplayMode(initialDisplayMode)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_note, container, false)
        initializeViews(view)
        setupRecyclerView()
        setupFab()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        actionMode?.finish()
        (activity as? MainActivity)?.setToolbarVisibility(true)
    }

    // ==============================================
    // Initialization Methods
    // ==============================================
    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        fabAddNote = view.findViewById(R.id.fab_add_note)
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(emptyList(), this)
        recyclerView.apply {
            adapter = noteAdapter
            itemAnimator = DefaultItemAnimator()
        }
        setupObservers()
    }

    private fun setupFab() {
        fabAddNote.setOnClickListener {
            startActivity(Intent(activity, NoteEditActivity::class.java))
        }
    }

    // ==============================================
    // Observer Setup
    // ==============================================
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredNotes.collect { notes ->
                        noteAdapter.updateNotes(notes)
                    }
                }

                launch {
                    viewModel.isGridLayout.collect { isGrid ->
                        setupLayoutManager(isGrid)
                    }
                }

                launch {
                    viewModel.displayMode.collect { mode ->
                        updateFabVisibility(mode)
                    }
                }

                launch {
                    viewModel.selectedNotes.collect { selectedNotes ->
                        updateActionModeTitle(selectedNotes.size)
                    }
                }
            }
        }
    }

    // ==============================================
    // UI Update Methods
    // ==============================================
    private fun setupLayoutManager(isGridLayout: Boolean) {
        val spanCount = if (isGridLayout) 2 else 1
        recyclerView.layoutManager = StaggeredGridLayoutManager(
            spanCount,
            StaggeredGridLayoutManager.VERTICAL
        )
    }

    private fun updateFabVisibility(displayMode: Int) {
        fabAddNote.visibility = if (displayMode == DISPLAY_BIN) View.GONE else View.VISIBLE
    }

    private fun updateActionModeTitle(selectedCount: Int) {
        actionMode?.title = "$selectedCount selected"
    }

    // ==============================================
    // Action Mode Callback
    // ==============================================
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_note_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            when (viewModel.displayMode.value) {
                DISPLAY_BIN -> {
                    menu.findItem(R.id.action_permanenlt_delete).isVisible = true
                    menu.findItem(R.id.action_archive).isVisible = false
                    menu.findItem(R.id.action_labels).isVisible = false
                    menu.findItem(R.id.action_delete).setIcon(R.drawable.restore)
                }
                DISPLAY_ARCHIVE -> {
                    menu.findItem(R.id.action_archive).setIcon(R.drawable.unarchive)
                }
            }
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_permanenlt_delete -> {
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
            (activity as MainActivity).setToolbarVisibility(true)
        }
    }

    // ==============================================
    // NoteAdapter Callbacks
    // ==============================================
    override fun onNoteClick(note: Note) {
        val intent = Intent(activity, NoteEditActivity::class.java)
        intent.putExtra("NOTE_ID", note.id)
        startActivity(intent)
    }

    override fun onSelectionModeStarted() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        fabAddNote.visibility = View.GONE
        (activity as MainActivity).setToolbarVisibility(false)
    }

    override fun onSelectionModeEnded() {
        actionMode?.finish()
    }

    override fun onSelectionChanged(selectedNotes: Set<Note>) {
        viewModel.setSelectedNotes(selectedNotes)
    }

    // ==============================================
    // Public Methods
    // ==============================================
    fun filterNotes(query: String) {
        viewModel.setSearchQuery(query)
    }

    fun updateDisplayMode(newMode: Int, labelId: String? = null) {
        viewModel.updateDisplayMode(newMode, labelId)
    }

    override fun onLayoutToggle(isGridLayout: Boolean) {
        viewModel.toggleLayoutMode(isGridLayout)
    }

    // ==============================================
    // Label Dialog Methods
    // ==============================================
    fun showLabelDialog(mode: ActionMode) {
        if (context == null) return

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Manage Labels")

        val scrollView = ScrollView(context)
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 16, 24, 0)

        val loadingText = EditText(context)
        loadingText.isEnabled = false
        loadingText.setText("Loading labels...")
        layout.addView(loadingText)

        scrollView.addView(layout)
        dialogBuilder.setView(scrollView)

        val editText = EditText(context)
        editText.hint = "Create a New Label"

        dialogBuilder.setPositiveButton("OK") { _, _ -> }
        dialogBuilder.setNegativeButton("Cancel") { _, _ -> }

        val dialog = dialogBuilder.create()
        dialog.show()

        val checkBoxes = mutableMapOf<CheckBox, String>()
        val initialLabelStates = mutableMapOf<String, Boolean>()

        viewModel.fetchLabels()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.labelDataBridge.labelsState.collect { labels ->
                if (dialog.isShowing) {
                    layout.removeAllViews()
                    checkBoxes.clear()

                    if (labels.isEmpty()) {
                        val noLabelsText = EditText(context)
                        noLabelsText.isEnabled = false
                        noLabelsText.setText("No existing labels. Create one below.")
                        layout.addView(noLabelsText)
                    } else {
                        labels.forEach { label ->
                            val anyNoteHasLabel = viewModel.selectedNotes.value.any {
                                it.labels.contains(label.id)
                            }
                            initialLabelStates[label.id] = anyNoteHasLabel

                            val checkBox = CheckBox(context)
                            checkBox.text = label.name
                            checkBox.isChecked = anyNoteHasLabel
                            checkBoxes[checkBox] = label.id
                            layout.addView(checkBox)
                        }
                    }

                    val paddingView = View(context)
                    paddingView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        24
                    )
                    layout.addView(paddingView)
                    layout.addView(editText)

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val checkedLabelIds = checkBoxes.filter { it.key.isChecked }.map { it.value }
                        val uncheckedLabelIds = checkBoxes.filter { !it.key.isChecked }.map { it.value }
                        val newLabelName = editText.text.toString().trim()
                        var newLabelId = ""

                        if (newLabelName.isNotEmpty()) {
                            newLabelId = viewModel.addNewLabel(newLabelName)
                        }

                        viewModel.updateSelectedNotesLabels(checkedLabelIds, uncheckedLabelIds, newLabelId)
                        dialog.dismiss()
                        mode.finish()
                    }
                }
            }
        }
    }
}