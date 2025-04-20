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
import kotlin.math.log

class AccountActionDialog : DialogFragment() {

    // UI Components
    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var cvManage: CardView
    private lateinit var cvLogout: CardView

    // Repositories
    private lateinit var authManager: AuthManager
    private lateinit var userRepository: FirestoreUserDataRepository
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Coroutine job
    private var userStateCollector: Job? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authManager = AuthManager(context)
        userRepository = FirestoreUserDataRepository(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_action_dialog, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUserInfo()
        setupClickListeners()
        collectUserData()
    }

    override fun onStart() {
        super.onStart()
        setupDialogWindow()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userStateCollector?.cancel()
    }

    private fun initializeViews(view: View) {
        ivProfile = view.findViewById(R.id.ivProfile)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        cvManage = view.findViewById(R.id.cvManage)
        cvLogout = view.findViewById(R.id.cvLogout)

        cvManage.visibility =GONE

    }

    private fun collectUserData() {
        userStateCollector = lifecycleScope.launch {
            userRepository.userState.collect { user ->
                user?.let {
                    // Set user info text
                    tvName.text = it.name
                    tvEmail.text = it.email

                    // Load profile image
                    Log.d("AccountActionDialog", "User from state: $it")
                    Log.d("AccountActionDialog", "Profile Image URL: ${it.profileImageUrl}")

                    context?.let { ctx ->
                        Glide.with(ctx)
                            .load(it.profileImageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.person)
                            .into(ivProfile)
                    }
                }
            }
        }
    }

    private fun setupDialogWindow() {
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
    private fun setupUserInfo() {
        userRepository.fetchUserData()
    }
    private fun setupClickListeners() {
        cvLogout.setOnClickListener {
            authManager.logout()
            dismiss()
        }
    }
}