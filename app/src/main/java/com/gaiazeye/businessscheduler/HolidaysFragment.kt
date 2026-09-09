package com.gaiazeye.businessscheduler

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.gaiazeye.businessscheduler.R
import java.util.*

class HolidaysFragment : Fragment() {

    private lateinit var adapter: HolidayAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_holidays, container, false)
        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        val recycler = view.findViewById<RecyclerView>(R.id.holidaysRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = HolidayAdapter(AvailabilityManager.holidays)
        recycler.adapter = adapter

        view.findViewById<MaterialButton>(R.id.addHolidayButton).setOnClickListener {
            showAddHolidayDialog()
        }

        return view
    }

    private fun showAddHolidayDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_holiday, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.holidayNameEdit)
        val dateText = dialogView.findViewById<TextView>(R.id.holidayDateText)
        
        var selectedDate = ""
        dateText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", m + 1, d, y)
                dateText.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Holiday")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEdit.text.toString()
                if (name.isNotEmpty() && selectedDate.isNotEmpty()) {
                    AvailabilityManager.holidays.add(Holiday(selectedDate, name, isClosed = true))
                    AvailabilityManager.saveData(requireContext())
                    adapter.notifyItemInserted(AvailabilityManager.holidays.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class HolidayAdapter(private val list: MutableList<Holiday>) : RecyclerView.Adapter<HolidayAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name = v.findViewById<TextView>(R.id.holidayNameText)
            val date = v.findViewById<TextView>(R.id.holidayDateText)
            val closedCheck = v.findViewById<CheckBox>(R.id.holidayClosedCheck)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = 
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_holiday, parent, false))
            
        override fun onBindViewHolder(holder: VH, position: Int) {
            val h = list[position]
            holder.name.text = h.name
            holder.date.text = h.date
            holder.closedCheck.isChecked = h.isClosed

            holder.closedCheck.setOnCheckedChangeListener { _, isChecked ->
                h.isClosed = isChecked
                AvailabilityManager.saveData(requireContext())
            }

            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Holiday")
                    .setMessage("Remove ${h.name} from list?")
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
