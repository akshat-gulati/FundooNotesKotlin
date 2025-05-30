package com.example.fundoonotes.ui.loginSignup

import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fundoonotes.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginSignupActivity : AppCompatActivity() {

    // ==============================================
    // ViewModel
    // ==============================================
    private lateinit var viewModel: LoginSignupViewModel

    // ==============================================
    // UI Components Declaration
    // ==============================================
    private lateinit var tabLayout: TabLayout
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var btnAction: Button
    private lateinit var tvLoginHeader: TextView
    private lateinit var ivGoogle: ImageView
    private lateinit var cvProfilePicture: CardView
    private lateinit var ibProfilePicture: ImageView

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    // ==============================================
    // Activity Result Launchers
    // ==============================================
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setProfileImageUri(uri)
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.person)
                    .into(ibProfilePicture)
                Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==============================================
    // Activity Lifecycle Methods
    // ==============================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_signup)

        viewModel = LoginSignupViewModel(this)

        initializeViews()
        setupTabLayout()
        setupClickListeners()
        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (viewModel.hasStoragePermission(this)) {
            openGallery()
        }
    }

    // ==============================================
    // Initialization Methods
    // ==============================================
    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        tilFullName = findViewById(R.id.tilFullName)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        btnAction = findViewById(R.id.btnAction)
        tvLoginHeader = findViewById(R.id.tvLoginHeader)
        ivGoogle = findViewById(R.id.ivGoogle)
        cvProfilePicture = findViewById(R.id.cvProfilePicture)
        ibProfilePicture = findViewById(R.id.ibProfilePicture)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etFullName = findViewById(R.id.etFullName)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
    }

    private fun setupTabLayout() {
        showLoginUI()
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

    // ==============================================
    // UI State Management
    // ==============================================
    private fun showLoginUI() {
        tilFullName.visibility = View.GONE
        tilConfirmPassword.visibility = View.GONE
        cvProfilePicture.visibility = View.GONE
        btnAction.text = getString(R.string.login)
        tvLoginHeader.text = getString(R.string.login)
    }

    private fun showRegisterUI() {
        tilFullName.visibility = View.VISIBLE
        tilConfirmPassword.visibility = View.VISIBLE
        cvProfilePicture.visibility = View.VISIBLE
        btnAction.text = getString(R.string.register)
        tvLoginHeader.text = getString(R.string.register)
    }

    // ==============================================
    // Click Listeners Setup
    // ==============================================
    private fun setupClickListeners() {
        btnAction.setOnClickListener {
            when (tabLayout.selectedTabPosition) {
                0 -> loginUser()
                1 -> registerUser()
            }
        }

        ivGoogle.setOnClickListener {
            viewModel.performGoogleSignIn()
        }

        cvProfilePicture.setOnClickListener {
            showImageSelectionDialog()
        }
    }

    // ==============================================
    // Authentication Methods
    // ==============================================
    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        viewModel.login(email, password)
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        viewModel.register(email, password, confirmPassword, fullName)
    }

    // ==============================================
    // Image Handling Methods
    // ==============================================
    private fun showImageSelectionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Profile Picture")
            .setItems(arrayOf("Choose from Gallery", "Cancel")) { dialog, which ->
                when (which) {
                    0 -> if (viewModel.hasStoragePermission(this)) openGallery()
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    // ==============================================
    // ViewModel Observation
    // ==============================================
    private fun observeViewModel() {

        lifecycleScope.launch {
            viewModel.authSuccess.collectLatest { success ->
                if (success == true) {
                    viewModel.navigateToMainActivity(this@LoginSignupActivity)
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.authError.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@LoginSignupActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                btnAction.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.profileImageUri.collectLatest { uri ->
                uri?.let {
                    Glide.with(this@LoginSignupActivity)
                        .load(it)
                        .circleCrop()
                        .placeholder(R.drawable.person)
                        .into(ibProfilePicture)
                }
            }
        }
    }
}