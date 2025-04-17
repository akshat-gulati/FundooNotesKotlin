package com.example.fundoonotes.data.repository.room

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.repository.interfaces.LabelInterface
import com.example.fundoonotes.data.repository.room.entity.LabelEntity
import com.google.android.play.integrity.internal.l
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomLabelRepository(context: Context): LabelInterface {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val labelDao = AppDatabase.getDatabase(context).labelDao()
    private val _labelsState = MutableStateFlow<List<Label>>(emptyList())
    val labelsState: StateFlow<List<Label>> = _labelsState.asStateFlow()

    companion object{
        private const val TAG = "RoomLabelRepository"
    }

    init {
        // Start collecting labels from Room database
        setupRealtimeUpdates()
    }

    private fun setupRealtimeUpdates() {
        coroutineScope.launch {
            labelDao.getAllLabels()
                .map { entityList -> entityList.map { it.toLabel() } }
                .collect { labels ->
                    _labelsState.value = labels
                    Log.d(TAG, "Real-time update received: ${labels.size} labels")
                }
        }
    }

    override fun fetchLabelById(
        labelId: String,
        onSuccess: (Label) -> Unit
    ) {
        coroutineScope.launch {
            val labelEntity = labelDao.getLabelById(labelId)
            labelEntity?.let {
                val label = it.toLabel()
                withContext(Dispatchers.Main) {
                    onSuccess(label)
                }
            }
        }
    }

    override fun fetchLabels() {
        // No need to explicitly fetch labels since we're using Flow
        // The StateFlow is automatically updated by the Room Flow collection
    }

    override fun addNewLabel(labelId: String, labelName: String): String {
        val label = Label(
            id = labelId,
            name = labelName,
        )
        coroutineScope.launch {
            try {
                val labelEntity = LabelEntity.fromLabel(label)
                labelDao.insertLabel(labelEntity)
            }
            catch (e: Exception) {
                Log.e(TAG, "Error adding label to Room: ${e.message}")
            }
        }
        return labelId
    }

    override fun updateLabel(labelId: String, labelName: String, noteIds: List<String>) {
        coroutineScope.launch {
            try {
                // Get current label first to preserve any fields not being updated
                val currentLabel = labelDao.getLabelById(labelId)
                currentLabel?.let {
                    val updatedEntity = it.copy(
                        name = labelName,
                        noteIds = noteIds.joinToString(",")
                    )
                    labelDao.updateLabel(updatedEntity)
                    Log.d(TAG, "Label updated successfully: $labelId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Label: ${e.message}")
            }
        }
    }

    override fun deleteLabel(labelId: String) {
        coroutineScope.launch {
            try {
                labelDao.deleteLabel(labelId)
                Log.d(TAG, "Label deleted successfully: $labelId")
            }
            catch (e: Exception){
                Log.e(TAG, "Error deleting Label: ${e.message}")
            }
        }
    }
}