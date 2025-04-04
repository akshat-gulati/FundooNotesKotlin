package com.example.fundoonotes.ui.labels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Label
import com.google.android.material.textfield.TextInputEditText

class LabelAdapter(
    private val labels: List<Label>,
    private val listener: OnLabelClickListener
) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

    interface OnLabelClickListener {
        fun onLabelClick(label: Label)
        fun onLabelDelete(label: Label, position: Int)
        fun onLabelEdit(label: Label, newName: String)
    }

    class LabelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelText: TextInputEditText = view.findViewById(R.id.tvLabelName)
        val deleteButton: ImageButton = view.findViewById(R.id.tvDeleteLabel)
        val editButton: ImageButton = view.findViewById(R.id.ibEditLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_label, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = labels[position]
        holder.labelText.setText(label.name)

        // Disable editing by default
        holder.labelText.isEnabled = false

        holder.itemView.setOnClickListener {
            listener.onLabelClick(label)
        }

        holder.deleteButton.setOnClickListener {
            listener.onLabelDelete(label, position)
        }

        holder.editButton.setOnClickListener {
            // Toggle edit mode
            val isCurrentlyEditing = holder.labelText.isEnabled

            if (isCurrentlyEditing) {
                // Save changes
                val newName = holder.labelText.text.toString().trim()
                listener.onLabelEdit(label, newName)
                holder.labelText.isEnabled = false
            } else {
                // Enable editing
                holder.labelText.isEnabled = true
                holder.labelText.requestFocus()
            }
        }
    }

    override fun getItemCount() = labels.size
}