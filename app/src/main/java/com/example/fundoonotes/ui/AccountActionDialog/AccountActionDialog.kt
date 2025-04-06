package com.example.fundoonotes.ui.AccountActionDialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fundoonotes.R
import com.example.fundoonotes.data.repository.AuthManager
import com.example.fundoonotes.data.repository.firebase.FirestoreUserDataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountActionDialog : DialogFragment() {

    // UI Components
    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var cvManage: CardView
    private lateinit var cvLogout: CardView
    private lateinit var authManager: AuthManager

    // Repositories
    private lateinit var userRepository: FirestoreUserDataRepository

    // Coroutine job
    private var userStateCollector: Job? = null

    override fun onAttach(context: android.content.Context) {
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

    private fun setupUserInfo() { userRepository.fetchUserData() }

    private fun collectUserData() {
        userStateCollector = lifecycleScope.launch {
            userRepository.userState.collect { user ->
                user?.let {
                    tvName.text = it.name
                    tvEmail.text = it.email
                }
            }
        }
    }

    private fun setupClickListeners() {
        cvLogout.setOnClickListener {
            authManager.logout()
            dismiss()
        }

        // cvManage click listener is missing - is this intentional?
    }
}