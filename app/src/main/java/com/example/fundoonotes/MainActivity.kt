package com.example.fundoonotes

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fundoonotes.ui.labels.LabelsFragment
import com.example.fundoonotes.ui.loginSignup.loginSignupActivity
import com.example.fundoonotes.ui.notes.NoteFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var navView: NavigationView
    private lateinit var titleText: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var layoutToggleIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var headerOptions: ImageView
    private lateinit var toolbar: Toolbar

    // State variables
    private var currentNavItemId: Int = R.id.navNotes // Default selected item
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
        } else {
            // Restore the current navigation item
            currentNavItemId = savedInstanceState.getInt("currentNavItemId", R.id.navNotes)
            // Update UI based on the restored navigation item
            updateUIForNavItem(currentNavItemId)
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

        profileIcon.setOnClickListener {
            logout()
        }

        headerOptions = findViewById(R.id.header_options)

        layoutToggleIcon.setOnClickListener {
            toggleLayout()
        }

        // Set initial icon for layout toggle
        layoutToggleIcon.setImageResource(
            if (isGridLayout) R.drawable.rectangle2x2 else R.drawable.rectangle1x2
        )
        toolbar = findViewById(R.id.toolbar)
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

    private fun handleNavigation(item: MenuItem): Boolean {
        if (currentNavItemId != item.itemId) {
            // Update the current selected item ID
            currentNavItemId = item.itemId

            // Check/highlight the selected item
            item.isChecked = true

            // Update UI based on the selected item
            updateUIForNavItem(currentNavItemId)
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.main)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun updateUIForNavItem(itemId: Int) {
        when (itemId) {
            R.id.navNotes -> navigateToNotes()
            R.id.navReminders -> navigateToReminders()
            R.id.navLabels -> navigateToLabels()
            R.id.navArchive -> navigateToArchive()
            R.id.navBin -> navigateToBin()
        }
    }

    private fun loadDefaultFragment() {
        // Set the default item as checked
        navView.menu.findItem(currentNavItemId).isChecked = true
        updateUIForNavItem(currentNavItemId)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun toggleLayout() {
        isGridLayout = !isGridLayout
        // Set the appropriate icon for layout toggle
        layoutToggleIcon.setImageResource(
            if (isGridLayout) R.drawable.rectangle2x2 else R.drawable.rectangle1x2
        )

        // Get the current fragment and notify it about the layout change
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is LayoutToggleListener) {
            currentFragment.onLayoutToggle(isGridLayout)
        }
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

    // Navigation methods - all using NoteFragment with different modes
// Add this at the beginning of each navigation method to clear the background
    private fun clearToolbarBackground() {
        toolbar.background = null
    }

// Then modify your navigation methods:

    private fun navigateToNotes() {
        // Set toolbar background back to default for Notes section
        toolbar.setBackgroundResource(R.drawable.toolbar_rounded) // Use your app's toolbar color resource

        val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_NOTES)
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
        clearToolbarBackground() // Clear the background
        val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_REMINDERS)
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
        clearToolbarBackground() // Clear the background
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
        clearToolbarBackground() // Clear the background
        val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_ARCHIVE)
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
        clearToolbarBackground() // Clear the background
        val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_BIN)
        titleText.text = getString(R.string.bin)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            options = false,
            search = false
        )
        loadFragment(fragment)
    }

    // Also update this method
    fun navigateToLabelNotes(label: String) {
        clearToolbarBackground() // Clear the background
        val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_LABELS, label)
        titleText.text = label
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            options = false,
            search = true
        )
        loadFragment(fragment)
    }

    // Authentication methods
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, loginSignupActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        redirectToLogin()
    }

    // Lifecycle methods
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current navigation item
        outState.putInt("currentNavItemId", currentNavItemId)
    }

    //Highlights Correct Navigation item
    override fun onResume() {
        super.onResume()
        // Make sure the correct navigation item is highlighted when returning to the activity
        navView.menu.findItem(currentNavItemId).isChecked = true
    }

    // Interface for fragment communication
    interface LayoutToggleListener {
        fun onLayoutToggle(isGridLayout: Boolean)
    }
}