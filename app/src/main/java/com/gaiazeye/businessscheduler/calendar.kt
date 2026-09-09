package com.gaiazeye.businessscheduler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class calendar : Fragment() {
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView
    private lateinit var monthlySummaryText: TextView
    private lateinit var dailySummaryText: TextView
    private lateinit var appointmentDaysChipGroup: ChipGroup
    private lateinit var appointmentDaysScroll: HorizontalScrollView
    private lateinit var adapter: AppointmentAdapter
    private var currentSelectedDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        calendarView = view.findViewById(R.id.calendarView)
        recyclerView = view.findViewById(R.id.dayAppointmentsRecycler)
        monthlySummaryText = view.findViewById(R.id.monthlySummaryText)
        dailySummaryText = view.findViewById(R.id.dailySummaryText)
        appointmentDaysChipGroup = view.findViewById(R.id.appointmentDaysChipGroup)
        appointmentDaysScroll = view.findViewById(R.id.appointmentDaysScroll)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.isNestedScrollingEnabled = true

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", month + 1, dayOfMonth, year)
            selectDate(selectedDate, month + 1, year)
        }

        // Initialize for today
        val today = Calendar.getInstance()
        val todayDate = String.format(Locale.getDefault(), "%02d/%02d/%d", 
            today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.YEAR))
        selectDate(todayDate, today.get(Calendar.MONTH) + 1, today.get(Calendar.YEAR))

        val fab = view.findViewById<FloatingActionButton>(R.id.addAppointmentFab)
        fab.setOnClickListener {
            checkAndOpenCreateAppointment(currentSelectedDate ?: todayDate)
        }

        return view
    }

    private fun checkAndOpenCreateAppointment(date: String) {
        val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val todayStr = sdfDate.format(Date())
        val todayStart = try { sdfDate.parse(todayStr) } catch (e: Exception) { null }
        val targetDate = try { sdfDate.parse(date) } catch (e: Exception) { null }

        if (targetDate != null && todayStart != null && targetDate.before(todayStart)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Past Date Selected")
                .setMessage("Appointments cannot be made for past dates ($date). Please select today or a future date.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val dayOpen = AvailabilityManager.isDayOpen(date)
        if (!dayOpen) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Time Slot Unavailable")
                .setMessage("Business is closed on $date. Do you still want to open the appointment scheduler?")
                .setPositiveButton("Continue") { _, _ ->
                    openCreateAppointment()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            openCreateAppointment()
        }
    }

    private fun openCreateAppointment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, create_appointment())
            .addToBackStack(null)
            .commit()
    }

    private fun selectDate(date: String, month: Int, year: Int) {
        currentSelectedDate = date
        AppointmentManager.selectedDateForAppointment = date
        filterAppointments(date)
        updateMonthlyAndDailySummary(date, month, year)
    }

    private fun filterAppointments(date: String) {
        AppointmentManager.sortAppointments()
        
        val filtered = AppointmentManager.appointments.filter { it.date == date }
        adapter = AppointmentAdapter(
            appointments = filtered,
            onClick = { appointment ->
                showAppointmentDetailsDialog(appointment, date)
            },
            onLongClick = { appointment ->
                showAppointmentDetailsDialog(appointment, date)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun showAppointmentDetailsDialog(appointment: Appointment, date: String) {
        val message = "Client: ${appointment.clientName}\n" +
                "Service: ${appointment.serviceName}\n" +
                "Date: ${appointment.date}\n" +
                "Time: ${appointment.time}\n" +
                "Price: $${String.format(Locale.getDefault(), "%.2f", appointment.price)}"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Appointment Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .setNegativeButton("Cancel Appointment") { _, _ ->
                AppointmentManager.deleteAppointment(appointment.id, requireContext())
                filterAppointments(date)
                
                val parts = date.split("/")
                if (parts.size == 3) {
                    updateMonthlyAndDailySummary(date, parts[0].toInt(), parts[2].toInt())
                }
            }
            .show()
    }

    private fun updateMonthlyAndDailySummary(selectedDate: String, month: Int, year: Int) {
        val monthPrefix = String.format(Locale.getDefault(), "%02d/", month)
        val yearSuffix = String.format(Locale.getDefault(), "/%d", year)
        
        val monthlyAppts = AppointmentManager.appointments.filter { 
            it.date.startsWith(monthPrefix) && it.date.endsWith(yearSuffix) 
        }
        
        val busyDates = monthlyAppts.map { it.date }.distinct().sorted()
        monthlySummaryText.text = "Monthly Overview: ${busyDates.size} days with appointments in ${getMonthName(month)}"

        // Daily Overview for selected date
        val dailyAppts = AppointmentManager.appointments.filter { it.date == selectedDate }
        val dailyRevenue = dailyAppts.sumOf { it.price }
        dailySummaryText.text = String.format(
            Locale.getDefault(),
            "Daily Overview (%s): %d appointment(s) | Total: $%.2f",
            selectedDate, dailyAppts.size, dailyRevenue
        )

        // Populate appointment day chips/markers
        appointmentDaysChipGroup.removeAllViews()
        var selectedChipView: View? = null

        if (busyDates.isEmpty()) {
            val emptyChip = Chip(requireContext()).apply {
                text = "No appointments this month"
                isEnabled = false
            }
            appointmentDaysChipGroup.addView(emptyChip)
        } else {
            val sdfInput = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val sdfOutput = SimpleDateFormat("MMM dd", Locale.getDefault())

            for (bDate in busyDates) {
                val apptCount = monthlyAppts.count { it.date == bDate }
                val chip = Chip(requireContext())
                try {
                    val parsed = sdfInput.parse(bDate)
                    val label = if (parsed != null) sdfOutput.format(parsed) else bDate
                    chip.text = "📅 $label ($apptCount)"
                } catch (e: Exception) {
                    chip.text = "$bDate ($apptCount)"
                }
                chip.isCheckable = true
                chip.isChecked = (bDate == selectedDate)
                chip.tag = bDate
                if (bDate == selectedDate) {
                    selectedChipView = chip
                }

                chip.setOnClickListener {
                    try {
                        val parsed = sdfInput.parse(bDate)
                        if (parsed != null) {
                            calendarView.date = parsed.time
                            selectDate(bDate, month, year)
                        }
                    } catch (e: Exception) {}
                }
                appointmentDaysChipGroup.addView(chip)
            }
        }

        appointmentDaysScroll.post {
            selectedChipView?.post {
                val index = appointmentDaysChipGroup.indexOfChild(selectedChipView)
                val totalChips = appointmentDaysChipGroup.childCount

                val chipLeft = selectedChipView.left
                val chipWidth = selectedChipView.width
                val scrollWidth = appointmentDaysScroll.width
                val maxScrollX = appointmentDaysChipGroup.width - scrollWidth

                val targetScrollX = when {
                    index == 0 -> 0
                    index == totalChips - 1 && maxScrollX > 0 -> maxScrollX
                    else -> Math.max(0, chipLeft - (scrollWidth - chipWidth) / 2)
                }
                appointmentDaysScroll.smoothScrollTo(targetScrollX, 0)
            }
        }
    }

    private fun getMonthName(month: Int): String {
        return DateFormatSymbols().months[month - 1]
    }
}
