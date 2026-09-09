package com.gaiazeye.businessscheduler

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        view.findViewById<MaterialButton>(R.id.btnWorkingHours).setOnClickListener {
            loadFragment(working_hours())
        }
        view.findViewById<MaterialButton>(R.id.btnHolidays).setOnClickListener {
            loadFragment(HolidaysFragment())
        }
        view.findViewById<MaterialButton>(R.id.btnBreaks).setOnClickListener {
            loadFragment(edit_breaks())
        }
        view.findViewById<MaterialButton>(R.id.btnCategories)?.setOnClickListener {
            CategoryManager.init(requireContext())
            val categoryList = CategoryManager.categories.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

            if (categoryList.isEmpty()) {
                Toast.makeText(requireContext(), "No categories to manage.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryArray = categoryList.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tap a Category to Delete")
                .setItems(categoryArray) { _, which ->
                    val selectedCategory = categoryArray[which]
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete category '$selectedCategory'?")
                        .setPositiveButton("Delete") { _, _ ->
                            CategoryManager.deleteCategory(selectedCategory, requireContext())
                            Toast.makeText(requireContext(), "Category '$selectedCategory' deleted", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Close", null)
                .show()
        }
        view.findViewById<MaterialButton>(R.id.btnAccount)?.setOnClickListener {
            loadFragment(AccountFragment())
        }
        view.findViewById<MaterialButton>(R.id.btnSubscription).setOnClickListener {
            loadFragment(subscription_status())
        }
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            requireActivity().finish()
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}
