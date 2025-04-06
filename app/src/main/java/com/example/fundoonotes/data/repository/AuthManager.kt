package com.example.fundoonotes.data.repository

import android.content.Context
import android.content.Intent
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.ui.loginSignup.LoginSignupActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthManager(private val context: Context?) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val sharedPreferences = context?.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    fun isUserLoggedIn(): Boolean {
        val userId = sharedPreferences?.getString("userId", null)
        val currentUser = firebaseAuth.currentUser

        return userId != null && currentUser != null && currentUser.uid == userId
    }

    fun logout() {
        firebaseAuth.signOut()
        sharedPreferences?.edit()?.remove("userId")?.apply()
        redirectToLogin()
    }

    fun redirectToLogin() {
        val intent = Intent(context, LoginSignupActivity::class.java)
        context?.startActivity(intent)
        (context as? MainActivity)?.finish()

    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun getUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

}