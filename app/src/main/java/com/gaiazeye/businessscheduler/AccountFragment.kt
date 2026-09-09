package com.gaiazeye.businessscheduler

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.UserProfileChangeRequest
import com.gaiazeye.businessscheduler.R

class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val uid = user?.uid ?: "guest"

        val userNameTv = view.findViewById<TextView>(R.id.userNameTv)
        val userBusinessTv = view.findViewById<TextView>(R.id.userBusinessTv)
        val userEmailTv = view.findViewById<TextView>(R.id.userEmailTv)
        val userBirthdateTv = view.findViewById<TextView>(R.id.userBirthdateTv)

        val fullNameEdit = view.findViewById<TextInputEditText>(R.id.fullNameEditText)
        val businessNameEdit = view.findViewById<TextInputEditText>(R.id.businessNameEditText)
        val btnUpdateProfile = view.findViewById<MaterialButton>(R.id.btnUpdateProfile)

        val newEmailEdit = view.findViewById<TextInputEditText>(R.id.newEmailEditText)
        val newPasswordEdit = view.findViewById<TextInputEditText>(R.id.newPasswordEditText)
        val confirmPasswordEdit = view.findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val btnUpdateEmail = view.findViewById<MaterialButton>(R.id.btnUpdateEmail)
        val btnUpdatePassword = view.findViewById<MaterialButton>(R.id.btnUpdatePassword)
        val btnDeleteAccount = view.findViewById<MaterialButton>(R.id.btnDeleteAccount)

        // Load profile from user-specific SharedPreferences
        val prefs = requireContext().getSharedPreferences("UserProfilePrefs_${uid}", Context.MODE_PRIVATE)
        val currentName = prefs.getString("full_name", user?.displayName ?: "Jane Doe") ?: "Jane Doe"
        val currentBusiness = prefs.getString("business_name", "My Business Scheduler") ?: "My Business Scheduler"
        val currentBirthdate = prefs.getString("birthdate", "01/01/1990") ?: "01/01/1990"

        userNameTv.text = "Full Name: $currentName"
        userBusinessTv.text = "Business Name: $currentBusiness"
        userEmailTv.text = "Email: ${user?.email ?: "Not logged in"}"
        userBirthdateTv.text = "Birthdate: $currentBirthdate"

        fullNameEdit.setText(currentName)
        businessNameEdit.setText(currentBusiness)

        btnUpdateProfile.setOnClickListener {
            val newName = fullNameEdit.text.toString().trim()
            val newBusiness = businessNameEdit.text.toString().trim()

            if (newName.isEmpty() || newBusiness.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all profile fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("full_name", newName)
                .putString("business_name", newBusiness)
                .apply()

            user?.let { u ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                u.updateProfile(profileUpdates)
            }

            userNameTv.text = "Full Name: $newName"
            userBusinessTv.text = "Business Name: $newBusiness"
            Toast.makeText(requireContext(), "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
        }

        fun promptReauthAndUpdate(onSuccessReauth: () -> Unit) {
            val input = EditText(requireContext()).apply {
                hint = "Current Password"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Re-authentication Required")
                .setMessage("For security, please enter your current password to proceed.")
                .setView(input)
                .setPositiveButton("Verify") { _, _ ->
                    val currentPass = input.text.toString()
                    val email = user?.email
                    if (email != null && currentPass.isNotEmpty()) {
                        val credential = EmailAuthProvider.getCredential(email, currentPass)
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                onSuccessReauth()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Re-authentication failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnUpdateEmail.setOnClickListener {
            val newEmail = newEmailEdit.text.toString().trim()
            if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null) {
                Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fun executeEmailUpdate() {
                user.updateEmail(newEmail)
                    .addOnSuccessListener {
                        userEmailTv.text = "Email: $newEmail"
                        newEmailEdit.setText("")
                        Toast.makeText(requireContext(), "Email updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        if (e is FirebaseAuthRecentLoginRequiredException) {
                            promptReauthAndUpdate { executeEmailUpdate() }
                        } else {
                            Toast.makeText(requireContext(), "Failed to update email: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            }

            executeEmailUpdate()
        }

        btnUpdatePassword.setOnClickListener {
            val newPass = newPasswordEdit.text.toString().trim()
            val confirmPass = confirmPasswordEdit.text.toString().trim()

            if (newPass.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null) {
                Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fun executePasswordUpdate() {
                user.updatePassword(newPass)
                    .addOnSuccessListener {
                        newPasswordEdit.setText("")
                        confirmPasswordEdit.setText("")
                        Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        if (e is FirebaseAuthRecentLoginRequiredException) {
                            promptReauthAndUpdate { executePasswordUpdate() }
                        } else {
                            Toast.makeText(requireContext(), "Failed to update password: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            }

            executePasswordUpdate()
        }

        fun performFullAccountDeletion() {
            val userUid = user?.uid ?: "guest"

            // 1. Clear SharedPreferences for this user
            requireContext().getSharedPreferences("AppPrefs_${userUid}", Context.MODE_PRIVATE).edit().clear().apply()
            requireContext().getSharedPreferences("AvailabilityPrefs_${userUid}", Context.MODE_PRIVATE).edit().clear().apply()
            requireContext().getSharedPreferences("UserProfilePrefs_${userUid}", Context.MODE_PRIVATE).edit().clear().apply()
            requireContext().getSharedPreferences("PaywallPrefs_${userUid}", Context.MODE_PRIVATE).edit().clear().apply()

            // 2. Clear in-memory managers
            AppointmentManager.appointments.clear()
            ClientManager.clients.clear()
            ServiceManager.services.clear()
            AvailabilityManager.businessHours.clear()
            AvailabilityManager.holidays.clear()
            AvailabilityManager.breaks.clear()
            CategoryManager.categories.clear()

            // 3. Delete Firebase User Account
            user?.delete()
                ?.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Your account has been permanently deleted.", Toast.LENGTH_LONG).show()
                    val intent = Intent(requireContext(), WelcomeActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                ?.addOnFailureListener { e ->
                    if (e is FirebaseAuthRecentLoginRequiredException) {
                        promptReauthAndUpdate { performFullAccountDeletion() }
                    } else {
                        Toast.makeText(requireContext(), "Account deletion failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnDeleteAccount?.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Entire Account?")
                .setMessage("DANGER: Are you sure you want to permanently delete your account and all business data? This action CANNOT be undone.")
                .setPositiveButton("Yes, Delete Everything") { _, _ ->
                    performFullAccountDeletion()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        return view
    }
}
