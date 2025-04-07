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

    // Multi-selection support
    private val selectedItems = mutableSetOf<String>()
    private var isInSelectionMode = false

    interface OnNoteClickListener {
        fun onNoteClick(note: Note)
        fun onSelectionModeStarted()
        fun onSelectionModeEnded()
        fun onSelectionChanged(selectedNotes: Set<Note>)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val llReminder: LinearLayout = itemView.findViewById(R.id.llReminder)
        val tvTimeDate: TextView = itemView.findViewById(R.id.tvTimeDate)
        val tvLabelName: TextView = itemView.findViewById(R.id.tvLabelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_row, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        holder.tvTitle.text = note.title
        holder.tvDescription.text = note.description

        // Highlight selected items
        if (selectedItems.contains(note.id)) {
            holder.itemView.setBackgroundResource(
                R.drawable.selected_card_border
            )
        } else {
                holder.itemView.setBackgroundResource(
                    R.drawable.card_border
                )
        }

        if (note.reminderTime != null) {
            holder.llReminder.visibility = View.VISIBLE
            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            holder.tvTimeDate.text = dateFormat.format(Date(note.reminderTime))
        } else {
            holder.llReminder.visibility = View.GONE
        }

        if (note.labels.isNotEmpty()) {
            val firestoreLabelRepository = FirestoreLabelRepository(holder.itemView.context)
            firestoreLabelRepository.fetchLabelsByIds(note.labels) { labels ->
                val labelNames = labels.map { it.name }
                holder.tvLabelName.visibility = View.VISIBLE
                holder.tvLabelName.text = labelNames.joinToString(", ")
            }
        } else {
            holder.tvLabelName.visibility = View.GONE
        }

        // Handle click events based on selection mode
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

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    // Methods for multi-selection mode
    private fun toggleSelection(note: Note) {
        if (selectedItems.contains(note.id)) {
            selectedItems.remove(note.id)
        } else {
            selectedItems.add(note.id)
        }

        if (selectedItems.isEmpty() && isInSelectionMode) {
            exitSelectionMode()
        } else {
            // Notify listener about selection changes
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

    fun isInSelectionMode(): Boolean {
        return isInSelectionMode
    }

    fun getSelectedItems(): Set<String> {
        return selectedItems.toSet()
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

    fun clearSelections() {
        selectedItems.clear()
        onNoteClickListener.onSelectionChanged(getSelectedNotes())
        notifyDataSetChanged()
    }
}