package com.example.fundoonotes.ui.labels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R

class LabelsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etNewLabel: EditText
    private lateinit var btnAddLabel: Button
    private val labels = mutableListOf("Personal", "Work", "Home", "Health", "Ideas")
    private lateinit var adapter: LabelAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_labels, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLabels)
        etNewLabel = view.findViewById(R.id.etNewLabel)
        btnAddLabel = view.findViewById(R.id.btnAddLabel)

        setupRecyclerView()
        setupAddLabelButton()

        return view
    }

    private fun setupRecyclerView() {
        adapter = LabelAdapter(labels, object : LabelAdapter.OnLabelClickListener {
            override fun onLabelClick(label: String) {
                // Navigate to notes with this label
                (activity as MainActivity).navigateToLabelNotes(label)
            }

            override fun onLabelDelete(label: String, position: Int) {
                // Remove the label
                labels.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupAddLabelButton() {
        btnAddLabel.setOnClickListener {
            val labelName = etNewLabel.text.toString().trim()
            if (labelName.isNotEmpty()) {
                labels.add(labelName)
                adapter.notifyItemInserted(labels.size - 1)
                etNewLabel.text.clear()
            }
        }
    }

    class LabelAdapter(
        private val labels: List<String>,
        private val listener: OnLabelClickListener
    ) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

        interface OnLabelClickListener {
            fun onLabelClick(label: String)
            fun onLabelDelete(label: String, position: Int)
        }

        class LabelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val labelText: TextView = view.findViewById(R.id.tvLabelName)
            val deleteButton: TextView = view.findViewById(R.id.tvDeleteLabel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_label, parent, false)
            return LabelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
            val label = labels[position]
            holder.labelText.text = label

            holder.itemView.setOnClickListener {
                listener.onLabelClick(label)
            }

            holder.deleteButton.setOnClickListener {
                listener.onLabelDelete(label, position)
            }
        }

        override fun getItemCount() = labels.size
    }
}