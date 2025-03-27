package com.example.fundoonotes.ui.loginSignup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginSignupActivity : AppCompatActivity() {

    // UI components
    private lateinit var tabLayout: TabLayout
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var cbAcceptTerms: CheckBox
    private lateinit var tvAcceptTerms: TextView
    private lateinit var clPasswordOptions: ConstraintLayout
    private lateinit var btnAction: Button
    private lateinit var tvLoginHeader: TextView
    private lateinit var ivGoogle: ImageView

    // Email and Password EditTexts
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var request: GetCredentialRequest

    companion object {
        private const val TAG = "LoginSignupActivity"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_signup)
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)
        initializeViews()
        setupTabLayout()
        setupGoogleSignIn()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnAction.setOnClickListener {
            // Determine action based on current tab
            val currentTab = tabLayout.selectedTabPosition
            if (currentTab == 0) {
                // Login
                loginUser()
            } else {
                // Register
                registerUser()
            }
        }
        ivGoogle.setOnClickListener {
            performGoogleSignIn()
        }
    }

    private fun setupGoogleSignIn() {
        // Instantiate a Google sign-in request
        val googleIdOption = GetGoogleIdOption.Builder()
            // Your server's client ID, not your Android client ID.
            .setServerClientId(getString(R.string.default_web_client_id))
            // Only show accounts previously used to sign in.
            .setFilterByAuthorizedAccounts(true)
            .build()

        // Create the Credential Manager request
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }
    private fun performGoogleSignIn() {
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginSignupActivity
                )
                handleSignIn(result.credential)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                Toast.makeText(this@LoginSignupActivity, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Google Authentication failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        tilFullName = findViewById(R.id.tilFullName)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        cbAcceptTerms = findViewById(R.id.cbAcceptTerms)
        tvAcceptTerms = findViewById(R.id.tvAcceptTerms)
        clPasswordOptions = findViewById(R.id.clPasswordOptions)
        btnAction = findViewById(R.id.btnAction)
        tvLoginHeader = findViewById(R.id.tvLoginHeader)
        ivGoogle = findViewById(R.id.ivGoogle)

        // Initialize EditTexts
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etFullName = findViewById(R.id.etFullName)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
    }

    private fun setupTabLayout() {
        // Set initial state to Login
        showLoginUI()

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showLoginUI()
                    1 -> showRegisterUI()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showLoginUI() {
        // Show login-specific UI elements
        tilFullName.visibility = View.GONE
        tilConfirmPassword.visibility = View.GONE
        cbAcceptTerms.visibility = View.GONE
        tvAcceptTerms.visibility = View.GONE

        // Show login-specific elements
        clPasswordOptions.visibility = View.GONE

        // Update action button
        btnAction.text = getString(R.string.login)
        tvLoginHeader.text = getString(R.string.login)
    }

    private fun showRegisterUI() {
        // Show register-specific UI elements
        tilFullName.visibility = View.VISIBLE
        tilConfirmPassword.visibility = View.VISIBLE
        cbAcceptTerms.visibility = View.VISIBLE
        tvAcceptTerms.visibility = View.VISIBLE

        // Hide login-specific elements
        clPasswordOptions.visibility = View.GONE

        // Update action button
        btnAction.text = getString(R.string.register)
        tvLoginHeader.text = getString(R.string.register)
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, displayying a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()

        // Basic validation
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }



        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Save login state
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            sharedPreferences.edit() {
                putBoolean("isLoggedIn", true)
            }

            // Start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close loginSignupActivity
        }
    }
}