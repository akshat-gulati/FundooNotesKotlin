package com.example.fundoonotes.data.repository.interfaces

import android.content.Context
import androidx.credentials.Credential
import com.example.fundoonotes.data.repository.firebase.FirebaseAuthService.AuthResult
import com.google.firebase.auth.FirebaseUser

interface AuthInterface {
    suspend fun loginWithEmailPassword(email: String, password: String): AuthResult
    suspend fun registerWithEmailPassword( email: String, password: String, confirmPassword: String, fullName: String): AuthResult
    suspend fun performGoogleSignIn(): AuthResult
    suspend fun handleSignIn(credential: Credential): AuthResult
    suspend fun firebaseAuthWithGoogle(idToken: String): AuthResult
    fun saveLoginState(user: FirebaseUser)
    fun navigateToMainActivity(currentContext: Context)
}