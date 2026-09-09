package com.gaiazeye.businessscheduler

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gaiazeye.businessscheduler.R
import java.text.SimpleDateFormat
import java.util.*

class working_hours : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_working_hours, container, false)
        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val recycler = view.findViewById<RecyclerView>(R.id.daysRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = HoursAdapter(AvailabilityManager.businessHours)
        return view
    }

    inner class HoursAdapter(private val list: List<BusinessHours>) : RecyclerView.Adapter<HoursAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val dayName = v.findViewById<TextView>(R.id.dayName)
            val startTime = v.findViewById<TextView>(R.id.startTime)
            val endTime = v.findViewById<TextView>(R.id.endTime)
            val closedCheck = v.findViewById<CheckBox>(R.id.closedCheck)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = 
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_working_hour, parent, false))
        
        override fun onBindViewHolder(holder: VH, position: Int) {
            val h = list[position]
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, h.dayOfWeek) }
            holder.dayName.text = sdf.format(cal.time)
            holder.startTime.text = h.startTime
            holder.endTime.text = h.endTime
            holder.closedCheck.isChecked = h.isClosed

            fun updateEnabledState() {
                val enabled = !h.isClosed
                holder.startTime.isEnabled = enabled
                holder.endTime.isEnabled = enabled
                holder.startTime.alpha = if (enabled) 1.0f else 0.4f
                holder.endTime.alpha = if (enabled) 1.0f else 0.4f
            }
            updateEnabledState()

            holder.startTime.setOnClickListener {
                showTimePicker(h.startTime) {
                    h.startTime = it
                    notifyItemChanged(position)
                    AvailabilityManager.saveData(requireContext())
                }
            }
            holder.endTime.setOnClickListener {
                showTimePicker(h.endTime) {
                    h.endTime = it
                    notifyItemChanged(position)
                    AvailabilityManager.saveData(requireContext())
                }
            }
            holder.closedCheck.setOnCheckedChangeListener { _, isChecked ->
                h.isClosed = isChecked
                updateEnabledState()
                AvailabilityManager.saveData(requireContext())
            }
        }
        override fun getItemCount() = list.size

        private fun showTimePicker(currentTime: String, onTimeSet: (String) -> Unit) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = sdf.parse(currentTime) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            TimePickerDialog(requireContext(), { _, hour, min ->
                val resCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                }
                onTimeSet(sdf.format(resCal.time))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }
    }
}
