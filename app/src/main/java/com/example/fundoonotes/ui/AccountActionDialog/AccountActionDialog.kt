package com.example.fundoonotes.ui.AccountActionDialog

import android.content.Context.MODE_PRIVATE
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
import com.example.fundoonotes.R
import com.google.firebase.auth.FirebaseAuth

class AccountActionDialog : DialogFragment() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var cvManage: CardView
    private lateinit var cvLogout: CardView

    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account_action_dialog, container, false)
        initializeViews(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        // Set transparent background for the dialog window
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.80).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setGravity(Gravity.TOP)

        val windowParams = dialog?.window?.attributes
        windowParams?.y = 50 // Distance from the top in pixels
        dialog?.window?.attributes = windowParams
    }

    private fun initializeViews(view: View) {
        ivProfile = view.findViewById(R.id.ivProfile)
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        cvManage = view.findViewById(R.id.cvManage)
        cvLogout = view.findViewById(R.id.cvLogout)
    }

}