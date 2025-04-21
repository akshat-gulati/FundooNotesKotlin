package com.example.fundoonotes.data.repository.dataBridge

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.fundoonotes.core.NetworkManager
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.repository.interfaces.LabelInterface
import com.example.fundoonotes.data.repository.firebase.FirestoreLabelRepository
import com.example.fundoonotes.data.repository.room.RoomLabelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LabelDataBridge(private val context: Context) : LabelInterface {

    // ==============================================
    // Companion Object (Constants)
    // ==============================================
    companion object {
        private const val TAG = "LabelsDataBridge"
    }

    // ==============================================
    // Properties and Initialization
    // ==============================================
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _labelsState = MutableStateFlow<List<Label>>(emptyList())
    val labelsState: StateFlow<List<Label>> = _labelsState.asStateFlow()

    private val firestoreLabelRepository: FirestoreLabelRepository = FirestoreLabelRepository(context)
    private val roomLabelRepository: RoomLabelRepository = RoomLabelRepository(context)
    private val networkManager = NetworkManager(context)
    val networkState: StateFlow<Boolean> = networkManager.networkState

    init {
        observeLabels()
    }

    // ==============================================
    // Data Observation
    // ==============================================
    private fun observeLabels() {
        // Observe network state changes
        coroutineScope.launch {
            networkState.collect { isOnline ->
                if (isOnline) {
                    _labelsState.value = firestoreLabelRepository.labelsState.value
                } else {
                    _labelsState.value = roomLabelRepository.labelsState.value
                }
            }
        }

        // Collect from both repositories
        coroutineScope.launch {
            // Always collect from Room for local changes
            launch {
                roomLabelRepository.labelsState.collect { roomLabels ->
                    if (!networkManager.isOnline()) {
                        _labelsState.value = roomLabels
                    }
                }
            }

            // Always collect from Firestore for remote changes
            launch {
                firestoreLabelRepository.labelsState.collect { firestoreLabels ->
                    if (networkManager.isOnline()) {
                        _labelsState.value = firestoreLabels
                    }
                }
            }
        }
    }

    // ==============================================
    // Core CRUD Operations
    // ==============================================
    override fun fetchLabelById(labelId: String, onSuccess: (Label) -> Unit) {
        if (networkManager.isOnline()) {
            firestoreLabelRepository.fetchLabelById(labelId, onSuccess)
        } else {
            roomLabelRepository.fetchLabelById(labelId, onSuccess)
        }
    }

    override fun fetchLabels() {
        if (networkManager.isOnline()) {
            firestoreLabelRepository.fetchLabels()
        } else {
            roomLabelRepository.fetchLabels()
        }
    }

    override fun addNewLabel(labelId: String, labelName: String): String {
        roomLabelRepository.addNewLabel(labelId, labelName)
        firestoreLabelRepository.addNewLabel(labelId, labelName)
        return labelId
    }

    override fun updateLabel(labelId: String, labelName: String, noteIds: List<String>) {
        roomLabelRepository.updateLabel(labelId, labelName, noteIds)
        firestoreLabelRepository.updateLabel(labelId, labelName, noteIds)
    }

    override fun deleteLabel(labelId: String) {
        firestoreLabelRepository.deleteLabel(labelId)
        roomLabelRepository.deleteLabel(labelId)
    }

    fun cleanRoomDB(){
        try {
        roomLabelRepository.clearAllData()
            Toast.makeText(context, "Deleted room db", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception){
            Toast.makeText(context, "Unable to Delete $e", Toast.LENGTH_SHORT).show()
        }
    }
}