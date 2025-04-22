package com.example.fundoonotes.ui.accountActionDialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.circleCrop
import com.example.fundoonotes.R
import com.example.fundoonotes.core.AuthManager
import com.example.fundoonotes.data.repository.firebase.FirestoreUserDataRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountActionDialog : DialogFragment() {

    // ==============================================
    // UI Components
    // ==============================================
    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var cvManage: CardView
    private lateinit var cvLogout: CardView

    // ==============================================
    // Data Repositories & Services
    // ==============================================
    private lateinit var authManager: AuthManager
    private lateinit var userRepository: FirestoreUserDataRepository

    // ==============================================
    // Coroutine Management
    // ==============================================
    private var userStateCollector: Job? = null

    // ==============================================
    // Lifecycle Methods
    // ==============================================
    override fun onAttach(context: Context) {
        super.onAttach(context)
        initializeServices(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_action_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupUIComponents()
    }

    override fun onStart() {
        super.onStart()
        configureDialogWindow()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanupResources()
    }

    // ==============================================
    // Initialization Methods
    // ==============================================
    private fun initializeServices(context: Context) {
        authManager = AuthManager(context)
        userRepository = FirestoreUserDataRepository(context)
    }

    private fun initializeViews(view: View) {
        ivProfile = view.findViewById(R.id.ivProfile)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        cvManage = view.findViewById(R.id.cvManage)
        cvLogout = view.findViewById(R.id.cvLogout)
        cvManage.visibility = GONE
    }

    // ==============================================
    // UI Setup Methods
    // ==============================================
    private fun setupUIComponents() {
        setupUserInfo()
        setupClickListeners()
        collectUserData()
    }

    private fun configureDialogWindow() {
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.80).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.TOP)
            attributes?.apply {
                y = 50 // Distance from the top in pixels
            }
        }
    }

    // ==============================================
    // Data Handling Methods
    // ==============================================
    private fun setupUserInfo() {
        userRepository.fetchUserData()
    }

    private fun collectUserData() {
        userStateCollector = lifecycleScope.launch {
            userRepository.userState.collect { user ->
                user?.let {
                    updateUserInfoUI(it)
                }
            }
        }
    }

    private fun updateUserInfoUI(user: com.example.fundoonotes.data.model.User) {
        // Set user info text
        tvName.text = user.name
        tvEmail.text = user.email

        // Load profile image
        Log.d("AccountActionDialog", "User from state: $user")
        Log.d("AccountActionDialog", "Profile Image URL: ${user.profileImageUrl}")

        context?.let { ctx ->
            Glide.with(ctx)
                .load(user.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.person)
                .into(ivProfile)
        }
    }

    // ==============================================
    // Event Handlers
    // ==============================================
    private fun setupClickListeners() {
        cvLogout.setOnClickListener {
            handleLogout()
        }
    }

    private fun handleLogout() {
        authManager.logout()
        dismiss()
    }

    // ==============================================
    // Cleanup Methods
    // ==============================================
    private fun cleanupResources() {
        userStateCollector?.cancel()
    }
}