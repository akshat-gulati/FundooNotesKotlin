package com.example.fundoonotes.data.repository

import android.content.Context
import android.util.Log
import com.example.fundoonotes.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreUserDataRepository(private val context: Context) {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val _userDataState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userDataState.asStateFlow()

    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    init {
        fetchUserData()
    }

    fun fetchUserData() {
        val userId = getUserId() ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                _userDataState.value = user
            }
            .addOnFailureListener { e ->
                Log.w("UserRepository", "Error fetching user data", e)
            }
    }

    fun addNewUser(name: String, email: String, profileImageUrl: String? = null): String {
        val userId = auth.currentUser?.uid ?: return ""

        val user = User(
            id = userId,
            name = name,
            email = email,
            profileImageUrl = profileImageUrl
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d("UserRepository", "User added successfully: $userId")
                fetchUserData()
            }
            .addOnFailureListener { e ->
                Log.e("UserRepository", "Error adding user", e)
            }

        return userId
    }

    fun updateUser(userId: String, name: String, email: String, profileImageUrl: String?) {
        val updatedUser = mutableMapOf<String, Any>(
            "name" to name,
            "email" to email
        )

        // Only add profileImageUrl if it's not null
        profileImageUrl?.let {
            updatedUser["profileImageUrl"] = it
        }

        db.collection("users").document(userId)
            .update(updatedUser)
            .addOnSuccessListener {
                fetchUserData()
            }
            .addOnFailureListener { e ->
                Log.w("UserRepository", "Error updating user", e)
            }
    }

    fun getUserById(userId: String): User? {
        return if (_userDataState.value?.id == userId) _userDataState.value else null
    }

    private fun getUserId(): String? {
        val userId = sharedPreferences.getString("userId", null)
        Log.d("UserRepository", "Retrieved User ID: $userId")
        return userId
    }
}
