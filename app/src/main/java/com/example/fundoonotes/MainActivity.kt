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

    private lateinit var title_text: TextView
    private lateinit var search_icon: ImageView
    private lateinit var layout_toggle_icon: ImageView
    private lateinit var profile_icon: ImageView
    private lateinit var header_options: ImageView


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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_view)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = systemBars.left) // Ensures drawer respects notch in landscape
            insets
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.main)
        val drawerButton: ImageButton = findViewById(R.id.drawer_button)
        title_text = findViewById(R.id.tvHeaderTitle)
        layout_toggle_icon = findViewById(R.id.layout_toggle_icon)
        search_icon = findViewById(R.id.search_icon)
        profile_icon = findViewById(R.id.profile_icon)
        header_options = findViewById(R.id.header_options)
        search_icon.visibility = View.GONE
        header_options.visibility = View.GONE


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
                    title_text.text = getString(R.string.notes)

                    layout_toggle_icon.visibility = View.VISIBLE
                    profile_icon.visibility = View.VISIBLE
                    header_options.visibility = View.GONE
                    search_icon.visibility = View.GONE

                    loadFragment(fragment)
                }
                R.id.navReminders -> {
                    // Navigate to RemindersFragment
                    val fragment = RemindersFragment()
                    title_text.text = getString(R.string.reminders)
                    layout_toggle_icon.visibility = View.VISIBLE
                    profile_icon.visibility = View.GONE
                    header_options.visibility = View.GONE
                    search_icon.visibility = View.VISIBLE
                    loadFragment(fragment)
                }
                R.id.navLabels -> {
                    // Navigate to LabelsFragment
                    val fragment = LabelsFragment()
//                    layout_toggle_icon.visibility = View.GONE
                    layout_toggle_icon.visibility = View.GONE
                    profile_icon.visibility = View.GONE
                    header_options.visibility = View.GONE
                    search_icon.visibility = View.GONE
                    title_text.text = getString(R.string.create_labels)
                    loadFragment(fragment)
                }
                R.id.navArchive -> {
                    // Navigate to ArchiveFragment
                    val fragment = ArchiveFragment()
                    layout_toggle_icon.visibility = View.VISIBLE
                    profile_icon.visibility = View.GONE
                    header_options.visibility = View.GONE
                    search_icon.visibility = View.VISIBLE
                    title_text.text = getString(R.string.archive)
                    loadFragment(fragment)
                }
                R.id.navBin -> {
                    // Navigate to BinFragment
                    val fragment = BinFragment()
                    title_text.text = getString(R.string.bin)
                    layout_toggle_icon.visibility = View.VISIBLE
                    profile_icon.visibility = View.GONE
                    header_options.visibility = View.GONE
                    search_icon.visibility = View.GONE
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