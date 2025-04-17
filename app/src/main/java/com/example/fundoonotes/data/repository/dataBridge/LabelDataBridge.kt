package com.example.fundoonotes.data.repository.dataBridge

import com.example.fundoonotes.data.model.Label
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
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

    init {
       observeLabels()
    }

    private fun observeLabels() {
        // Observe network state changes
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Set up network callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // When network becomes available, switch to Firestore data and sync
                coroutineScope.launch {
                    _labelsState.value = firestoreLabelRepository.labelsState.value
//                    syncRoomToFirestore() // Sync any changes made while offline
                }
            }

            override fun onLost(network: Network) {
                // When network is lost, switch to Room data
                coroutineScope.launch {
                    _labelsState.value = roomLabelRepository.labelsState.value
                }
            }
        }

        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Start collecting from both repositories
        coroutineScope.launch {
            // Always collect from Room for local changes
            launch {
                roomLabelRepository.labelsState.collect { roomLabels ->
                    if (!isOnline(context)) {
                        _labelsState.value = roomLabels
                        Log.d(TAG, "New Room notes received: ${roomLabels.size}")
                    }
                }
            }

            // Always collect from Firestore for remote changes
            launch {
                firestoreLabelRepository.labelsState.collect { firestoreLabelRepository ->
                    if (isOnline(context)) {
                        _labelsState.value = firestoreLabelRepository
                        Log.d(TAG, "New Firestore notes received: ${firestoreLabelRepository.size}")

                        // Update Room with the latest Firestore data to keep local copy updated
//                        updateRoomFromFirestore(firestoreNotes)
                    }
                }
            }
        }
    }


    override fun fetchLabelById(
        labelId: String,
        onSuccess: (Label) -> Unit
    ) {
        if (isOnline(context)) {
            firestoreLabelRepository.fetchLabelById(labelId, onSuccess)
        }
        else {
            firestoreLabelRepository.fetchLabelById(labelId, onSuccess)
        }
    }

    override fun fetchLabels() {
        if (isOnline(context)){
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
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        capabilities?.let {
            when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

}