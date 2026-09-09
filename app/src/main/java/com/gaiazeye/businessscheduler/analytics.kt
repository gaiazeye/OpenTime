package com.gaiazeye.businessscheduler

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.gaiazeye.businessscheduler.R
import java.text.SimpleDateFormat
import java.util.*

class analytics : Fragment() {

    private lateinit var todayOverviewTv: TextView
    private lateinit var filteredStatsTv: TextView
    private lateinit var yearlyTitleTv: TextView
    private lateinit var yearlyAppointmentsTv: TextView
    private lateinit var appointmentsCountTv: TextView
    private lateinit var revenueAmountTv: TextView
    private lateinit var clientsCountTv: TextView
    private lateinit var mostBookedServiceTv: TextView
    private lateinit var monthAutocomplete: AutoCompleteTextView
    private lateinit var yearAutocomplete: AutoCompleteTextView
    private lateinit var fromDateEdit: TextInputEditText
    private lateinit var toDateEdit: TextInputEditText
    private lateinit var btnOpenSummaryBreakdown: MaterialButton

    private var selectedMonthIndex: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        todayOverviewTv = view.findViewById(R.id.todayOverviewText)
        filteredStatsTv = view.findViewById(R.id.filteredStatsText)
        yearlyTitleTv = view.findViewById(R.id.yearlyTitleText)
        yearlyAppointmentsTv = view.findViewById(R.id.yearlyAppointmentsCount)
        appointmentsCountTv = view.findViewById(R.id.appointmentsCount)
        revenueAmountTv = view.findViewById(R.id.revenueAmount)
        clientsCountTv = view.findViewById(R.id.clientsCount)
        mostBookedServiceTv = view.findViewById(R.id.mostBookedService)
        monthAutocomplete = view.findViewById(R.id.monthAutocomplete)
        yearAutocomplete = view.findViewById(R.id.yearAutocomplete)
        fromDateEdit = view.findViewById(R.id.fromDateEditText)
        toDateEdit = view.findViewById(R.id.toDateEditText)
        btnOpenSummaryBreakdown = view.findViewById(R.id.btnOpenSummaryBreakdown)

        setupMonthYearFilter()
        setupDatePickers()

        btnOpenSummaryBreakdown.setOnClickListener {
            val fromStr = fromDateEdit.text.toString()
            val toStr = toDateEdit.text.toString()
            val fragment = AnalyticsSummaryFragment.newInstance(fromStr, toStr)
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        updateStats()

        return view
    }

    private fun setupMonthYearFilter() {
        val months = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, months)
        monthAutocomplete.setAdapter(monthAdapter)
        monthAutocomplete.setText(months[selectedMonthIndex - 1], false)

        val currentYr = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYr - 2..currentYr + 3).map { it.toString() }
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, years)
        yearAutocomplete.setAdapter(yearAdapter)
        yearAutocomplete.setText(selectedYear.toString(), false)

        updateDateRangeFromMonthYear()

        monthAutocomplete.setOnItemClickListener { _, _, position, _ ->
            selectedMonthIndex = position + 1
            updateDateRangeFromMonthYear()
            updateStats()
        }

        yearAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val sel = parent.getItemAtPosition(position) as String
            selectedYear = sel.toIntOrNull() ?: currentYr
            updateDateRangeFromMonthYear()
            updateStats()
        }
    }

    private fun updateDateRangeFromMonthYear() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonthIndex - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val firstDay = sdf.format(cal.time)

        val lastDayNum = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, lastDayNum)
        val lastDay = sdf.format(cal.time)

        fromDateEdit.setText(firstDay)
        toDateEdit.setText(lastDay)
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        fromDateEdit.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                fromDateEdit.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", m + 1, d, y))
                updateStats()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        toDateEdit.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                toDateEdit.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", m + 1, d, y))
                updateStats()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val appointments = AppointmentManager.appointments
        val totalClients = ClientManager.clients.size

        // 1. Today's Stats
        val todayDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date())
        val todayAppts = appointments.filter { it.date == todayDate }
        val todayRevenue = todayAppts.sumOf { it.price }
        todayOverviewTv.text = String.format(
            Locale.getDefault(),
            "%d appointment(s) scheduled for today | Revenue: $%.2f",
            todayAppts.size, todayRevenue
        )

        // 2. Filtered Range Stats (From Date to To Date)
        val fromStr = fromDateEdit.text.toString()
        val toStr = toDateEdit.text.toString()
        val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val fromDate = try { sdfDate.parse(fromStr) } catch (e: Exception) { null }
        val toDate = try { sdfDate.parse(toStr) } catch (e: Exception) { null }

        val filteredAppts = appointments.filter { appt ->
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
        val filteredRevenue = filteredAppts.sumOf { it.price }
        filteredStatsTv.text = String.format(
            Locale.getDefault(),
            "Selected Range (%s to %s): %d appointment(s) | Revenue: $%.2f",
            fromStr, toStr, filteredAppts.size, filteredRevenue
        )

        // 3. Yearly Total Appointments
        val yearSuffix = String.format(Locale.getDefault(), "/%d", selectedYear)
        val yearlyAppts = appointments.filter { it.date.endsWith(yearSuffix) }
        yearlyTitleTv.text = "Yearly Total Appointments ($selectedYear)"
        yearlyAppointmentsTv.text = yearlyAppts.size.toString()

        // 4. Current Month Appointments
        val curMonth = SimpleDateFormat("MM", Locale.getDefault()).format(Date())
        val curYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val thisMonthAppts = appointments.filter {
            it.date.startsWith("$curMonth/") && it.date.endsWith("/$curYear")
        }
        appointmentsCountTv.text = thisMonthAppts.size.toString()

        // 5. Total All-time Revenue & Most Popular
        val totalRevenue = appointments.sumOf { it.price }
        val mostPopular = if (appointments.isNotEmpty()) {
            appointments.groupBy { it.serviceName }
                .maxByOrNull { it.value.size }?.key ?: "N/A"
        } else "N/A"

        clientsCountTv.text = totalClients.toString()
        revenueAmountTv.text = String.format(Locale.getDefault(), "$%.2f", totalRevenue)
        mostBookedServiceTv.text = mostPopular
    }
}
