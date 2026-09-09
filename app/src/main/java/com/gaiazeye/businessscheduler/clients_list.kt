package com.gaiazeye.businessscheduler

import com.gaiazeye.businessscheduler.R

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class clients_list : Fragment() {
    private lateinit var adapter: ClientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clients_list, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.clientsRecycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.isNestedScrollingEnabled = false
        adapter = ClientAdapter(ClientManager.clients,
            onClick = { client ->
                val fragment = AddClientFragment().apply {
                    arguments = Bundle().apply {
                        putString("CLIENT_ID", client.id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { client ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Client")
                    .setMessage("Are you sure you want to delete ${client.name}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ClientManager.deleteClient(client.id, requireContext())
                        adapter.updateList(ClientManager.clients)
                    }
                    .show()
            }
        )
        recyclerView.adapter = adapter

        val searchEditText = view.findViewById<TextInputEditText>(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val addClientFab = view.findViewById<FloatingActionButton>(R.id.addClientFab)
        addClientFab.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, AddClientFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        adapter.updateList(ClientManager.clients)
    }
}
