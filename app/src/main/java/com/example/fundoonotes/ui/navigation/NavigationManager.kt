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
import com.example.fundoonotes.ui.LayoutToggleListener
import com.example.fundoonotes.ui.NavigationInterface
import com.example.fundoonotes.ui.labels.LabelsFragment
import com.example.fundoonotes.ui.notes.DisplayMode
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
    // ==============================================
    // Fragment Management
    // ==============================================
    private var noteFragment: NoteFragment = NoteFragment.newInstance(DisplayMode.NOTES)
    private var currentFragment: Fragment? = null

    // ==============================================
    // View References
    // ==============================================
    private val activity: MainActivity get() = navigationInterface.getContext() as MainActivity
    private val fragmentManager: FragmentManager get() = navigationInterface.getSupportFragmentManager()

    // ==============================================
    // State Variables
    // ==============================================
    private var currentNavItemId: Int = R.id.navNotes
    private var isGridLayout = true
    private var currentLabelName: String? = null
    private var isInSearchMode = false

    // ==============================================
    // Label Navigation Constants
    // ==============================================
    private val LABEL_MENU_GROUP_ID = 3
    private var menuLabelsGroup: Menu? = null

    // ==============================================
    // Data Bridge
    // ==============================================
    private lateinit var labelDataBridge: LabelDataBridge

    // ==============================================
    // Initialization Methods
    // ==============================================
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

    // ==============================================
    // Event Listeners
    // ==============================================
    private fun setupClickListeners() {
        layoutToggleIcon.setOnClickListener {
            toggleLayout()
        }

        searchIcon.setOnClickListener {
            toggleSearchMode(true)
        }

        drawerButton.setOnClickListener {
            if (isInSearchMode) {
                toggleSearchMode(false)
            } else {
                navigationInterface.openDrawer()
            }
        }
    }

    // ==============================================
    // Navigation Methods
    // ==============================================
    fun loadDefaultFragment() {
        navView.menu.findItem(currentNavItemId).isChecked = true
        updateUIForNavItem(currentNavItemId)
    }

    fun handleNavigation(item: MenuItem): Boolean {
        if (item.groupId == LABEL_MENU_GROUP_ID) {
            navigateToLabelNotes(item.title.toString())
            currentNavItemId = item.itemId
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

    // ==============================================
    // Fragment Loading Methods
    // ==============================================
    private fun loadFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        currentFragment = fragment
    }

    private fun navigateToNotes() {
        toolbar.setBackgroundResource(R.drawable.toolbar_rounded)
        titleText.text = activity.getString(R.string.notes)
        updateHeaderVisibility(true, true, true)

        if (currentFragment !is NoteFragment) {
            noteFragment = NoteFragment.newInstance(DisplayMode.NOTES)
            loadFragment(noteFragment)
        } else {
            (currentFragment as NoteFragment).updateDisplayMode(DisplayMode.NOTES)
        }
    }

    private fun navigateToReminders() {
        clearToolbarBackground()
        titleText.text = activity.getString(R.string.reminders)
        updateHeaderVisibility(true, false, true)

        if (currentFragment !is NoteFragment) {
            noteFragment = NoteFragment.newInstance(DisplayMode.REMINDERS)
            loadFragment(noteFragment)
        } else {
            (currentFragment as NoteFragment).updateDisplayMode(DisplayMode.REMINDERS)
        }
    }

    private fun navigateToLabels() {
        clearToolbarBackground()
        val fragment = LabelsFragment()
        titleText.text = activity.getString(R.string.createLabels)
        updateHeaderVisibility(false, false, false)
        loadFragment(fragment)
    }

    private fun navigateToArchive() {
        clearToolbarBackground()
        titleText.text = activity.getString(R.string.archive)
        updateHeaderVisibility(true, false, true)

        if (currentFragment !is NoteFragment) {
            noteFragment = NoteFragment.newInstance(DisplayMode.ARCHIVE)
            loadFragment(noteFragment)
        } else {
            (currentFragment as NoteFragment).updateDisplayMode(DisplayMode.ARCHIVE)
        }
    }

    private fun navigateToBin() {
        clearToolbarBackground()
        titleText.text = activity.getString(R.string.bin)
        updateHeaderVisibility(true, false, false)

        if (currentFragment !is NoteFragment) {
            noteFragment = NoteFragment.newInstance(DisplayMode.BIN)
            loadFragment(noteFragment)
        } else {
            (currentFragment as NoteFragment).updateDisplayMode(DisplayMode.BIN)
        }
    }

    private fun navigateToLabelNotes(labelName: String) {
        clearToolbarBackground()
        currentLabelName = labelName
        val label = labelDataBridge.labelsState.value.find { it.name == labelName }

        if (label != null) {
            titleText.text = labelName
            updateHeaderVisibility(true, false, true)

            if (currentFragment !is NoteFragment) {
                noteFragment = NoteFragment.newInstance(DisplayMode.LABELS)
                val labelId = label.id
                fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, noteFragment)
                    .runOnCommit {
                        noteFragment.updateDisplayMode(DisplayMode.LABELS, labelId)
                    }
                    .commit()
                currentFragment = noteFragment
            } else {
                (currentFragment as NoteFragment).updateDisplayMode(DisplayMode.LABELS, label.id)
            }
        }
    }

    // ==============================================
    // UI Update Methods
    // ==============================================
    private fun toggleLayout() {
        isGridLayout = !isGridLayout
        layoutToggleIcon.setImageResource(
            if (isGridLayout) R.drawable.rectangle2x2 else R.drawable.rectangle1x2
        )
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is LayoutToggleListener) {
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

    // ==============================================
    // Search Mode Methods
    // ==============================================
    fun toggleSearchMode(enable: Boolean) {
        isInSearchMode = enable
        titleText.visibility = if (enable) View.GONE else View.VISIBLE
        layoutToggleIcon.visibility = if (enable) View.GONE else View.VISIBLE
        profileIcon.visibility = if (enable) View.GONE else View.VISIBLE
        searchIcon.visibility = if (enable) View.GONE else View.VISIBLE
        etSearch.visibility = if (enable) View.VISIBLE else View.GONE

        if (enable) {
            drawerButton.setImageResource(R.drawable.arrow_back)
            etSearch.requestFocus()
        } else {
            drawerButton.setImageResource(R.drawable.menu)
            etSearch.setText("")
        }
    }

    fun handleSearchQuery(query: String) {
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is NoteFragment) {
            currentFragment.filterNotes(query)
        }
    }

    fun isInSearchMode(): Boolean {
        return isInSearchMode
    }

    // ==============================================
    // Label Menu Management
    // ==============================================
    fun updateLabelMenu(labels: List<Label>) {
        val menu = navView.menu
        val remindersMenuItem = menu.findItem(R.id.navReminders)

        if (menuLabelsGroup != null) {
            menuLabelsGroup?.clear()
        } else {
            menuLabelsGroup = menu.addSubMenu(Menu.NONE, Menu.NONE,
                remindersMenuItem.order + 1, "Labels")
        }

        labels.forEach { label ->
            menuLabelsGroup?.add(LABEL_MENU_GROUP_ID, View.generateViewId(), Menu.NONE, label.name)
                ?.setIcon(R.drawable.tag)
                ?.setCheckable(true)
        }
    }

    // ==============================================
    // State Management
    // ==============================================
    fun restoreState() {
        val menuItem = navView.menu.findItem(currentNavItemId)

        if (menuItem != null) {
            menuItem.isChecked = true
        } else if (currentLabelName != null) {
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

            if (!found) {
                currentNavItemId = R.id.navNotes
                navView.menu.findItem(currentNavItemId)?.isChecked = true
                currentLabelName = null
            }
        } else {
            currentNavItemId = R.id.navNotes
            navView.menu.findItem(currentNavItemId)?.isChecked = true
        }
    }

    fun saveState(outState: android.os.Bundle) {
        outState.putInt("currentNavItemId", currentNavItemId)
    }
}