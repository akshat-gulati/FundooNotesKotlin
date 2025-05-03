package com.example.fundoonotes.ui.labels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.databinding.ActivityMainBinding
import com.example.fundoonotes.databinding.ItemLabelBinding
import com.google.android.material.textfield.TextInputEditText

class LabelAdapter(
    private val labels: List<Label>,
    private val listener: OnLabelClickListener
) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

    // ==============================================
    // Interface Definition
    // ==============================================
    interface OnLabelClickListener {
        fun onLabelClick(label: Label)
        fun onLabelDelete(label: Label, position: Int)
        fun onLabelEdit(label: Label, newName: String)
    }

    // ==============================================
    // ViewHolder Class
    // ==============================================
    class LabelViewHolder(val binding: ItemLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        val labelText: TextInputEditText = binding.tvLabelName
        val deleteButton: ImageButton = binding.tvDeleteLabel
        val editButton: ImageView = binding.ibEditLabel
    }

    // ==============================================
    // RecyclerView.Adapter Overrides
    // ==============================================
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemLabelBinding.inflate(
            LayoutInflater.from(parent.context), parent,false)
        return LabelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = labels[position]
        setupViewHolder(holder, label, position)
    }

    override fun getItemCount(): Int = labels.size

    // ==============================================
    // ViewHolder Setup
    // ==============================================
    private fun setupViewHolder(holder: LabelViewHolder, label: Label, position: Int) {
        // Initialize view state
        holder.labelText.setText(label.name)
        holder.labelText.isEnabled = false

        // Set up click listeners
        setupClickListeners(holder, label, position)
    }

    // ==============================================
    // Event Handlers
    // ==============================================
    private fun setupClickListeners(holder: LabelViewHolder, label: Label, position: Int) {
        // Item click listener
        holder.itemView.setOnClickListener {
            listener.onLabelClick(label)
        }

        // Delete button listener
        holder.deleteButton.setOnClickListener {
            listener.onLabelDelete(label, position)
        }

        // Edit mode toggle function
        val toggleEditMode = createEditModeToggle(holder, label)

        // Edit button listener
        holder.editButton.setOnClickListener { toggleEditMode() }

        // Label text click listener
        holder.labelText.setOnClickListener { toggleEditMode() }
    }

    // ==============================================
    // Edit Mode Management
    // ==============================================
    private fun createEditModeToggle(holder: LabelViewHolder, label: Label): () -> Unit {
        return {
            val isCurrentlyEditing = holder.labelText.isEnabled

            if (isCurrentlyEditing) {
                // Save changes and exit edit mode
                val newName = holder.labelText.text.toString().trim()
                listener.onLabelEdit(label, newName)
                holder.labelText.isEnabled = false
                holder.editButton.setImageResource(R.drawable.edit)
            } else {
                // Enter edit mode
                holder.labelText.isEnabled = true
                holder.labelText.requestFocus()
                holder.editButton.setImageResource(R.drawable.check)
            }
        }
    }
}