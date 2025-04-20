package com.example.fundoonotes.data.repository.dataBridge

import com.example.fundoonotes.data.model.Label
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.fundoonotes.core.NetworkManager
import com.example.fundoonotes.data.repository.interfaces.LabelInterface
import com.example.fundoonotes.data.repository.firebase.FirestoreLabelRepository
import com.example.fundoonotes.data.repository.room.RoomLabelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LabelDataBridge(private val context: Context): LabelInterface {

    companion object{
        private const val TAG = "LabelsDataBridge"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val _labelsState = MutableStateFlow<List<Label>>(emptyList())
    val labelsState: StateFlow<List<Label>> = _labelsState.asStateFlow()
    val firestoreLabelRepository: FirestoreLabelRepository = FirestoreLabelRepository(context)
    private val roomLabelRepository: RoomLabelRepository = RoomLabelRepository(context)

    // Use the NetworkManager for network state monitoring
    private val networkManager = NetworkManager(context)
    val networkState: StateFlow<Boolean> = networkManager.networkState

    init {
       observeLabels()
    }

    private fun observeLabels() {

                coroutineScope.launch {
                    networkState.collect { isOnline ->
                        if (isOnline) {
                            _labelsState.value = firestoreLabelRepository.labelsState.value
                        } else {
                            _labelsState.value = roomLabelRepository.labelsState.value
                        }
                    }
                }


        // Start collecting from both repositories
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
                firestoreLabelRepository.labelsState.collect { firestoreLabelRepository ->
                    if (networkManager.isOnline()) {
                        _labelsState.value = firestoreLabelRepository
                    }
                }
            }
        }
    }


    override fun fetchLabelById(
        labelId: String,
        onSuccess: (Label) -> Unit
    ) {
        if (networkManager.isOnline()) {
            firestoreLabelRepository.fetchLabelById(labelId, onSuccess)
        }
        else {
            roomLabelRepository.fetchLabelById(labelId, onSuccess)
        }
    }

    override fun fetchLabels() {
        if (networkManager.isOnline()){
            firestoreLabelRepository.fetchLabels()
        }
        else{
            roomLabelRepository.fetchLabels()
        }
    }

    override fun addNewLabel(labelId: String,labelName: String): String {
        roomLabelRepository.addNewLabel(labelId, labelName)
        firestoreLabelRepository.addNewLabel(labelId, labelName)

        return labelId
    }

    override fun updateLabel(
        labelId: String,
        labelName: String,
        noteIds: List<String>
    ) {
        roomLabelRepository.updateLabel(labelId, labelName, noteIds)
        firestoreLabelRepository.updateLabel(labelId, labelName, noteIds)
    }

    override fun deleteLabel(labelId: String) {
        firestoreLabelRepository.deleteLabel(labelId)
        roomLabelRepository.deleteLabel(labelId)
    }

}