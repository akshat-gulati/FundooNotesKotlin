package com.example.fundoonotes

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
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener { item: MenuItem ->
            val itemId: Int = item.itemId

            when (itemId) {
                R.id.navNotes -> {
                    Toast.makeText(this, "Notes selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navReminders -> {
                    Toast.makeText(this, "Reminders selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navLabels -> {
                    Toast.makeText(this, "Create/Edit labels selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navArchive -> {
                    Toast.makeText(this, "Archive selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navBin -> {
                    Toast.makeText(this, "Bin selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navSettings -> {
                    Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navFeedback -> {
                    Toast.makeText(this, "Send app feedback selected", Toast.LENGTH_SHORT).show()
                }
                R.id.navHelp -> {
                    Toast.makeText(this, "Help selected", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}