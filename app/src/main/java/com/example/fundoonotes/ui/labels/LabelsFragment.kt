package com.example.fundoonotes.ui.labels;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.view.View
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.R
import com.google.android.material.textfield.TextInputEditText
import kotlin.String


class LabelsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etNewLabel: EditText
    private lateinit var btnAddLabel: Button
    private lateinit var labelDataBridge: LabelDataBridge
    private lateinit var adapter: LabelAdapter
    private val labels = mutableListOf<Label>()

    private var labelsCollectionJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_labels, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLabels)
        etNewLabel = view.findViewById(R.id.etNewLabel)
        btnAddLabel = view.findViewById(R.id.btnAddLabel)
        labelDataBridge = LabelDataBridge(requireContext())

        setupRecyclerView()
        setupAddLabelButton()
        observeLabels()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        labelDataBridge.fetchLabels()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        labelsCollectionJob?.cancel()
    }

    private fun observeLabels() {
        labelsCollectionJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelDataBridge.labelsState.collect { labelsList ->
                    labels.clear()
                    labels.addAll(labelsList)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = LabelAdapter(labels, object : LabelAdapter.OnLabelClickListener {
            override fun onLabelClick(label: Label) {
                // Navigate to notes with this label
                (activity as MainActivity).navigateToLabelNotes(label.id)
            }

            override fun onLabelDelete(label: Label, position: Int) {
                // Remove the label from Firestore
                labelDataBridge.deleteLabel(label.id)
            }

            override fun onLabelEdit(
                label: Label,
                newName: String
            ) {
                labelDataBridge.updateLabel(label.id, newName, label.noteIds)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupAddLabelButton() {
        btnAddLabel.setOnClickListener {
            val labelName = etNewLabel.text.toString().trim()
            if (labelName.isNotEmpty()) {
                labelDataBridge.addNewLabel(labelName)
                etNewLabel.text.clear()
            }
        }
    }

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
}