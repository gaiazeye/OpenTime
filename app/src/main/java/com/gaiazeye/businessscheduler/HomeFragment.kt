package com.gaiazeye.businessscheduler

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.gaiazeye.businessscheduler.R
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var billingDateTv: TextView
    private lateinit var billingPlanTv: TextView
    private lateinit var totalAppointmentsTv: TextView
    private lateinit var totalClientsTv: TextView
    private lateinit var todayTotalTv: TextView
    private lateinit var remainingTodayTv: TextView
    private lateinit var monthlyRevenueTv: TextView
    private lateinit var upcomingScheduleTv: TextView
    private lateinit var upcomingHeaderTitle: TextView
    private lateinit var upcomingDropdownContainer: LinearLayout
    private lateinit var upcomingScheduleCard: MaterialCardView
    private lateinit var billingCard: MaterialCardView

    private var isUpcomingExpanded = false
    private val periodicUpdateHandler = Handler(Looper.getMainLooper())
    private val periodicUpdateRunnable = object : Runnable {
        override fun run() {
            if (isAdded && !isDetached) {
                updateDashboard()
                periodicUpdateHandler.postDelayed(this, 15000) // refresh every 15s for precise time updates
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val btnAddAppointment = view.findViewById<MaterialButton>(R.id.btnAddAppointment)
        val btnAddService = view.findViewById<MaterialButton>(R.id.btnAddService)
        val btnAddClient = view.findViewById<MaterialButton>(R.id.btnAddClient)
        
        billingDateTv = view.findViewById(R.id.billingDate)
        billingPlanTv = view.findViewById(R.id.billingPlan)
        totalAppointmentsTv = view.findViewById(R.id.totalAppointments)
        totalClientsTv = view.findViewById(R.id.totalClients)
        todayTotalTv = view.findViewById(R.id.todayTotal)
        remainingTodayTv = view.findViewById(R.id.remainingToday)
        monthlyRevenueTv = view.findViewById(R.id.monthlyRevenue)
        upcomingScheduleTv = view.findViewById(R.id.upcomingScheduleText)
        upcomingHeaderTitle = view.findViewById(R.id.upcomingHeaderTitle)
        upcomingDropdownContainer = view.findViewById(R.id.upcomingDropdownContainer)
        upcomingScheduleCard = view.findViewById(R.id.upcomingScheduleCard)
        billingCard = view.findViewById(R.id.billingCard)

        updateDashboard()

        btnAddAppointment.setOnClickListener {
            loadFragment(create_appointment())
        }

        btnAddService.setOnClickListener {
            loadFragment(AddServiceFragment())
        }

        btnAddClient.setOnClickListener {
            loadFragment(AddClientFragment())
        }

        billingCard.setOnClickListener {
            loadFragment(subscription_status())
        }

        val toggleScheduleVisibility = {
            isUpcomingExpanded = !isUpcomingExpanded
            upcomingDropdownContainer.visibility = if (isUpcomingExpanded) View.VISIBLE else View.GONE
            upcomingHeaderTitle.text = if (isUpcomingExpanded) "Upcoming Schedule (Tap to collapse)" else "Upcoming Schedule (Tap to expand)"
        }

        upcomingScheduleCard.setOnClickListener { toggleScheduleVisibility() }
        upcomingHeaderTitle.setOnClickListener { toggleScheduleVisibility() }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
        periodicUpdateHandler.removeCallbacks(periodicUpdateRunnable)
        periodicUpdateHandler.postDelayed(periodicUpdateRunnable, 15000)
    }

    override fun onPause() {
        super.onPause()
        periodicUpdateHandler.removeCallbacks(periodicUpdateRunnable)
    }

    private fun isSameDay(dateStr1: String, dateStr2: String): Boolean {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return try {
            val d1 = sdf.parse(dateStr1)
            val d2 = sdf.parse(dateStr2)
            d1 != null && d2 != null && d1 == d2
        } catch (e: Exception) {
            dateStr1.trim() == dateStr2.trim()
        }
    }

    private fun parseAppointmentStartTime(appt: Appointment): Date? {
        val formats = arrayOf(
            "MM/dd/yyyy h:mm a", "MM/dd/yyyy hh:mm a",
            "M/d/yyyy h:mm a", "M/d/yyyy hh:mm a",
            "MM/dd/yyyy H:mm", "MM/dd/yyyy HH:mm"
        )
        val combined = "${appt.date.trim()} ${appt.time.trim()}"
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val start = sdf.parse(combined)
                if (start != null) return start
            } catch (e: Exception) {}
        }
        return null
    }

    private fun showCancelConfirmationDialog(appt: Appointment) {
        val message = "Client: ${appt.clientName}\n" +
                "Service: ${appt.serviceName}\n" +
                "Date: ${appt.date}\n" +
                "Time: ${appt.time}\n" +
                "Price: $${String.format(Locale.getDefault(), "%.2f", appt.price)}"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Appointment Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .setNegativeButton("Cancel Appointment") { _, _ ->
                AppointmentManager.deleteAppointment(appt.id, requireContext())
                Toast.makeText(requireContext(), "Appointment Cancelled", Toast.LENGTH_SHORT).show()
                updateDashboard()
            }
            .show()
    }

    private fun updateDashboard() {
        context?.let { ctx ->
            AppointmentManager.loadAppointments(ctx)
            ClientManager.loadClients(ctx)
            ServiceManager.loadServices(ctx)
        }
        AppointmentManager.sortAppointments()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

        // Load Business Name from user profile
        val profilePrefs = requireContext().getSharedPreferences("UserProfilePrefs_${uid}", Context.MODE_PRIVATE)
        val businessName = profilePrefs.getString("business_name", "My Business Scheduler") ?: "My Business Scheduler"
        view?.findViewById<TextView>(R.id.companyName)?.text = businessName

        // Load Subscription Info
        val sharedPrefs = requireContext().getSharedPreferences("PaywallPrefs_${uid}", Context.MODE_PRIVATE)
        val planName = sharedPrefs.getString("plan_name", "Basic Plan")
        val planType = sharedPrefs.getString("plan_type", "Free")
        val billingDate = sharedPrefs.getString("billing_date", "N/A")

        billingPlanTv.text = "Plan: $planName ($planType)"
        billingDateTv.text = "Next Billing Date: $billingDate"

        // Update Dashboard Stats
        totalAppointmentsTv.text = AppointmentManager.appointments.size.toString()
        totalClientsTv.text = ClientManager.clients.size.toString()

        // Today's Date
        val now = Calendar.getInstance().time
        val todayDateStr = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(now)
        val todayAppointments = AppointmentManager.appointments.filter { isSameDay(it.date, todayDateStr) }
        todayTotalTv.text = todayAppointments.size.toString()

        // Remaining Today (do not count current appointment 5 minutes after it has begun)
        val todayRemainingAppts = todayAppointments.filter { appt ->
            val startTime = parseAppointmentStartTime(appt)
            if (startTime != null) {
                val graceTime = Calendar.getInstance().apply {
                    time = startTime
                    add(Calendar.MINUTE, 5)
                }.time
                graceTime.after(now)
            } else {
                true
            }
        }.sortedWith(compareBy { appt ->
            parseAppointmentStartTime(appt)?.time ?: 0L
        })

        remainingTodayTv.text = todayRemainingAppts.size.toString()

        // Monthly Revenue
        val currentMonth = SimpleDateFormat("MM", Locale.getDefault()).format(now)
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(now)
        val monthlyRevenue = AppointmentManager.appointments
            .filter { it.date.startsWith("$currentMonth/") && it.date.endsWith("/$currentYear") }
            .sumOf { it.price }
        
        monthlyRevenueTv.text = String.format(Locale.getDefault(), "$%.2f", monthlyRevenue)

        // Upcoming Appointments: Filter appointments occurring within the 24-hour window from current time
        val nowMs = now.time
        val twentyFourHoursLaterMs = nowMs + (24 * 60 * 60 * 1000L)

        val upcoming24HourAppts = AppointmentManager.appointments.filter { appt ->
            val startTime = parseAppointmentStartTime(appt)
            if (startTime != null) {
                val startMs = startTime.time
                startMs > nowMs && startMs <= twentyFourHoursLaterMs
            } else {
                false
            }
        }.sortedWith(compareBy { appt ->
            parseAppointmentStartTime(appt)?.time ?: 0L
        })

        val nextAppt = upcoming24HourAppts.firstOrNull()

        if (nextAppt != null) {
            val dateLabel = if (isSameDay(nextAppt.date, todayDateStr)) "Today" else nextAppt.date
            upcomingScheduleTv.text = "Next: ${nextAppt.clientName} - ${nextAppt.serviceName} ($dateLabel at ${nextAppt.time})"
            upcomingScheduleTv.setTextColor(resources.getColor(android.R.color.black, null))
        } else {
            upcomingScheduleTv.text = "No upcoming appointments within the next 24 hours."
            upcomingScheduleTv.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        // Populate dropdown container with interactive cancel/details support
        upcomingDropdownContainer.removeAllViews()
        if (upcoming24HourAppts.isEmpty()) {
            val tv = TextView(context).apply {
                text = "No upcoming appointments within the next 24 hours."
                setPadding(0, 8, 0, 8)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            upcomingDropdownContainer.addView(tv)
        } else {
            for ((index, appt) in upcoming24HourAppts.withIndex()) {
                val dateLabel = if (isSameDay(appt.date, todayDateStr)) "Today" else appt.date
                val itemTv = TextView(context).apply {
                    text = "${index + 1}. $dateLabel at ${appt.time} - ${appt.clientName} (${appt.serviceName}) [$${String.format(Locale.getDefault(), "%.2f", appt.price)}]"
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                    setTextColor(resources.getColor(android.R.color.black, null))
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        showCancelConfirmationDialog(appt)
                    }
                }
                upcomingDropdownContainer.addView(itemTv)
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }
}
