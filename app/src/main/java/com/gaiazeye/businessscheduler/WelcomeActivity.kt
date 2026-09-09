package com.gaiazeye.businessscheduler

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already authenticated
        if (FirebaseAuth.getInstance().currentUser != null) {
            checkPaywallAndNavigate()
            return
        }

        setContentView(R.layout.activity_welcome)

        val btnGetStarted = findViewById<MaterialButton>(R.id.btnGetStarted)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun checkPaywallAndNavigate() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val sharedPrefs = getSharedPreferences("PaywallPrefs_${uid}", MODE_PRIVATE)
        val planName = sharedPrefs.getString("plan_name", null)

        if (planName != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            startActivity(Intent(this, PaywallActivity::class.java))
        }
        finish()
    }
}
