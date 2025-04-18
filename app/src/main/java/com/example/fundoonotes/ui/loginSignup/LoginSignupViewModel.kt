package com.example.fundoonotes.ui.loginSignup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fundoonotes.core.PermissionManager
import com.example.fundoonotes.data.repository.CloudinaryImageManager
import com.example.fundoonotes.data.repository.firebase.FirebaseAuthService
import com.example.fundoonotes.data.repository.firebase.FirestoreUserDataRepository
import kotlinx.coroutines.launch

class LoginSignupViewModel(context: Context) : ViewModel() {

    private var firebaseAuthService: FirebaseAuthService = FirebaseAuthService(context)
    private var cloudinaryImageManager:CloudinaryImageManager = CloudinaryImageManager(context)
    private var permissionManager: PermissionManager = PermissionManager(context)
    private var firestoreUserDataRepository:FirestoreUserDataRepository = FirestoreUserDataRepository(context)


    private val _currentTab = MutableLiveData<Int>(0) // 0 for Login, 1 for Register
    val currentTab: LiveData<Int> = _currentTab

    // Profile image data
    private val _profileImageUri = MutableLiveData<Uri?>(null)
    val profileImageUri: LiveData<Uri?> = _profileImageUri

    private val _profileImageUrl = MutableLiveData<String?>(null)
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    // Auth result state
    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Function to switch between login and register tabs
    fun setCurrentTab(tabPosition: Int) {
        _currentTab.value = tabPosition
    }

    // Function to set the profile image Uri
    fun setProfileImageUri(uri: Uri?) {
        _profileImageUri.value = uri
        uploadProfileImage(uri)
    }

    // Upload profile image to Cloudinary
    private fun uploadProfileImage(uri: Uri?) {
        uri?.let {
            _isLoading.value = true
            cloudinaryImageManager.uploadProfileImage(it) { imageUrl ->
                _profileImageUrl.value = imageUrl
                _isLoading.value = false
            }
        }
    }

    // Login function
    fun login(email: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = firebaseAuthService.loginWithEmailPassword(email, password)
            _authResult.value = when (result) {
                is FirebaseAuthService.AuthResult.Success -> AuthResult.Success
                is FirebaseAuthService.AuthResult.Error -> AuthResult.Error(result.message)
            }
            _isLoading.value = false
        }
    }

    // Register function
    fun register(email: String, password: String, confirmPassword: String, fullName: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = firebaseAuthService.registerWithEmailPassword(
                email, password, confirmPassword, fullName
            )

            when (result) {
                is FirebaseAuthService.AuthResult.Success -> {
                    // Store user data in Firestore
                    firestoreUserDataRepository.addNewUser(fullName, email, _profileImageUrl.value)
                    _authResult.value = AuthResult.Success
                }
                is FirebaseAuthService.AuthResult.Error -> {
                    _authResult.value = AuthResult.Error(result.message)
                }
            }
            _isLoading.value = false
        }
    }

    // Google sign-in function
    fun performGoogleSignIn() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = firebaseAuthService.performGoogleSignIn()
            _authResult.value = when (result) {
                is FirebaseAuthService.AuthResult.Success -> AuthResult.Success
                is FirebaseAuthService.AuthResult.Error -> AuthResult.Error(result.message)
            }
            _isLoading.value = false
        }
    }

    // Check if storage permission is granted
    fun hasStoragePermission(activity: LoginSignupActivity): Boolean {
        return permissionManager.checkStoragePermission(activity)
    }

    // Navigation function
    fun navigateToMainActivity(activity: LoginSignupActivity) {
        firebaseAuthService.navigateToMainActivity(activity)
    }

    // Sealed class for authentication results
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}