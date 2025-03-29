package com.example.fundoonotes.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.core.content.edit

class FirebaseAuthService(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    // Sealed class to represent authentication results
    sealed class AuthResult {
        data class Success(val user: FirebaseUser) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    companion object {
        private const val TAG = "FirebaseAuthService"
    }

    suspend fun loginWithEmailPassword(email: String, password: String): AuthResult {
        return try {
            // Input validation
            if (email.isEmpty() || password.isEmpty()) {
                return AuthResult.Error("Please enter email and password")
            }

            // Attempt login
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.let {
                saveLoginState(it)
                AuthResult.Success(it)
            } ?: AuthResult.Error("Authentication failed")

        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Login failed")
        }
    }

    suspend fun registerWithEmailPassword(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String
    ): AuthResult {
        return try {
            // Input validation
            when {
                email.isEmpty() || password.isEmpty() || fullName.isEmpty() ->
                    return AuthResult.Error("Please fill in all fields")
                password != confirmPassword ->
                    return AuthResult.Error("Passwords do not match")
            }

            // Attempt registration
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            authResult.user?.let {
                saveLoginState(it)

                // Add this line to save user data to Firestore
                val userRepo = FirestoreUserDataRepository(context)
                userRepo.addNewUser(fullName, email)

                AuthResult.Success(it)
            } ?: AuthResult.Error("Registration failed")

        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    suspend fun performGoogleSignIn(): AuthResult {
        return try {
            // Setup Google Sign-In
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credentials
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Handle sign-in
            handleSignIn(result.credential)

        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            AuthResult.Error("Google Sign-In failed: ${e.localizedMessage}")
        }
    }

    private suspend fun handleSignIn(credential: Credential): AuthResult {
        return try {
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } else {
                AuthResult.Error("Invalid credential type")
            }
        } catch (e: Exception) {
            AuthResult.Error("Sign-in failed: ${e.localizedMessage}")
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            authResult.user?.let {
                saveLoginState(it)
                AuthResult.Success(it)
            } ?: AuthResult.Error("Google Authentication failed")
        } catch (e: Exception) {
            AuthResult.Error("Google Authentication failed: ${e.localizedMessage}")
        }
    }

    fun saveLoginState(user: FirebaseUser) {
        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit() { putString("userId", user.uid) }
    }

    fun navigateToMainActivity(currentContext: Context) {
        val intent = Intent(currentContext, MainActivity::class.java)
        currentContext.startActivity(intent)
    }
}