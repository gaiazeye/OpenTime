package com.gaiazeye.businessscheduler

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R
import java.text.SimpleDateFormat
import java.util.*

class PaywallActivity : AppCompatActivity() {

    private var selectedPlan: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_subscription_paywall)

        val monthlyCard = findViewById<MaterialCardView>(R.id.monthlyCard)
        val annualCard = findViewById<MaterialCardView>(R.id.annualCard)
        val subscribeButton = findViewById<MaterialButton>(R.id.subscribeButton)
        val freeTrialButton = findViewById<MaterialButton>(R.id.freeTrialButton)

        monthlyCard.setOnClickListener {
            monthlyCard.isChecked = true
            annualCard.isChecked = false
            selectedPlan = "Monthly Plan"
        }

        annualCard.setOnClickListener {
            annualCard.isChecked = true
            monthlyCard.isChecked = false
            selectedPlan = "Annual Plan"
        }

        subscribeButton.setOnClickListener {
            if (selectedPlan == null) {
                Toast.makeText(this, "Please select a plan first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveSubscription(selectedPlan!!, "Premium")
            navigateToDashboard()
        }

        freeTrialButton.setOnClickListener {
            saveSubscription("30-Day Free Trial", "Trial")
            navigateToDashboard()
        }
    }

    private fun saveSubscription(planName: String, planType: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val sharedPrefs = getSharedPreferences("PaywallPrefs_${uid}", MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        
        if (planName == "30-Day Free Trial") {
            calendar.add(Calendar.DAY_OF_YEAR, 30)
        } else if (planName == "Monthly Plan") {
            calendar.add(Calendar.MONTH, 1)
        } else {
            calendar.add(Calendar.YEAR, 1)
        }

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val billingDate = sdf.format(calendar.time)

        sharedPrefs.edit().apply {
            putString("plan_name", planName)
            putString("plan_type", planType)
            putString("billing_date", billingDate)
            putLong("subscription_start_date", System.currentTimeMillis())
            apply()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}
