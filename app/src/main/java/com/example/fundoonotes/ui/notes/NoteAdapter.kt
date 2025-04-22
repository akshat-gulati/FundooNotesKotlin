package com.example.fundoonotes.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Note
import com.example.fundoonotes.data.repository.firebase.FirestoreLabelRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private var notes: List<Note>,
    private val onNoteClickListener: OnNoteClickListener
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    // ==============================================
    // ViewHolder Class
    // ==============================================
    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val llReminder: LinearLayout = itemView.findViewById(R.id.llReminder)
        val tvTimeDate: TextView = itemView.findViewById(R.id.tvTimeDate)
        val tvLabelName: TextView = itemView.findViewById(R.id.tvLabelName)
        val tvLabelName2: TextView = itemView.findViewById(R.id.tvLabelName2)
        val tvLabelName3: TextView = itemView.findViewById(R.id.tvLabelName3)
    }

    // ==============================================
    // Properties
    // ==============================================
    private val labelCache = mutableMapOf<String, List<String>>()
    private val selectedItems = mutableSetOf<String>()
    private var isInSelectionMode = false

    // ==============================================
    // Adapter Overrides
    // ==============================================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        bindNoteData(holder, note)
        setupClickListeners(holder, note)
    }

    override fun getItemCount(): Int = notes.size

    // ==============================================
    // Data Binding Methods
    // ==============================================
    private fun bindNoteData(holder: NoteViewHolder, note: Note) {
        holder.tvTitle.text = note.title
        holder.tvDescription.text = note.description
        updateSelectionUI(holder, note)
        bindReminderData(holder, note)
        bindLabelData(holder, note)
    }

    private fun updateSelectionUI(holder: NoteViewHolder, note: Note) {
        holder.itemView.setBackgroundResource(
            if (selectedItems.contains(note.id)) R.drawable.selected_card_border
            else R.drawable.card_border
        )
    }

    private fun bindReminderData(holder: NoteViewHolder, note: Note) {
        if (note.reminderTime != null) {
            holder.llReminder.visibility = View.VISIBLE
            holder.tvTimeDate.text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                .format(Date(note.reminderTime))
        } else {
            holder.llReminder.visibility = View.GONE
        }
    }

    private fun bindLabelData(holder: NoteViewHolder, note: Note) {
        // Reset visibility of all label views
        holder.tvLabelName.visibility = View.GONE
        holder.tvLabelName2.visibility = View.GONE
        holder.tvLabelName3.visibility = View.GONE

        if (note.labels.isNotEmpty()) {
            val noteLabelsKey = note.id
            val cachedLabels = labelCache[noteLabelsKey]

            if (cachedLabels != null) {
                displayLabels(holder, cachedLabels)
            } else {
                fetchAndCacheLabels(holder, note)
            }
        }
    }
    private fun displayLabels(holder: NoteViewHolder, labelNames: List<String>) {
        when {
            labelNames.isEmpty() -> {
                // No labels to display
                holder.tvLabelName.visibility = View.GONE
                holder.tvLabelName2.visibility = View.GONE
                holder.tvLabelName3.visibility = View.GONE
            }
            labelNames.size == 1 -> {
                // Only one label
                holder.tvLabelName.visibility = View.VISIBLE
                holder.tvLabelName.text = labelNames[0]
                holder.tvLabelName2.visibility = View.GONE
                holder.tvLabelName3.visibility = View.GONE
            }
            labelNames.size == 2 -> {
                // Exactly two labels
                holder.tvLabelName.visibility = View.VISIBLE
                holder.tvLabelName.text = labelNames[0]
                holder.tvLabelName2.visibility = View.VISIBLE
                holder.tvLabelName2.text = labelNames[1]
                holder.tvLabelName3.visibility = View.GONE
            }
            else -> {
                // More than two labels
                holder.tvLabelName.visibility = View.VISIBLE
                holder.tvLabelName.text = labelNames[0]
                holder.tvLabelName2.visibility = View.VISIBLE
                holder.tvLabelName2.text = labelNames[1]
                holder.tvLabelName3.visibility = View.VISIBLE
                holder.tvLabelName3.text = "+${labelNames.size - 2}" // Show remaining count
            }
        }
    }


    private fun fetchAndCacheLabels(holder: NoteViewHolder, note: Note) {
        val firestoreLabelRepository = FirestoreLabelRepository(holder.itemView.context)
        firestoreLabelRepository.fetchLabelsByIds(note.labels) { labels ->
            val labelNames = labels.map { it.name }
            labelCache[note.id] = labelNames

            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                holder.bindingAdapterPosition < notes.size &&
                notes[holder.bindingAdapterPosition].id == note.id) {

                displayLabels(holder, labelNames)
            }
        }
    }

    // ==============================================
    // Click Listeners
    // ==============================================
    private fun setupClickListeners(holder: NoteViewHolder, note: Note) {
        holder.itemView.setOnClickListener {
            if (isInSelectionMode) {
                toggleSelection(note)
            } else {
                onNoteClickListener.onNoteClick(note)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isInSelectionMode) {
                isInSelectionMode = true
                toggleSelection(note)
                onNoteClickListener.onSelectionModeStarted()
            } else {
                toggleSelection(note)
            }
            true
        }
    }

    // ==============================================
    // Selection Management
    // ==============================================
    private fun toggleSelection(note: Note) {
        if (selectedItems.contains(note.id)) {
            selectedItems.remove(note.id)
        } else {
            selectedItems.add(note.id)
        }

        if (selectedItems.isEmpty() && isInSelectionMode) {
            exitSelectionMode()
        } else {
            onNoteClickListener.onSelectionChanged(getSelectedNotes())
            notifyDataSetChanged()
        }
    }

    fun exitSelectionMode() {
        if (isInSelectionMode) {
            isInSelectionMode = false
            selectedItems.clear()
            onNoteClickListener.onSelectionModeEnded()
            notifyDataSetChanged()
        }
    }

    // ==============================================
    // Public Methods
    // ==============================================
    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    fun getSelectedNotes(): Set<Note> {
        return notes.filter { note -> selectedItems.contains(note.id) }.toSet()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(notes.map { it.id })
        onNoteClickListener.onSelectionChanged(getSelectedNotes())
        notifyDataSetChanged()
    }
}