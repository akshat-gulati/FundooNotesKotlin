package com.example.fundoonotes

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            // Redirect to login activity
            val intent = Intent(this, loginSignupActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.main)
        val drawerButton: ImageButton = findViewById(R.id.drawer_button)

        drawerButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener { item: MenuItem ->
            // Update the current selected item ID
            currentNavItemId = item.itemId

            // Check/highlight the selected item
            item.isChecked = true

            when (item.itemId) {
                R.id.navNotes -> {
                    // Navigate to NotesFragment
                    val fragment = NoteFragment()
                    loadFragment(fragment)
                }
                R.id.navReminders -> {
                    // Navigate to RemindersFragment
                    val fragment = RemindersFragment()
                    loadFragment(fragment)
                }
                R.id.navLabels -> {
                    // Navigate to LabelsFragment
                    val fragment = LabelsFragment()
                    loadFragment(fragment)
                }
                R.id.navArchive -> {
                    // Navigate to ArchiveFragment
                    val fragment = ArchiveFragment()
                    loadFragment(fragment)
                }
                R.id.navBin -> {
                    // Navigate to BinFragment
                    val fragment = BinFragment()
                    loadFragment(fragment)
                }
                // Handle other menu items...
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Load default fragment if no fragment is loaded
        if (savedInstanceState == null) {
            // Set the default item as checked
            navView.menu.findItem(currentNavItemId).isChecked = true
            loadFragment(NoteFragment())
        }
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
        // Replace with your actual logic to check if the user is logged in
        // For example, check if a token exists in SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
}