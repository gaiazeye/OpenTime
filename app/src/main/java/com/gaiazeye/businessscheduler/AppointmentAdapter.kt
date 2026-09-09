package com.gaiazeye.businessscheduler

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AppointmentAdapter(
    private val appointments: List<Appointment>,
    private val onClick: (Appointment) -> Unit,
    private val onLongClick: (Appointment) -> Unit = {}
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clientName: TextView = view.findViewById(R.id.text1)
        val details: TextView = view.findViewById(R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.clientName.text = appointment.clientName
        holder.details.text = String.format(Locale.getDefault(), "%s at %s ($%.2f)", 
            appointment.serviceName, appointment.time, appointment.price)
        
        holder.itemView.setOnClickListener {
            onClick(appointment)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(appointment)
            true
        }
    }

    override fun getItemCount() = appointments.size
}
