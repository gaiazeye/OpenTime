package com.gaiazeye.businessscheduler

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import java.util.UUID

class AddServiceFragment : Fragment() {

    private var editingServiceId: String? = null
    private var oldServiceName: String = ""

    companion object {
        fun parseDurationToMinutes(input: String): Int {
            val str = input.trim().lowercase(Locale.ROOT)
            if (str.isEmpty()) return 0

            if (str.contains(":")) {
                val parts = str.split(":")
                val hours = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                val mins = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                return (hours * 60) + mins
            }

            val hourRegex = "(\\d+(?:\\.\\d+)?)\\s*(?:hr|hrs|hour|hours|h)".toRegex()
            val minRegex = "(\\d+)\\s*(?:min|mins|minute|minutes|m)".toRegex()

            val hourMatch = hourRegex.find(str)
            val minMatch = minRegex.find(str)

            if (hourMatch != null || minMatch != null) {
                val hours = hourMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                val mins = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                return (hours * 60).toInt() + mins
            }

            if (str.contains(".")) {
                val doubleVal = str.toDoubleOrNull()
                if (doubleVal != null && doubleVal > 0) {
                    return (doubleVal * 60).toInt()
                }
            }

            return str.toIntOrNull() ?: 0
        }

        fun formatMinutesToWords(totalMinutes: Int): String {
            if (totalMinutes <= 0) return ""
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60

            val hrPart = when {
                hours == 0 -> ""
                hours == 1 -> "1 hour"
                else -> "$hours hours"
            }

            val minPart = when {
                mins == 0 -> ""
                mins == 1 -> "1 minute"
                else -> "$mins minutes"
            }

            val text = when {
                hours == 1 && mins == 30 -> "an hour and a half"
                hours > 0 && mins > 0 -> "$hrPart $minPart"
                hours > 0 -> hrPart
                else -> minPart
            }

            return "$text ($totalMinutes mins)"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_service, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val nameEdit = view.findViewById<TextInputEditText>(R.id.serviceNameEditText)
        val categoryAutocomplete = view.findViewById<AutoCompleteTextView>(R.id.categoryAutocomplete)
        val btnManageCategories = view.findViewById<MaterialButton>(R.id.btnManageCategories)
        val durationInputLayout = view.findViewById<TextInputLayout>(R.id.durationInputLayout)
        val durationEdit = view.findViewById<TextInputEditText>(R.id.durationEditText)
        val priceEdit = view.findViewById<TextInputEditText>(R.id.priceEditText)
        val descriptionEdit = view.findViewById<TextInputEditText>(R.id.descriptionEditText)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveServiceButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteServiceButton)

        fun refreshCategoryDropdown() {
            CategoryManager.init(requireContext())
            val currentList = CategoryManager.categories.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currentList)
            categoryAutocomplete.setAdapter(categoryAdapter)
        }

        refreshCategoryDropdown()

        btnManageCategories.setOnClickListener {
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
                            refreshCategoryDropdown()
                            Toast.makeText(requireContext(), "Category '$selectedCategory' deleted", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Close", null)
                .show()
        }

        fun updateDurationHelperText(input: String) {
            val mins = parseDurationToMinutes(input)
            if (mins > 0) {
                val formatted = formatMinutesToWords(mins)
                durationInputLayout.helperText = "Converts to: $formatted"
            } else {
                durationInputLayout.helperText = null
            }
        }

        durationEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateDurationHelperText(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        priceEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val text = priceEdit.text.toString()
                if (text == "0.0" || text == "0") {
                    priceEdit.setText("")
                }
            }
        }

        editingServiceId = arguments?.getString("SERVICE_ID")
        editingServiceId?.let { id ->
            val service = ServiceManager.services.find { it.id == id }
            service?.let {
                oldServiceName = it.name
                nameEdit.setText(it.name)
                categoryAutocomplete.setText(it.category, false)
                durationEdit.setText(it.durationMinutes.toString())
                updateDurationHelperText(it.durationMinutes.toString())
                priceEdit.setText(it.price.toString())
                descriptionEdit.setText(it.description)
                saveButton.text = "Update Service"
                deleteButton.visibility = View.VISIBLE
            }
        }

        deleteButton.setOnClickListener {
            editingServiceId?.let { id ->
                ServiceManager.deleteService(id, requireContext())
                Toast.makeText(context, "Service deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        saveButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val category = categoryAutocomplete.text.toString().trim()
            val durationStr = durationEdit.text.toString()
            val priceStr = priceEdit.text.toString()
            val description = descriptionEdit.text.toString()

            val duration = parseDurationToMinutes(durationStr)
            val price = priceStr.toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || category.isEmpty() || duration <= 0 || priceStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill required fields with valid values", Toast.LENGTH_SHORT).show()
            } else {
                CategoryManager.addCategory(category, requireContext())

                if (editingServiceId != null) {
                    val updatedService = Service(
                        id = editingServiceId!!,
                        name = name,
                        category = category,
                        durationMinutes = duration,
                        price = price,
                        description = description
                    )
                    ServiceManager.updateService(updatedService, requireContext())
                    AppointmentManager.onServiceUpdated(oldServiceName, name, price, requireContext())
                    Toast.makeText(requireContext(), "Service Updated!", Toast.LENGTH_SHORT).show()
                } else {
                    val newService = Service(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        category = category,
                        durationMinutes = duration,
                        price = price,
                        description = description
                    )
                    ServiceManager.services.add(newService)
                    ServiceManager.saveServices(requireContext())
                    Toast.makeText(requireContext(), "Service Added!", Toast.LENGTH_SHORT).show()
                }
                parentFragmentManager.popBackStack()
            }
        }

        return view
    }
}
