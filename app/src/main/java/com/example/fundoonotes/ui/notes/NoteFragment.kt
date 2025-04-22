package com.example.fundoonotes.ui.notes

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
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
import com.example.fundoonotes.ui.LayoutToggleListener
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteFragment : Fragment(), LayoutToggleListener, OnNoteClickListener {

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
    private lateinit var labelDialogManager: LabelDialogManager
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val animationSlideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        val controller = LayoutAnimationController(animationSlideUp)
        recyclerView.layoutAnimation = controller
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
        labelDialogManager = LabelDialogManager(requireContext(), viewLifecycleOwner, viewModel)
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
                    labelDialogManager.showLabelDialog(mode)
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
}