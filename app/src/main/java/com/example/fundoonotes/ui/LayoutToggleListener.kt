package com.example.fundoonotes.ui

/**
 * Interface for handling layout toggle events between grid and list views.
 * Implemented by fragments that need to respond to layout changes.
 */
interface LayoutToggleListener {
    /**
     * Called when the layout toggle button is pressed
     * @param isGridLayout true if the layout is now grid, false if it's list
     */
    fun onLayoutToggle(isGridLayout: Boolean)
}