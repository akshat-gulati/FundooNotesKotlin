package com.example.fundoonotes

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import com.example.fundoonotes.core.PermissionManager
import com.google.android.material.navigation.NavigationView
import com.example.fundoonotes.core.AuthManager
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.data.repository.dataBridge.NotesDataBridge
import com.example.fundoonotes.ui.NavigationInterface
import com.example.fundoonotes.ui.accountActionDialog.AccountActionDialog
import com.example.fundoonotes.ui.navigation.NavigationComponent
import com.example.fundoonotes.ui.navigation.NavigationManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationInterface {

    // UI components
    private lateinit var navView: NavigationView
    private lateinit var titleText: TextView
    private lateinit var searchIcon: ImageView
    private lateinit var layoutToggleIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var toolbar: Toolbar
    private lateinit var authManager: AuthManager
    private lateinit var etSearch: EditText
    private lateinit var drawerButton: ImageButton
    private lateinit var drawerLayout: DrawerLayout

    // Navigation manager
    private lateinit var navigationManager: NavigationManager
    private lateinit var navigationComponent: NavigationComponent
    private lateinit var permissionManager: PermissionManager

    // Data bridges
    private lateinit var labelDataBridge: LabelDataBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        // Check if user is logged in
        if (!authManager.isUserLoggedIn()) {
            authManager.redirectToLogin()
            return
        }
        setupUI(savedInstanceState)

        val notesDataBridge = NotesDataBridge(applicationContext)
        notesDataBridge.initializeDatabase()

        initializeWorkManager()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@MainActivity, "Back pressed", Toast.LENGTH_SHORT).show()
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

    }
    private fun initializeWorkManager() {
        // Initialize WorkManager if it hasn't been initialized yet
        try {
            WorkManager.getInstance(applicationContext)
        } catch (e: IllegalStateException) {
            // WorkManager not initialized yet, so initialize it
            val config = androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build()

            WorkManager.initialize(applicationContext, config)
            Log.d("MainActivity", "WorkManager initialized")
        }
    }

    private fun setupUI(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupWindowInsets()
        initializeViews()
        setupDrawer()
        setupNavigation()

        // Initialize data bridges
        labelDataBridge = LabelDataBridge(this)
        permissionManager = PermissionManager(this)
        permissionManager.checkNotificationPermission(this)

        navigationComponent = NavigationComponent(
            navigationInterface = this,  // Changed from navigationContract to match the class definition
            labelDataBridge = labelDataBridge,
            toolbar = toolbar,
            titleText = titleText,
            layoutToggleIcon = layoutToggleIcon,
            searchIcon = searchIcon,
            profileIcon = profileIcon,
            drawerButton = drawerButton,
            etSearch = etSearch,
            navView = navView
        )
        navigationComponent.initialize()
        navigationManager = navigationComponent.getNavigationManager()

        // Observe labels for navigation
        observeLabels()

        // Load default fragment if no fragment is loaded
        if (savedInstanceState == null) {
            navigationComponent.getNavigationManager().loadDefaultFragment()
        } else {
            navigationComponent.onRestoreInstanceState()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionManager.handlePermissionResult(
            this,
            requestCode,
            permissions,
            grantResults
        ) { code ->
            // You can add specific handling if needed
            Log.d(javaClass.simpleName.toString(), "Permission granted: $code")
        }
    }

    private fun observeLabels() {
        // Observe labels and update navigation dynamically
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                labelDataBridge.labelsState.collect { labels ->
                    navigationComponent.getNavigationManager().updateLabelMenu(labels)
                }
            }
        }

        // Fetch labels initially
        labelDataBridge.fetchLabels()
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
        searchIcon = findViewById(R.id.searchIcon)
        profileIcon = findViewById(R.id.profile_icon)
        etSearch = findViewById(R.id.etSearch)
        drawerButton = findViewById(R.id.drawer_button)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.main)

        // Setup profile click listener
        profileIcon.setOnClickListener {
            val dialog = AccountActionDialog()
            dialog.show(supportFragmentManager, "AccountActionDialog")
        }

        // Setup search text change listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                navigationManager.handleSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupDrawer() {
        drawerButton.setOnClickListener {
            if (navigationManager.isInSearchMode()) {
                // Act as back button in search mode
                navigationManager.toggleSearchMode(false)
            } else {
                // Act as drawer button normally
                openDrawer()
            }
        }
    }

    override fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupNavigation() {
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener { item: MenuItem ->
            val result = navigationManager.handleNavigation(item)
            closeDrawer()
            result
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (navigationManager.isInSearchMode()) {
            // Exit search mode if back is pressed while searching
            navigationManager.toggleSearchMode(false)
            dismissKeyboardShortcutsHelper()
            Toast.makeText(this@MainActivity, "Back Pressed", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressedDispatcher
        }
    }

    //Hide keyboard (Need to be checked - copied from stack overflow 😬)
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    // Lifecycle methods
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navigationComponent.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        navigationManager.restoreState()
    }

    // A communication channel between NoteFragment and MainActivity to control the toolbar visibility.
    override fun setToolbarVisibility(isVisible: Boolean) {
        toolbar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun getContext(): Context = this

    override fun getSupportFragmentManager(): FragmentManager = super.getSupportFragmentManager()
}