package com.gaiazeye.businessscheduler

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<View>(R.id.backButton)?.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        val emailField = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordField = findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val signupRedirect = findViewById<TextView>(R.id.signupRedirect)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password) { success, error ->
                if (success) {
                    checkPaywallAndNavigate()
                } else {
                    Toast.makeText(this, "Login failed: $error", Toast.LENGTH_LONG).show()
                }
            }
        }

        signupRedirect.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser != null) {
            checkPaywallAndNavigate()
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
