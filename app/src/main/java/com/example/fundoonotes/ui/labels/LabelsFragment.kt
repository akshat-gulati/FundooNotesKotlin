package com.example.fundoonotes.ui.labels;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
import com.example.fundoonotes.data.repository.NoteLabelRepository
import java.util.UUID
import kotlin.String


class LabelsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etNewLabel: EditText
    private lateinit var btnAddLabel: Button
    private lateinit var labelDataBridge: LabelDataBridge
    private lateinit var adapter: LabelAdapter
    private val labels = mutableListOf<Label>()

    private lateinit var NoteLabelRepository: NoteLabelRepository

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
        NoteLabelRepository = NoteLabelRepository(requireContext())

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
//                // Navigate to notes with this label
//                (activity as MainActivity).navigateToLabelNotes(label.id)
            }

            override fun onLabelDelete(label: Label, position: Int) {
                // Remove the label from Firestore
                NoteLabelRepository.deleteLabel(label.id)
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
                val labelId = UUID.randomUUID().toString()
                labelDataBridge.addNewLabel(labelId,labelName)
                etNewLabel.text.clear()
            }
        }
    }


}