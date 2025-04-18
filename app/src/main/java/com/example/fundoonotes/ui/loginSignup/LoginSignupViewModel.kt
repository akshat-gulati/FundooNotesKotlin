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

    // Dependencies
    private val firebaseAuthService: FirebaseAuthService = FirebaseAuthService(context)
    private val cloudinaryImageManager: CloudinaryImageManager = CloudinaryImageManager(context)
    private val permissionManager: PermissionManager = PermissionManager(context)
    private val firestoreUserDataRepository: FirestoreUserDataRepository = FirestoreUserDataRepository(context)

    // LiveData for UI state
    private val _currentTab = MutableLiveData<Int>(0) // 0 for Login, 1 for Register
    val currentTab: LiveData<Int> = _currentTab

    private val _profileImageUri = MutableLiveData<Uri?>(null)
    val profileImageUri: LiveData<Uri?> = _profileImageUri

    private val _profileImageUrl = MutableLiveData<String?>(null)
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Sealed class for authentication results
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    // Authentication Methods
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

    fun register(email: String, password: String, confirmPassword: String, fullName: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = firebaseAuthService.registerWithEmailPassword(
                email, password, confirmPassword, fullName
            )

            when (result) {
                is FirebaseAuthService.AuthResult.Success -> {
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

    // Profile Image Management
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

    // Permission Management
    fun hasStoragePermission(activity: LoginSignupActivity): Boolean {
        return permissionManager.checkStoragePermission(activity)
    }

    // Navigation
    fun navigateToMainActivity(activity: LoginSignupActivity) {
        firebaseAuthService.navigateToMainActivity(activity)
    }
}