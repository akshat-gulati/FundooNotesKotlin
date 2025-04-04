package com.example.fundoonotes.data.repository

import com.example.fundoonotes.data.model.Label
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LabelDataBridge(private val context: Context): LabelRepository {

    companion object{
        private const val TAG = "LabelsDataBridge"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _labelsState = MutableStateFlow<List<Label>>(emptyList())
    val labelsState: StateFlow<List<Label>> = _labelsState.asStateFlow()
    private val firestoreLabelRepository: FirestoreLabelRepository = FirestoreLabelRepository(context)
    private var activeRepository: LabelRepository = firestoreLabelRepository


    override fun fetchLabelById(
        labelId: String,
        onSuccess: (Label) -> Unit
    ) {
        activeRepository.fetchLabelById(labelId, onSuccess)
    }

    override fun fetchLabels() {
        activeRepository.fetchLabels()
    }

    override fun addNewLabel(labelName: String): String {
        return activeRepository.addNewLabel(labelName)
    }

    override fun updateLabel(
        labelId: String,
        labelName: String,
        noteIds: List<String>
    ) {
        activeRepository.updateLabel(labelId, labelName, noteIds)
    }

    override fun deleteLabel(labelId: String) {
        activeRepository.deleteLabel(labelId)
    }

}