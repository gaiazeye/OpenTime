package com.gaiazeye.businessscheduler

import com.gaiazeye.businessscheduler.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsSummaryFragment : Fragment() {

    private lateinit var dateRangeHeaderText: TextView
    private lateinit var totalAppointmentsText: TextView
    private lateinit var totalRevenueText: TextView
    private lateinit var activeDaysText: TextView
    private lateinit var avgRevenueText: TextView
    private lateinit var dailyBreakdownRecycler: RecyclerView

    private var fromDateStr: String = ""
    private var toDateStr: String = ""

    companion object {
        private const val ARG_FROM_DATE = "FROM_DATE"
        private const val ARG_TO_DATE = "TO_DATE"

        fun newInstance(fromDate: String, toDate: String): AnalyticsSummaryFragment {
            return AnalyticsSummaryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FROM_DATE, fromDate)
                    putString(ARG_TO_DATE, toDate)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics_summary, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        dateRangeHeaderText = view.findViewById(R.id.dateRangeHeaderText)
        totalAppointmentsText = view.findViewById(R.id.totalAppointmentsText)
        totalRevenueText = view.findViewById(R.id.totalRevenueText)
        activeDaysText = view.findViewById(R.id.activeDaysText)
        avgRevenueText = view.findViewById(R.id.avgRevenueText)
        dailyBreakdownRecycler = view.findViewById(R.id.dailyBreakdownRecycler)

        dailyBreakdownRecycler.layoutManager = LinearLayoutManager(requireContext())
        dailyBreakdownRecycler.isNestedScrollingEnabled = false

        fromDateStr = arguments?.getString(ARG_FROM_DATE) ?: ""
        toDateStr = arguments?.getString(ARG_TO_DATE) ?: ""

        dateRangeHeaderText.text = "Range: $fromDateStr to $toDateStr"

        loadSummaryData()

        return view
    }

    private fun loadSummaryData() {
        val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val fromDate = try { sdfDate.parse(fromDateStr) } catch (e: Exception) { null }
        val toDate = try { sdfDate.parse(toDateStr) } catch (e: Exception) { null }

        AppointmentManager.sortAppointments()

        val matchingAppts = AppointmentManager.appointments.filter { appt ->
            try {
                val d = sdfDate.parse(appt.date)
                if (d != null) {
                    val inFrom = fromDate == null || !d.before(fromDate)
                    val inTo = toDate == null || !d.after(toDate)
                    inFrom && inTo
                } else false
            } catch (e: Exception) {
                false
            }
        }

        // Group by date
        val grouped = matchingAppts.groupBy { it.date }.toSortedMap(compareBy {
            try { sdfDate.parse(it)?.time ?: 0L } catch (e: Exception) { 0L }
        })

        val adapter = DailySummaryAdapter(grouped)
        dailyBreakdownRecycler.adapter = adapter

        // Calculate Totals at bottom
        val totalAppts = matchingAppts.size
        val totalRev = matchingAppts.sumOf { it.price }
        val activeDays = grouped.size
        val avgRev = if (activeDays > 0) totalRev / activeDays else 0.0

        totalAppointmentsText.text = "Total Appointments: $totalAppts"
        totalRevenueText.text = String.format(Locale.getDefault(), "Total Revenue: $%.2f", totalRev)
        activeDaysText.text = "Active Days with Appointments: $activeDays"
        avgRevenueText.text = String.format(Locale.getDefault(), "Average Revenue per Active Day: $%.2f", avgRev)
    }

    inner class DailySummaryAdapter(
        private val dayGroupMap: Map<String, List<Appointment>>
    ) : RecyclerView.Adapter<DailySummaryAdapter.VH>() {

        private val datesList = dayGroupMap.keys.toList()

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val dateHeader: TextView = v.findViewById(R.id.dailyDateHeader)
            val revenueText: TextView = v.findViewById(R.id.dailyRevenueText)
            val apptCountText: TextView = v.findViewById(R.id.dailyApptCountText)
            val listContainer: LinearLayout = v.findViewById(R.id.dailyAppointmentsListContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_summary, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val dateStr = datesList[position]
            val apptsForDay = dayGroupMap[dateStr] ?: emptyList()
            val dayRev = apptsForDay.sumOf { it.price }

            val sdfInput = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val sdfDisplay = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
            var formattedDate = dateStr
            try {
                val parsed = sdfInput.parse(dateStr)
                if (parsed != null) formattedDate = sdfDisplay.format(parsed)
            } catch (e: Exception) {}

            holder.dateHeader.text = formattedDate
            holder.revenueText.text = String.format(Locale.getDefault(), "$%.2f", dayRev)
            holder.apptCountText.text = "${apptsForDay.size} Appointment(s)"

            holder.listContainer.removeAllViews()
            for (appt in apptsForDay) {
                val itemTv = TextView(context).apply {
                    text = "• ${appt.time} - ${appt.clientName} (${appt.serviceName}) [$${String.format(Locale.getDefault(), "%.2f", appt.price)}]"
                    textSize = 14f
                    setPadding(0, 4, 0, 4)
                    setTextColor(resources.getColor(R.color.black, null))
                }
                holder.listContainer.addView(itemTv)
            }
        }

        override fun getItemCount() = datesList.size
    }
}
