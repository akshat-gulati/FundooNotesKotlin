package com.example.fundoonotes

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.ui.archive.ArchiveFragment
import com.example.fundoonotes.ui.bin.BinFragment
import com.example.fundoonotes.ui.labels.LabelsFragment
import com.example.fundoonotes.ui.loginSignup.loginSignupActivity
import com.example.fundoonotes.ui.notes.NoteFragment
import com.example.fundoonotes.ui.reminders.RemindersFragment
import com.google.android.material.navigation.NavigationView



class MainActivity : AppCompatActivity() {

    private lateinit var navView: NavigationView
    private var currentNavItemId: Int = R.id.navNotes // Default selected item

    private lateinit var titleText: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var layoutToggleIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var headerOptions: ImageView
    private var isGridLayout = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            redirectToLogin()
            return
        }

        setupUI(savedInstanceState)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, loginSignupActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun setupUI(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupWindowInsets()
        initializeViews()
        setupDrawer()
        setupNavigation()

        // Load default fragment if no fragment is loaded
        if (savedInstanceState == null) {
            loadDefaultFragment()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_view)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = systemBars.left) // Ensures drawer respects notch in landscape
            insets
        }
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.tvHeaderTitle)
        layoutToggleIcon = findViewById(R.id.layout_toggle_icon)
        searchIcon = findViewById(R.id.search_icon)
        profileIcon = findViewById(R.id.profile_icon)
        headerOptions = findViewById(R.id.header_options)

        layoutToggleIcon.setOnClickListener {
            toggleLayout()
        }

        // Set initial visibility
        searchIcon.visibility = View.GONE
        headerOptions.visibility = View.GONE
    }

    private fun toggleLayout() {
        isGridLayout = !isGridLayout
        // Set the appropriate icon for layout toggle
        layoutToggleIcon.setImageResource(
            if (isGridLayout) R.drawable.rectangle1x2 else R.drawable.rectangle2x2
        )

        // Get the current fragment and notify it about the layout change
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is LayoutToggleListener) {
            currentFragment.onLayoutToggle(isGridLayout)
        }
    }

    private fun setupDrawer() {
        val drawerLayout: DrawerLayout = findViewById(R.id.main)
        val drawerButton: ImageButton = findViewById(R.id.drawer_button)

        drawerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigation() {
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener { item: MenuItem ->
            handleNavigation(item)
        }
    }

    private fun loadDefaultFragment() {
        // Set the default item as checked
        navView.menu.findItem(currentNavItemId).isChecked = true
        loadFragment(NoteFragment())
    }

    private fun handleNavigation(item: MenuItem): Boolean {
        // Update the current selected item ID
        currentNavItemId = item.itemId

        // Check/highlight the selected item
        item.isChecked = true

        when (item.itemId) {
            R.id.navNotes -> {
                navigateToNotes()
            }
            R.id.navReminders -> {
                navigateToReminders()
            }
            R.id.navLabels -> {
                navigateToLabels()
            }
            R.id.navArchive -> {
                navigateToArchive()
            }
            R.id.navBin -> {
                navigateToBin()
            }
            // Handle other menu items...
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.main)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun navigateToNotes() {
        val fragment = NoteFragment()
        titleText.text = getString(R.string.notes)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = true,
            options = false,
            search = false
        )
        loadFragment(fragment)
    }

    private fun navigateToReminders() {
        val fragment = RemindersFragment()
        titleText.text = getString(R.string.reminders)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            options = false,
            search = true
        )
        loadFragment(fragment)
    }

    private fun navigateToLabels() {
        val fragment = LabelsFragment()
        titleText.text = getString(R.string.create_labels)
        updateHeaderVisibility(
            layoutToggle = false,
            profile = false,
            options = false,
            search = false
        )
        loadFragment(fragment)
    }

    private fun navigateToArchive() {
        val fragment = ArchiveFragment()
        titleText.text = getString(R.string.archive)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            options = false,
            search = true
        )
        loadFragment(fragment)
    }

    private fun navigateToBin() {
        val fragment = BinFragment()
        titleText.text = getString(R.string.bin)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            options = false,
            search = false
        )
        loadFragment(fragment)
    }

    private fun updateHeaderVisibility(
        layoutToggle: Boolean,
        profile: Boolean,
        options: Boolean,
        search: Boolean
    ) {
        layoutToggleIcon.visibility = if (layoutToggle) View.VISIBLE else View.GONE
        profileIcon.visibility = if (profile) View.VISIBLE else View.GONE
        headerOptions.visibility = if (options) View.VISIBLE else View.GONE
        searchIcon.visibility = if (search) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Make sure the correct navigation item is highlighted when returning to the activity
        navView.menu.findItem(currentNavItemId).isChecked = true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun isUserLoggedIn(): Boolean {
        // Logic to check if the user is logged in
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
    
    interface LayoutToggleListener {
        fun onLayoutToggle(isGridLayout: Boolean)

    }
}