package com.gaiazeye.businessscheduler

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R
import java.util.Calendar
import java.util.Locale

class SignupActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        findViewById<View>(R.id.backButton)?.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        val fullNameField = findViewById<TextInputEditText>(R.id.fullNameEditText)
        val businessNameField = findViewById<TextInputEditText>(R.id.businessNameEditText)
        val birthdateField = findViewById<TextInputEditText>(R.id.birthdateEditText)
        val emailField = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordField = findViewById<TextInputEditText>(R.id.passwordEditText)
        val signupButton = findViewById<MaterialButton>(R.id.signupButton)

        val calendar = Calendar.getInstance()
        birthdateField.setOnClickListener {
            val datePicker = DatePickerDialog(this, { _, year, month, day ->
                val selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", month + 1, day, year)
                birthdateField.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        signupButton.setOnClickListener {
            val fullName = fullNameField.text.toString().trim()
            val businessName = businessNameField.text.toString().trim()
            val birthdate = birthdateField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (fullName.isEmpty() || businessName.isEmpty() || birthdate.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.signup(email, password) { success, error ->
                if (success) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
                    val prefs = getSharedPreferences("UserProfilePrefs_${uid}", MODE_PRIVATE)
                    prefs.edit()
                        .putString("full_name", fullName)
                        .putString("business_name", businessName)
                        .putString("birthdate", birthdate)
                        .apply()

                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                    
                    // Always route new signup to PaywallActivity to configure subscription
                    val intent = Intent(this, PaywallActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
