package com.example.fundoonotes.ui.navigation

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.example.fundoonotes.data.repository.dataBridge.LabelDataBridge
import com.example.fundoonotes.ui.NavigationInterface
import com.google.android.material.navigation.NavigationView

class NavigationCoordinator(
    private val navigationInterface: NavigationInterface,
    private val labelDataBridge: LabelDataBridge,
    private val toolbar: Toolbar,
    private val titleText: TextView,
    private val layoutToggleIcon: ImageView,
    private val searchIcon: ImageView,
    private val profileIcon: ImageView,
    private val drawerButton: ImageButton,
    private val etSearch: EditText,
    private val navView: NavigationView
) {
    private lateinit var navigationManager: NavigationManager

    fun initialize() {
        navigationManager = NavigationManager(
            navigationInterface = navigationInterface,
            toolbar = toolbar,
            titleText = titleText,
            layoutToggleIcon = layoutToggleIcon,
            searchIcon = searchIcon,
            profileIcon = profileIcon,
            drawerButton = drawerButton,
            etSearch = etSearch,
            navView = navView
        )
        navigationManager.initialize(labelDataBridge)
    }

    fun getNavigationManager(): NavigationManager = navigationManager

    fun onSaveInstanceState(outState: Bundle) {
        navigationManager.saveState(outState)
    }

    fun onRestoreInstanceState() {
        navigationManager.restoreState()
    }
}