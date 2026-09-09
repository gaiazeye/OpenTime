package com.gaiazeye.businessscheduler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ServiceAdapter(
    private val services: List<Service>,
    private val onClick: (Service) -> Unit,
    private val onLongClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.serviceName)
        val infoTv: TextView = view.findViewById(R.id.serviceInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        val durationFormatted = AddServiceFragment.formatMinutesToWords(service.durationMinutes)
        val durationDisplay = if (durationFormatted.isNotEmpty()) durationFormatted else "${service.durationMinutes} min"

        holder.nameTv.text = service.name
        holder.infoTv.text = String.format(Locale.getDefault(), "%s | %s | $%.2f", service.category, durationDisplay, service.price)
        
        holder.itemView.setOnClickListener { onClick(service) }
        holder.itemView.setOnLongClickListener {
            onLongClick(service)
            true
        }
    }

    override fun getItemCount() = services.size
}
