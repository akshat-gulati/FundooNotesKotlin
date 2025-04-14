package com.example.fundoonotes.ui

import android.content.Context
import androidx.fragment.app.FragmentManager

interface NavigationInterface {
    fun openDrawer()
    fun closeDrawer()
    fun setToolbarVisibility(isVisible: Boolean)
    fun getContext(): Context
    fun getSupportFragmentManager(): FragmentManager
}