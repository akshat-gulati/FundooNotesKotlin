package com.example.fundoonotes.core

import android.content.Context
import android.content.Intent
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.ui.loginSignup.LoginSignupActivity
import com.google.firebase.auth.FirebaseAuth

class AuthManager(private val context: Context) {

    // ==============================================
    // Dependencies and Initialization
    // ==============================================
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val notesDataBridge = NotesDataBridge(context)
    private val labelDataBridge = LabelDataBridge(context)
    private val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    // ==============================================
    // Authentication State Management
    // ==============================================
    fun isUserLoggedIn(): Boolean {
        val userId = sharedPreferences?.getString("userId", null)
        val currentUser = firebaseAuth.currentUser
        return userId != null && currentUser != null && currentUser.uid == userId
    }

    // ==============================================
    // Authentication Actions
    // ==============================================
    fun logout() {
        notesDataBridge.cleanRoomDB()
        labelDataBridge.cleanRoomDB()
        firebaseAuth.signOut()
        sharedPreferences?.edit()?.remove("userId")?.apply()
        redirectToLogin()
    }

    // ==============================================
    // Navigation Methods
    // ==============================================
    fun redirectToLogin() {
        val intent = Intent(context, LoginSignupActivity::class.java)
        context?.startActivity(intent)
        (context as? MainActivity)?.finish()
    }
}