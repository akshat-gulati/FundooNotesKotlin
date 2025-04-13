package com.example.fundoonotes.ui.loginSignup

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fundoonotes.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fundoonotes.data.repository.CloudinaryImageManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.example.fundoonotes.data.repository.firebase.FirebaseAuthService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.Manifest
import com.example.fundoonotes.data.repository.firebase.FirestoreUserDataRepository

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
    private lateinit var ibProfilePicture: ImageView

    // Email and Password EditTexts
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var credentialManager: CredentialManager
    private lateinit var firebaseAuthService: FirebaseAuthService
    private lateinit var cloudinaryImageManager: CloudinaryImageManager

    private var profileImageUri: Uri? = null
    private var profileImageUrl: String? = null

    // Permission constants
    private val STORAGE_PERMISSION_CODE = 101

    // Activity result launchers
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profileImageUri = uri
                // Show selected image in the ImageButton
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.person)
                    .into(ibProfilePicture)

                // Upload to Cloudinary
                Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
                cloudinaryImageManager.uploadProfileImage(uri) { imageUrl ->
                    profileImageUrl = imageUrl
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_signup)
        firebaseAuthService = FirebaseAuthService(this)
        credentialManager = CredentialManager.create(this)
        cloudinaryImageManager = CloudinaryImageManager(this)
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
            showImageSelectionDialog()
        }
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("Choose from Gallery", "Cancel")

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkStoragePermissionAndOpenGallery()
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkStoragePermissionAndOpenGallery() {
        when {
            // For Android 13+ (API 33+), use READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) -> {
                        explainPermissionRationale("Storage", Manifest.permission.READ_MEDIA_IMAGES, STORAGE_PERMISSION_CODE)
                    }
                    else -> {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                            STORAGE_PERMISSION_CODE
                        )
                    }
                }
            }
            // For Android 12 and below, use READ_EXTERNAL_STORAGE
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) -> {
                        explainPermissionRationale("Storage", Manifest.permission.READ_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE)
                    }
                    else -> {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            STORAGE_PERMISSION_CODE
                        )
                    }
                }
            }
        }
    }

    private fun explainPermissionRationale(permissionType: String, permission: String, requestCode: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("$permissionType Permission Required")
            .setMessage("This app needs $permissionType access to handle profile pictures.")
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "$permissionType permission is required for this feature",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    // Check if permission was permanently denied
                    val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissionToCheck)) {
                        // Show dialog to open app settings
                        showSettingsDialog("Storage")
                    } else {
                        Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showSettingsDialog(permissionType: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("$permissionType Permission Required")
            .setMessage("$permissionType permission is needed but has been permanently denied. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
        ibProfilePicture = findViewById(R.id.ibProfilePicture)

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
                    val userRepository = FirestoreUserDataRepository(this@LoginSignupActivity)

                    // Add the user data including profile image URL
                    userRepository.addNewUser(fullName, email, profileImageUrl)

                    // Navigate to MainActivity
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