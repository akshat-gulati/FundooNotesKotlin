package com.example.fundoonotes.ui.loginSignup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
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

class loginSignupActivity : AppCompatActivity() {

    // UI components
    private lateinit var tabLayout: TabLayout
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var cbAcceptTerms: CheckBox
    private lateinit var tvAcceptTerms: TextView
    private lateinit var clPasswordOptions: ConstraintLayout
    private lateinit var btnAction: Button
    private lateinit var tvLoginHeader: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_signup)

        initializeViews()
        setupTabLayout()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnAction.setOnClickListener {
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

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tabLayout)
        tilFullName = findViewById(R.id.tilFullName)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        cbAcceptTerms = findViewById(R.id.cbAcceptTerms)
        tvAcceptTerms = findViewById(R.id.tvAcceptTerms)
        clPasswordOptions = findViewById(R.id.clPasswordOptions)
        btnAction = findViewById(R.id.btnAction)
        tvLoginHeader = findViewById(R.id.tvLoginHeader)
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

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not needed as of now
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not needed as of now
            }
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
}