package com.gaiazeye.businessscheduler

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R

class subscription_status : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subscription_status, container, false)
        
        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        val planNameTv = view.findViewById<TextView>(R.id.planName)
        val planTypeTv = view.findViewById<TextView>(R.id.planType)
        val renewalDateTv = view.findViewById<TextView>(R.id.renewalDate)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val sharedPrefs = requireContext().getSharedPreferences("PaywallPrefs_${uid}", Context.MODE_PRIVATE)
        val planName = sharedPrefs.getString("plan_name", "Basic Plan")
        val planType = sharedPrefs.getString("plan_type", "Free")
        val billingDate = sharedPrefs.getString("billing_date", "N/A")

        planNameTv.text = "Plan: $planName"
        planTypeTv.text = "Type: $planType"
        renewalDateTv.text = "Billing Date: $billingDate"

        return view
    }
}
