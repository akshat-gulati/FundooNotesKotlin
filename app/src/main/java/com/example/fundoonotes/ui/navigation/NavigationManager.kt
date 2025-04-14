package com.example.fundoonotes.ui.navigation

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fundoonotes.MainActivity
import com.example.fundoonotes.R
import com.example.fundoonotes.data.model.Label
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.ui.NavigationInterface
import com.example.fundoonotes.ui.labels.LabelsFragment
import com.example.fundoonotes.ui.notes.NoteFragment
import com.google.android.material.navigation.NavigationView

class NavigationManager(
    private val navigationInterface: NavigationInterface,
    private val toolbar: androidx.appcompat.widget.Toolbar,
    private val titleText: TextView,
    private val layoutToggleIcon: ImageView,
    private val searchIcon: ImageView,
    private val profileIcon: ImageView,
    private val drawerButton: ImageButton,
    private val etSearch: EditText,
    private val navView: NavigationView
) {
    // Fragment management
    private var noteFragment: NoteFragment = NoteFragment.newInstance(NoteFragment.DISPLAY_NOTES)
    private var labelsFragment: LabelsFragment = LabelsFragment()
    private var currentFragment: Fragment? = null

    private val activity: MainActivity get() = navigationInterface.getContext() as MainActivity
    private val fragmentManager: FragmentManager get() = navigationInterface.getSupportFragmentManager()

    // State variables
    private var currentNavItemId: Int = R.id.navNotes
    private var isGridLayout = true
    private var currentLabelName: String? = null
    private var isInSearchMode = false

    // Label navigation constants
    private val LABEL_MENU_GROUP_ID = 3
    private var menuLabelsGroup: Menu? = null

    // Initialize LabelDataBridge
    private lateinit var labelDataBridge: LabelDataBridge

    fun initialize(labelDataBridge: LabelDataBridge) {
        this.labelDataBridge = labelDataBridge
        setupInitialState()
    }

    fun setupInitialState() {
        layoutToggleIcon.setImageResource(
            if (isGridLayout) R.drawable.rectangle2x2 else R.drawable.rectangle1x2
        )
        setupClickListeners()
    }

    private fun setupClickListeners() {
        layoutToggleIcon.setOnClickListener {
            toggleLayout()
        }

        searchIcon.setOnClickListener {
            toggleSearchMode(true)
        }

        drawerButton.setOnClickListener {
            if (isInSearchMode) {
                // Act as back button in search mode
                toggleSearchMode(false)
            } else {
                // Act as drawer button normally
                navigationInterface.openDrawer()
            }
        }
    }

    fun loadDefaultFragment() {
        // Set the default item as checked
        navView.menu.findItem(currentNavItemId).isChecked = true
        updateUIForNavItem(currentNavItemId)
    }

    fun handleNavigation(item: MenuItem): Boolean {
        if (item.groupId == LABEL_MENU_GROUP_ID) {
            // This is a label item, handle label navigation
            navigateToLabelNotes(item.title.toString())
            currentNavItemId = item.itemId // Store the current item ID
            item.isChecked = true
        } else if (currentNavItemId != item.itemId) {
            currentNavItemId = item.itemId
            item.isChecked = true
            updateUIForNavItem(currentNavItemId)
        }

        return true
    }

    fun updateUIForNavItem(itemId: Int) {
        when (itemId) {
            R.id.navNotes -> navigateToNotes()
            R.id.navReminders -> navigateToReminders()
            R.id.navLabels -> navigateToLabels()
            R.id.navArchive -> navigateToArchive()
            R.id.navBin -> navigateToBin()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        currentFragment = fragment
    }

    private fun toggleLayout() {
        isGridLayout = !isGridLayout
        layoutToggleIcon.setImageResource(
            if (isGridLayout) R.drawable.rectangle2x2 else R.drawable.rectangle1x2
        )
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is MainActivity.LayoutToggleListener) {
            currentFragment.onLayoutToggle(isGridLayout)
        }
    }

    private fun updateHeaderVisibility(layoutToggle: Boolean, profile: Boolean, search: Boolean) {
        layoutToggleIcon.visibility = if (layoutToggle) View.VISIBLE else View.GONE
        profileIcon.visibility = if (profile) View.VISIBLE else View.GONE
        searchIcon.visibility = if (search) View.VISIBLE else View.GONE
    }

    private fun clearToolbarBackground() {
        toolbar.background = null
    }

    private fun navigateToNotes() {
        toolbar.setBackgroundResource(R.drawable.toolbar_rounded)
        titleText.text = activity.getString(R.string.notes)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = true,
            search = true
        )

        if (currentFragment !is NoteFragment) {
            // If we're coming from a different fragment type, create and load a new NoteFragment
            noteFragment = NoteFragment.newInstance(NoteFragment.DISPLAY_NOTES)
            loadFragment(noteFragment)
        } else {
            // If we're already on a NoteFragment, just update its display mode
            (currentFragment as NoteFragment).updateDisplayMode(NoteFragment.DISPLAY_NOTES)
        }
    }

    private fun navigateToReminders() {
        clearToolbarBackground() // Clear the background
        titleText.text = activity.getString(R.string.reminders)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            search = true
        )

        // Check if NoteFragment is already loaded
        if (currentFragment !is NoteFragment) {
            // Only create a new fragment if we don't already have a NoteFragment
            noteFragment = NoteFragment.newInstance(NoteFragment.DISPLAY_REMINDERS)
            loadFragment(noteFragment)
        } else {
            // Just update the existing fragment's display mode
            (currentFragment as NoteFragment).updateDisplayMode(NoteFragment.DISPLAY_REMINDERS)
        }
    }

    private fun navigateToLabels() {
        clearToolbarBackground() // Clear the background
        val fragment = LabelsFragment()
        titleText.text = activity.getString(R.string.create_labels)
        updateHeaderVisibility(
            layoutToggle = false,
            profile = false,
            search = false
        )
        loadFragment(fragment)
    }

    private fun navigateToArchive() {
        clearToolbarBackground() // Clear the background
        titleText.text = activity.getString(R.string.archive)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            search = true
        )

        // Check if NoteFragment is already loaded
        if (currentFragment !is NoteFragment) {
            // Only create a new fragment if we don't already have a NoteFragment
            noteFragment = NoteFragment.newInstance(NoteFragment.DISPLAY_ARCHIVE)
            loadFragment(noteFragment)
        } else {
            // Just update the existing fragment's display mode
            (currentFragment as NoteFragment).updateDisplayMode(NoteFragment.DISPLAY_ARCHIVE)
        }
    }

    private fun navigateToBin() {
        clearToolbarBackground() // Clear the background
        titleText.text = activity.getString(R.string.bin)
        updateHeaderVisibility(
            layoutToggle = true,
            profile = false,
            search = false
        )

        // Check if NoteFragment is already loaded
        if (currentFragment !is NoteFragment) {
            // Only create a new fragment if we don't already have a NoteFragment
            noteFragment = NoteFragment.newInstance(NoteFragment.DISPLAY_BIN)
            loadFragment(noteFragment)
        } else {
            // Just update the existing fragment's display mode
            (currentFragment as NoteFragment).updateDisplayMode(NoteFragment.DISPLAY_BIN)
        }
    }

    private fun navigateToLabelNotes(labelName: String) {
        clearToolbarBackground()
        currentLabelName = labelName // Store the current label name

        // Find the label ID from the name
        val label = labelDataBridge.labelsState.value.find { it.name == labelName }

        if (label != null) {
            titleText.text = labelName
            updateHeaderVisibility(
                layoutToggle = true,
                profile = false,
                search = true
            )

            // Check if NoteFragment is already loaded
            if (currentFragment !is NoteFragment) {
                // Only create a new fragment if we don't already have a NoteFragment
                noteFragment = NoteFragment.newInstance(NoteFragment.DISPLAY_LABELS)
                loadFragment(noteFragment)
            }

            // Update the display mode with the label ID
            (currentFragment as NoteFragment).updateDisplayMode(NoteFragment.DISPLAY_LABELS, label.id)
        }
    }

    fun toggleSearchMode(enable: Boolean) {
        isInSearchMode = enable

        // Toggle visibility of UI elements
        titleText.visibility = if (enable) View.GONE else View.VISIBLE
        layoutToggleIcon.visibility = if (enable) View.GONE else View.VISIBLE
        profileIcon.visibility = if (enable) View.GONE else View.VISIBLE
        searchIcon.visibility = if (enable) View.GONE else View.VISIBLE
        etSearch.visibility = if (enable) View.VISIBLE else View.GONE

        // Change drawer button to back button
        if (enable) {
            drawerButton.setImageResource(R.drawable.arrow_back)
            etSearch.requestFocus()
        } else {
            drawerButton.setImageResource(R.drawable.menu)
            etSearch.setText("") // Clear search text when exiting search mode
        }
    }

    fun handleSearchQuery(query: String) {
        // Get the current fragment
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)

        // If it's a NoteFragment, pass the search query
        if (currentFragment is NoteFragment) {
            currentFragment.filterNotes(query)
        }
    }

    fun updateLabelMenu(labels: List<Label>) {
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

    fun restoreState() {
        // First try to find the menu item directly
        val menuItem = navView.menu.findItem(currentNavItemId)

        if (menuItem != null) {
            menuItem.isChecked = true
        } else if (currentLabelName != null) {
            // If we were on a label, try to find it by name in the current menu
            var found = false
            if (menuLabelsGroup != null) {
                for (i in 0 until menuLabelsGroup!!.size()) {
                    val item = menuLabelsGroup!!.getItem(i)
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

    fun saveState(outState: android.os.Bundle) {
        outState.putInt("currentNavItemId", currentNavItemId)
    }

    fun isInSearchMode(): Boolean {
        return isInSearchMode
    }

    fun getCurrentNavItemId(): Int {
        return currentNavItemId
    }

    fun setCurrentNavItemId(id: Int) {
        currentNavItemId = id
    }
}