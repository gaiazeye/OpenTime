package com.gaiazeye.businessscheduler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.gaiazeye.businessscheduler.R
import java.util.UUID

class AddClientFragment : Fragment() {

    private var editingClientId: String? = null
    private var oldClientName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_client, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val nameField = view.findViewById<TextInputEditText>(R.id.clientNameEditText)
        val emailField = view.findViewById<TextInputEditText>(R.id.clientEmailEditText)
        val phoneField = view.findViewById<TextInputEditText>(R.id.clientPhoneEditText)
        val businessField = view.findViewById<TextInputEditText>(R.id.clientBusinessEditText)
        val notesField = view.findViewById<TextInputEditText>(R.id.clientNotesEditText)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveClientButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteClientButton)

        editingClientId = arguments?.getString("CLIENT_ID")
        editingClientId?.let { id ->
            val client = ClientManager.clients.find { it.id == id }
            client?.let {
                oldClientName = it.name
                nameField.setText(it.name)
                emailField.setText(it.email)
                phoneField.setText(it.phone)
                businessField.setText(it.business)
                notesField.setText(it.notes)
                saveButton.text = "Update Client"
                deleteButton.visibility = View.VISIBLE
            }
        }

        deleteButton.setOnClickListener {
            editingClientId?.let { id ->
                ClientManager.deleteClient(id, requireContext())
                Toast.makeText(context, "Client deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        saveButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val business = businessField.text.toString().trim()
            val notes = notesField.text.toString().trim()

            if (name.isEmpty()) {
                nameField.error = "Name is required"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailField.error = "Email is required"
                return@setOnClickListener
            }

            if (phone.isEmpty()) {
                phoneField.error = "Phone number is required"
                return@setOnClickListener
            }

            if (editingClientId != null) {
                val updatedClient = Client(
                    id = editingClientId!!,
                    name = name,
                    email = email,
                    phone = phone,
                    business = business,
                    notes = notes
                )
                ClientManager.updateClient(updatedClient, requireContext())
                AppointmentManager.onClientUpdated(oldClientName, name, requireContext())
                Toast.makeText(context, "Client $name updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                val newClient = Client(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    email = email,
                    phone = phone,
                    business = business,
                    notes = notes
                )
                ClientManager.clients.add(newClient)
                ClientManager.saveClients(requireContext())
                Toast.makeText(context, "Client $name saved successfully!", Toast.LENGTH_SHORT).show()
            }
            
            // Navigate back
            parentFragmentManager.popBackStack()
        }

        return view
    }
}
