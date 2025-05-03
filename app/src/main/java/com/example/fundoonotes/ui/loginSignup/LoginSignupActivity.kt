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
import com.example.fundoonotes.databinding.ActivityLoginSignupBinding
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
    private val viewModel by lazy { LoginSignupViewModel(this) }
    private val binding by lazy { ActivityLoginSignupBinding.inflate(layoutInflater) }

    // ==============================================
    // UI Components Declaration
    // ==============================================
    private val tabLayout by lazy { binding.tabLayout }


    private val tilFullName: TextInputLayout by lazy { binding.tilFullName }
    private val tilConfirmPassword: TextInputLayout by lazy { binding.tilConfirmPassword }
    private val btnAction: Button by lazy { binding.btnAction }
    private val tvLoginHeader: TextView by lazy { binding.tvLoginHeader }
    private val ivGoogle: ImageView by lazy { binding.ivGoogle }
    private val cvProfilePicture: CardView by lazy { binding.cvProfilePicture }
    private val ibProfilePicture: ImageView by lazy { binding.ibProfilePicture }

    private val etEmail: TextInputEditText by lazy { binding.etEmail }
    private val etPassword: TextInputEditText by lazy { binding.etPassword }
    private val etFullName: TextInputEditText by lazy { binding.etFullName }
    private val etConfirmPassword: TextInputEditText by lazy { binding.etConfirmPassword }

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
        setContentView(binding.root)
        setupTabLayout()
        setupClickListeners()
        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
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