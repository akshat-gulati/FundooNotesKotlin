package com.example.fundoonotes.data.repository

import android.content.Context
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.model.Note
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreLabelRepository(private val context: Context): LabelRepository {
    private val db = Firebase.firestore
    private val _labelsState = MutableStateFlow<List<Label>>(emptyList())
    val labelsState: StateFlow<List<Label>> = _labelsState.asStateFlow()

    private val labelsListener: ListenerRegistration? = null

    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)")





    override fun fetchLabelById(
        labelId: String,
        onSuccess: (Label) -> Unit
    ) {
        db.collection("labels").document(labelId)
    }

    override fun fetchLabels() {
        TODO("Not yet implemented")
    }

    override fun addNewLabel(labelName: String) {
        TODO("Not yet implemented")
    }

    override fun updateLabel(labelId: String, labelName: String) {
        TODO("Not yet implemented")
    }

    override fun deleteLabel(labelId: String) {
        TODO("Not yet implemented")
    }

}