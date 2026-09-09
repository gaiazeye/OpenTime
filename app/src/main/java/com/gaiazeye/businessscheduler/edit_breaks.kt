package com.gaiazeye.businessscheduler

import com.gaiazeye.businessscheduler.R

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class edit_breaks : Fragment() {

    private lateinit var adapter: BreakAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_edit_breaks, container, false)
        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val recycler = view.findViewById<RecyclerView>(R.id.breaksRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = BreakAdapter(AvailabilityManager.breaks)
        recycler.adapter = adapter

        view.findViewById<MaterialButton>(R.id.addBreakButton).setOnClickListener {
            showAddBreakDialog()
        }
        return view
    }

    private fun showAddBreakDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_break, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.breakNameEdit)
        val startText = dialogView.findViewById<TextView>(R.id.breakStartText)
        val endText = dialogView.findViewById<TextView>(R.id.breakEndText)
        val infoText = dialogView.findViewById<TextView>(R.id.workingHoursInfoText)

        val openDays = AvailabilityManager.businessHours.filter { !it.isClosed }
        val minStart = if (openDays.isNotEmpty()) openDays.minOf { it.startTime } else "09:00 AM"
        val maxEnd = if (openDays.isNotEmpty()) openDays.maxOf { it.endTime } else "05:00 PM"

        infoText.text = "Working Hours Availability: $minStart - $maxEnd"

        var start = "12:00 PM"
        var end = "01:00 PM"

        startText.setOnClickListener { showTimePicker(start) { start = it; startText.text = it } }
        endText.setOnClickListener { showTimePicker(end) { end = it; endText.text = it } }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Break")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEdit.text.toString()
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                val sTime = sdf.parse(start)
                val eTime = sdf.parse(end)
                val minTime = sdf.parse(minStart)
                val maxTime = sdf.parse(maxEnd)

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter break name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (sTime != null && eTime != null && !sTime.before(eTime)) {
                    Toast.makeText(requireContext(), "Break start time must be before end time!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (sTime != null && minTime != null && eTime != null && maxTime != null) {
                    if (sTime.before(minTime) || eTime.after(maxTime)) {
                        Toast.makeText(requireContext(), "Break must be within working hours ($minStart - $maxEnd)!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                AvailabilityManager.breaks.add(Break(name, start, end))
                AvailabilityManager.saveData(requireContext())
                adapter.notifyItemInserted(AvailabilityManager.breaks.size - 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(currentTime: String, onTimeSet: (String) -> Unit) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = sdf.parse(currentTime) ?: Date()
        val cal = Calendar.getInstance().apply { time = date }
        TimePickerDialog(requireContext(), { _, h, m ->
            val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
            onTimeSet(sdf.format(c.time))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    inner class BreakAdapter(private val list: MutableList<Break>) : RecyclerView.Adapter<BreakAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name = v.findViewById<TextView>(android.R.id.text1)
            val times = v.findViewById<TextView>(android.R.id.text2)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = 
            VH(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val b = list[position]
            holder.name.text = b.name
            holder.times.text = "${b.startTime} - ${b.endTime}"
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Break")
                    .setMessage("Remove ${b.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        list.removeAt(position)
                        AvailabilityManager.saveData(requireContext())
                        notifyItemRemoved(position)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
        }
        override fun getItemCount() = list.size
    }
}
