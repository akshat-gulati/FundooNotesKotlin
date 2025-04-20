package com.example.fundoonotes.data.repository.firebase

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.repository.interfaces.LabelInterface
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreLabelRepository(private val context: Context): LabelInterface {

    // ==============================================
    // Dependencies and Initialization
    // ==============================================
    private val db = Firebase.firestore
    private val _labelsState = MutableStateFlow<List<Label>>(emptyList())
    val labelsState: StateFlow<List<Label>> = _labelsState.asStateFlow()
    private var labelsListener: ListenerRegistration? = null
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private val userId = sharedPreferences.getString("userId", null)

    // ==============================================
    // Label Fetching Operations
    // ==============================================
    override fun fetchLabelById(labelId: String, onSuccess: (Label) -> Unit) {
        db.collection("labels").document(labelId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Label::class.java)?.copy(id = document.id)?.let(onSuccess)
                    ?: Log.d("LabelRepository", "No such document with ID: $labelId")
            }
            .addOnFailureListener { e ->
                Log.w("LabelRepository", "Error getting label", e)
            }
    }

    override fun fetchLabels() {
        if (userId == null) {
            Log.e("LabelRepository", "No user ID found")
            return
        }

        labelsListener?.remove()
        labelsListener = db.collection("labels")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("LabelRepository", "Listen failed.", error)
                    return@addSnapshotListener
                }
                snapshot?.documents?.let { documents ->
                    _labelsState.value = documents.map { doc ->
                        doc.toObject(Label::class.java)?.copy(id = doc.id) ?: Label()
                    }
                    Log.d("LabelRepository", "Real-time update received: ${documents.size} labels")
                }
            }
    }

    // ==============================================
    // Label CRUD Operations
    // ==============================================
    override fun addNewLabel(labelId: String, labelName: String): String {
        if (userId == null) {
            Log.e("LabelRepository", "No user ID found")
            return ""
        }

        val labelRef = db.collection("labels").document(labelId)
        val label = Label(
            id = labelRef.id,
            userId = userId,
            name = labelName,
            noteIds = emptyList()
        )

        labelRef.set(label)
            .addOnSuccessListener {
                Log.d("LabelRepository", "Label added successfully: ${label.id}")
            }
            .addOnFailureListener { e ->
                Log.e("LabelRepository", "Error adding label", e)
            }
        return labelRef.id
    }

    override fun updateLabel(labelId: String, labelName: String, noteIds: List<String>) {
        db.collection("labels").document(labelId)
            .update(mapOf("name" to labelName, "noteIds" to noteIds))
            .addOnSuccessListener {
                Log.d("LabelRepository", "Label updated successfully: $labelId")
            }
            .addOnFailureListener { e ->
                Log.w("LabelRepository", "Error updating label", e)
            }
    }

    override fun deleteLabel(labelId: String) {
        db.collection("labels").document(labelId)
            .delete()
            .addOnSuccessListener {
                Log.d("LabelRepository", "Label deleted successfully: $labelId")
            }
            .addOnFailureListener { e ->
                Log.w("LabelRepository", "Error deleting label", e)
            }
    }

    // ==============================================
    // Utility Methods
    // ==============================================
    fun fetchLabelsByIds(labelIds: List<String>, onSuccess: (List<Label>) -> Unit) {
        db.collection("labels")
            .whereIn("id", labelIds)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { it.toObject(Label::class.java) })
            }
            .addOnFailureListener { e ->
                Log.w("LabelRepository", "Error getting labels", e)
            }
    }
}