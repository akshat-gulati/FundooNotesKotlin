package com.example.fundoonotes.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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

    interface OnNoteClickListener {
        fun onNoteClick(note: Note)
        fun onNoteLongClick(note: Note, position: Int): Boolean
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


        if (note.reminderTime != null) {
            holder.llReminder.visibility = View.VISIBLE
            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            holder.tvTimeDate.text = dateFormat.format(Date(note.reminderTime))
        } else {
            holder.llReminder.visibility = View.GONE
        }
// Tight Coupling with FirestoreLabelRepository
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



        holder.itemView.setOnClickListener {
            onNoteClickListener.onNoteClick(note)
        }

        holder.itemView.setOnLongClickListener {
            onNoteClickListener.onNoteLongClick(note, position)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}

