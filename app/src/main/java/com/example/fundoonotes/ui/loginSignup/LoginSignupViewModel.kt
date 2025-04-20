package com.example.fundoonotes.ui.loginSignup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.core.PermissionManager
import com.example.fundoonotes.data.repository.CloudinaryImageManager
import com.example.fundoonotes.data.repository.firebase.FirebaseAuthService
import com.example.fundoonotes.data.repository.firebase.FirestoreUserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginSignupViewModel(context: Context) : ViewModel() {

    // ==============================================
    // Dependencies
    // ==============================================
    private val firebaseAuthService: FirebaseAuthService = FirebaseAuthService(context)
    private val cloudinaryImageManager: CloudinaryImageManager = CloudinaryImageManager(context)
    private val permissionManager: PermissionManager = PermissionManager(context)
    private val firestoreUserDataRepository: FirestoreUserDataRepository = FirestoreUserDataRepository(context)

    // ==============================================
    // StateFlow Declarations
    // ==============================================
    private val _currentTab = MutableStateFlow<Int>(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _profileImageUri = MutableStateFlow<Uri?>(null)
    val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val _authSuccess = MutableStateFlow<Boolean?>(null)
    val authSuccess: StateFlow<Boolean?> = _authSuccess.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ==============================================
    // Authentication Methods
    // ==============================================
    fun login(email: String, password: String) {
        _isLoading.value = true
        clearAuthState()

        viewModelScope.launch {
            when (val result = firebaseAuthService.loginWithEmailPassword(email, password)) {
                is FirebaseAuthService.AuthResult.Success -> {
                    _authSuccess.value = true
                }
                is FirebaseAuthService.AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun register(email: String, password: String, confirmPassword: String, fullName: String) {
        _isLoading.value = true
        clearAuthState()

        viewModelScope.launch {
            when (val result = firebaseAuthService.registerWithEmailPassword(
                email, password, confirmPassword, fullName
            )) {
                is FirebaseAuthService.AuthResult.Success -> {
                    firestoreUserDataRepository.addNewUser(fullName, email, profileImageUrl.value)
                    _authSuccess.value = true
                }
                is FirebaseAuthService.AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun performGoogleSignIn() {
        _isLoading.value = true
        clearAuthState()

        viewModelScope.launch {
            when (val result = firebaseAuthService.performGoogleSignIn()) {
                is FirebaseAuthService.AuthResult.Success -> {
                    _authSuccess.value = true
                }
                is FirebaseAuthService.AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    // ==============================================
    // Profile Image Management
    // ==============================================
    fun setProfileImageUri(uri: Uri?) {
        _profileImageUri.value = uri
        uploadProfileImage(uri)
    }

    private fun uploadProfileImage(uri: Uri?) {
        uri?.let {
            _isLoading.value = true
            cloudinaryImageManager.uploadProfileImage(it) { imageUrl ->
                _profileImageUrl.value = imageUrl
                _isLoading.value = false
            }
        }
    }

    // ==============================================
    // Permission Management
    // ==============================================
    fun hasStoragePermission(activity: LoginSignupActivity): Boolean {
        return permissionManager.checkStoragePermission(activity)
    }

    // ==============================================
    // Navigation
    // ==============================================
    fun navigateToMainActivity(activity: LoginSignupActivity) {
        firebaseAuthService.navigateToMainActivity(activity)
    }

    // ==============================================
    // Helper Methods
    // ==============================================
    private fun clearAuthState() {
        _authSuccess.value = null
        _authError.value = null
    }
}