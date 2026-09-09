package com.gaiazeye.businessscheduler

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gaiazeye.businessscheduler.R

class ServicesFragment : Fragment() {
    private lateinit var adapter: ServiceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_services_list, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        ServiceManager.sortServices()

        val recyclerView = view.findViewById<RecyclerView>(R.id.servicesRecycler)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ServiceAdapter(ServiceManager.services, 
            onClick = { service ->
                val fragment = AddServiceFragment().apply {
                    arguments = Bundle().apply {
                        putString("SERVICE_ID", service.id)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { service ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Service")
                    .setMessage("Are you sure you want to delete ${service.name}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ServiceManager.deleteService(service.id, requireContext())
                        adapter.notifyDataSetChanged()
                    }
                    .show()
            }
        )
        recyclerView.adapter = adapter

        val addServiceFab = view.findViewById<FloatingActionButton>(R.id.addServiceFab)
        addServiceFab.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, AddServiceFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        ServiceManager.sortServices()
        adapter.notifyDataSetChanged()
    }
}
