package com.example.fundoonotes

import android.os.Bundle
import android.view.Menu
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.ui.notes.NoteFragment
import com.google.android.material.navigation.NavigationView
import com.example.fundoonotes.data.repository.AuthManager
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.ui.labels.LabelsFragment
import kotlinx.coroutines.launch
import androidx.core.view.size
import androidx.core.view.get
import com.example.fundoonotes.ui.accountActionDialog.AccountActionDialog


class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var navView: NavigationView
    private lateinit var titleText: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var layoutToggleIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var toolbar: Toolbar
    private lateinit var authManager: AuthManager
    // Add new property
    private lateinit var labelDataBridge: LabelDataBridge
    private var menuLabelsGroup: Menu? = null
    private val LABEL_MENU_GROUP_ID = 1001
    // State variables
    private var currentNavItemId: Int = R.id.navNotes
    private var isGridLayout = true
    private var currentLabelName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        // Check if user is logged in
        if (!authManager.isUserLoggedIn()) {
            authManager.redirectToLogin()
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
        observeLabels()

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

    private fun observeLabels() {
        labelDataBridge = LabelDataBridge(this)

        // Observe labels and update navigation dynamically
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelDataBridge.labelsState.collect { labels ->
                    updateLabelMenu(labels)
                }
            }
        }

        // Fetch labels initially
        labelDataBridge.fetchLabels()
    }

    private fun updateLabelMenu(labels: List<Label>) {
        // Get the menu from NavigationView
        val menu = navView.menu

        // Find the Reminders menu item
        val remindersMenuItem = menu.findItem(R.id.navReminders)

        // Clear existing label items if any
        if (menuLabelsGroup != null) {
            menuLabelsGroup?.clear()
        } else {
            // Create a new group for labels between group1 (which contains Reminders) and group2
            menuLabelsGroup = menu.addSubMenu(Menu.NONE, Menu.NONE,
                remindersMenuItem.order + 1, "Labels")
        }

        // Add each label as a menu item
        labels.forEach { label ->
            menuLabelsGroup?.add(LABEL_MENU_GROUP_ID, View.generateViewId(), Menu.NONE, label.name)
                ?.setIcon(R.drawable.tag)
                ?.setCheckable(true)
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
                val dialog = AccountActionDialog()
                dialog.show(supportFragmentManager, "AccountActionDialog")
            }

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
        if (item.groupId == LABEL_MENU_GROUP_ID) {
            // This is a label item, handle label navigation
            navigateToLabelNotes(item.title.toString())
            currentNavItemId = item.itemId // Store the current item ID
            item.isChecked = true
        } else if (currentNavItemId != item.itemId) {
            // Existing code for standard navigation items
            currentNavItemId = item.itemId
            item.isChecked = true
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
        // Set thw icon for layout toggle
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
        search: Boolean
    ) {
        layoutToggleIcon.visibility = if (layoutToggle) View.VISIBLE else View.GONE
        profileIcon.visibility = if (profile) View.VISIBLE else View.GONE
        searchIcon.visibility = if (search) View.VISIBLE else View.GONE
    }


    private fun clearToolbarBackground() {
        toolbar.background = null
    }


    private fun navigateToNotes() {

        toolbar.setBackgroundResource(R.drawable.toolbar_rounded)

        val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_NOTES)
        titleText.text = getString(R.string.notes)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = true,
            search = true
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
            search = false
        )
        loadFragment(fragment)
    }

    // Lifecycle methods
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentNavItemId", currentNavItemId)
    }


    // When navigating to label notes, store the label name
    private fun navigateToLabelNotes(labelName: String) {
        clearToolbarBackground()
        currentLabelName = labelName // Store the current label name

        // Find the label ID from the name
        val label = labelDataBridge.labelsState.value.find { it.name == labelName }

        if (label != null) {
            val fragment = NoteFragment.newInstance(NoteFragment.DISPLAY_LABELS, label.id)
            titleText.text = labelName
            updateHeaderVisibility(
                layoutToggle = true,
                profile = false,
                search = true
            )
            loadFragment(fragment)
        }
    }

    // Modify onResume to handle label menu items
    override fun onResume() {
        super.onResume()

        // First try to find the menu item directly
        val menuItem = navView.menu.findItem(currentNavItemId)

        if (menuItem != null) {
            menuItem.isChecked = true
        } else if (currentLabelName != null) {
            // If we were on a label, try to find it by name in the current menu
            var found = false
            if (menuLabelsGroup != null) {
                for (i in 0 until menuLabelsGroup!!.size) {
                    val item = menuLabelsGroup!![i]
                    if (item.title.toString() == currentLabelName) {
                        item.isChecked = true
                        currentNavItemId = item.itemId
                        found = true
                        break
                    }
                }
            }

            // If we couldn't find the label, reset to default
            if (!found) {
                currentNavItemId = R.id.navNotes
                navView.menu.findItem(currentNavItemId)?.isChecked = true
                currentLabelName = null
            }
        } else {
            // If all else fails, reset to Notes
            currentNavItemId = R.id.navNotes
            navView.menu.findItem(currentNavItemId)?.isChecked = true
        }
    }

    // Interface for fragment communication
    interface LayoutToggleListener {
        fun onLayoutToggle(isGridLayout: Boolean)
    }


//   A communication channel between NoteFragment and MainActivity to control the toolbar visibility.
    fun setToolbarVisibility(isVisible: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}