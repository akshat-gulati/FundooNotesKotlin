package com.example.fundoonotes.ui.loginSignup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fundoonotes.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.example.fundoonotes.data.repository.firebase.FirebaseAuthService

class LoginSignupActivity : AppCompatActivity() {

    // UI components
    private lateinit var tabLayout: TabLayout
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var clPasswordOptions: ConstraintLayout
    private lateinit var btnAction: Button
    private lateinit var tvLoginHeader: TextView
    private lateinit var ivGoogle: ImageView
    private lateinit var cvProfilePicture: CardView

    // Email and Password EditTexts
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var credentialManager: CredentialManager
    private lateinit var firebaseAuthService: FirebaseAuthService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_signup)
        firebaseAuthService = FirebaseAuthService(this)
        credentialManager = CredentialManager.create(this)
        initializeViews()
        setupTabLayout()

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

        cvProfilePicture.setOnClickListener{
            selectProfilePicture()
        }
    }


    private fun performGoogleSignIn() {
        lifecycleScope.launch {
            when (val result = firebaseAuthService.performGoogleSignIn()) {
                is FirebaseAuthService.AuthResult.Success -> {
                    firebaseAuthService.navigateToMainActivity(this@LoginSignupActivity)
                    finish()
                }

                is FirebaseAuthService.AuthResult.Error -> {
                    Toast.makeText(this@LoginSignupActivity, result.message, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        tilFullName = findViewById(R.id.tilFullName)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        clPasswordOptions = findViewById(R.id.clPasswordOptions)
        btnAction = findViewById(R.id.btnAction)
        tvLoginHeader = findViewById(R.id.tvLoginHeader)
        ivGoogle = findViewById(R.id.ivGoogle)
        cvProfilePicture = findViewById(R.id.cvProfilePicture)

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
        cvProfilePicture.visibility = View.GONE  // Hide profile picture upload in login mode

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
        cvProfilePicture.visibility = View.VISIBLE  // Show profile picture upload in register mode

        // Hide login-specific elements
        clPasswordOptions.visibility = View.GONE

        // Update action button
        btnAction.text = getString(R.string.register)
        tvLoginHeader.text = getString(R.string.register)
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        lifecycleScope.launch {
            when (val result = firebaseAuthService.loginWithEmailPassword(email, password)) {
                is FirebaseAuthService.AuthResult.Success -> {
                    firebaseAuthService.navigateToMainActivity(this@LoginSignupActivity)
                    finish()
                }

                is FirebaseAuthService.AuthResult.Error -> {
                    Toast.makeText(this@LoginSignupActivity, result.message, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()

        lifecycleScope.launch {
            when (val result = firebaseAuthService.registerWithEmailPassword(
                email, password, confirmPassword, fullName
            )) {
                is FirebaseAuthService.AuthResult.Success -> {
                    firebaseAuthService.navigateToMainActivity(this@LoginSignupActivity)
                    finish()
                }

                is FirebaseAuthService.AuthResult.Error -> {
                    Toast.makeText(this@LoginSignupActivity, result.message, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}

private fun LoginSignupActivity.selectProfilePicture() {
    Toast.makeText(baseContext, "Upload feature will be implemented soon", Toast.LENGTH_SHORT).show()
}
