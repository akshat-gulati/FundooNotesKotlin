package com.example.fundoonotes.ui.labels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NoteLabelDataBridge
import com.example.fundoonotes.databinding.FragmentLabelsBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class LabelsFragment : Fragment() {

    private var _binding: FragmentLabelsBinding? = null
    private val binding get() = _binding!!

    // ==============================================
    // UI Components
    // ==============================================
    private val recyclerView: RecyclerView by lazy { binding.recyclerViewLabels }
    private val etNewLabel: EditText by lazy { binding.etNewLabel }
    private val btnAddLabel: Button by lazy { binding.btnAddLabel }

    // ==============================================
    // Data Repositories
    // ==============================================
    private val labelDataBridge: LabelDataBridge by lazy { LabelDataBridge(requireContext()) }
    private val noteLabelDataBridge: NoteLabelDataBridge by lazy { NoteLabelDataBridge(requireContext()) }

    // ==============================================
    // Adapter & Data
    // ==============================================
    private lateinit var adapter: LabelAdapter
    private val labels = mutableListOf<Label>()

    // ==============================================
    // Coroutine Management
    // ==============================================
    private var labelsCollectionJob: Job? = null

    // ==============================================
    // Lifecycle Methods
    // ==============================================
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLabelsBinding.inflate(inflater, container, false)
        setupUIComponents()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchInitialData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanupResources()
    }

    // ==============================================
    // UI Setup Methods
    // ==============================================
    private fun setupUIComponents() {
        setupRecyclerView()
        setupAddLabelButton()
        observeLabels()
    }

    private fun setupRecyclerView() {
        adapter = LabelAdapter(labels, object : LabelAdapter.OnLabelClickListener {
            override fun onLabelClick(label: Label) {
                // Currently no action on label click
            }

            override fun onLabelDelete(label: Label, position: Int) {
                deleteLabel(label)
            }

            override fun onLabelEdit(label: Label, newName: String) {
                updateLabel(label, newName)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupAddLabelButton() {
        btnAddLabel.setOnClickListener {
            addNewLabel()
        }
    }

    // ==============================================
    // Data Operations
    // ==============================================
    private fun fetchInitialData() {
        labelDataBridge.fetchLabels()
    }

    private fun observeLabels() {
        labelsCollectionJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelDataBridge.labelsState.collect { labelsList ->
                    updateLabelsList(labelsList)
                }
            }
        }
    }

    private fun updateLabelsList(labelsList: List<Label>) {
        labels.clear()
        labels.addAll(labelsList)
        adapter.notifyDataSetChanged()
    }

    private fun addNewLabel() {
        val labelName = etNewLabel.text.toString().trim()
        if (labelName.isNotEmpty()) {
            val labelId = UUID.randomUUID().toString()
            labelDataBridge.addNewLabel(labelId, labelName)
            etNewLabel.text.clear()
        }
    }

    private fun deleteLabel(label: Label) {
        noteLabelDataBridge.deleteLabel(label.id)
    }

    private fun updateLabel(label: Label, newName: String) {
        labelDataBridge.updateLabel(label.id, newName, label.noteIds)
    }

    // ==============================================
    // Cleanup Methods
    // ==============================================
    private fun cleanupResources() {
        labelsCollectionJob?.cancel()
    }
}