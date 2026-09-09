package com.gaiazeye.businessscheduler

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

object AvailabilityManager {
    var businessHours: MutableList<BusinessHours> = mutableListOf()
    var holidays: MutableList<Holiday> = mutableListOf()
    var breaks: MutableList<Break> = mutableListOf()

    private val gson = Gson()

    private fun getUserPrefsName(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "AvailabilityPrefs_${uid}"
    }

    fun init(context: Context) {
        loadData(context)
        if (businessHours.isEmpty()) {
            for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
                businessHours.add(BusinessHours(i, "09:00 AM", "05:00 PM", i == Calendar.SUNDAY || i == Calendar.SATURDAY))
            }
            saveData(context)
        }
        if (holidays.isEmpty()) {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            holidays.addAll(
                listOf(
                    Holiday("01/01/$year", "New Year's Day", isClosed = true),
                    Holiday("01/15/$year", "Martin Luther King Jr. Day", isClosed = true),
                    Holiday("02/19/$year", "Presidents' Day", isClosed = true),
                    Holiday("05/27/$year", "Memorial Day", isClosed = true),
                    Holiday("06/19/$year", "Juneteenth", isClosed = true),
                    Holiday("07/04/$year", "Independence Day", isClosed = true),
                    Holiday("09/02/$year", "Labor Day", isClosed = true),
                    Holiday("11/11/$year", "Veterans Day", isClosed = true),
                    Holiday("11/28/$year", "Thanksgiving Day", isClosed = true),
                    Holiday("12/25/$year", "Christmas Day", isClosed = true)
                )
            )
            saveData(context)
        }
    }

    fun saveData(context: Context) {
        val prefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("business_hours", gson.toJson(businessHours))
            putString("holidays", gson.toJson(holidays))
            putString("breaks", gson.toJson(breaks))
            apply()
        }
    }

    private fun loadData(context: Context) {
        businessHours.clear()
        holidays.clear()
        breaks.clear()

        val prefs = context.getSharedPreferences(getUserPrefsName(), Context.MODE_PRIVATE)
        val bhJson = prefs.getString("business_hours", null)
        val hJson = prefs.getString("holidays", null)
        val bJson = prefs.getString("breaks", null)

        val bhType = object : TypeToken<MutableList<BusinessHours>>() {}.type
        val hType = object : TypeToken<MutableList<Holiday>>() {}.type
        val bType = object : TypeToken<MutableList<Break>>() {}.type

        bhJson?.let { businessHours = gson.fromJson(it, bhType) }
        hJson?.let { holidays = gson.fromJson(it, hType) }
        bJson?.let { breaks = gson.fromJson(it, bType) }
    }

    fun isDayOpen(date: String): Boolean {
        val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val parsedDate = try { sdfDate.parse(date) } catch (e: Exception) { null } ?: return true
        calendar.time = parsedDate
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val holiday = holidays.find { it.date == date }
        if (holiday != null && holiday.isClosed) return false

        val hours = businessHours.find { it.dayOfWeek == dayOfWeek }
        if (hours != null && hours.isClosed) return false

        return true
    }

    enum class ConflictType {
        NONE,
        EXISTING_APPOINTMENT,
        OUTSIDE_WORKING_HOURS,
        HOLIDAY_OR_BREAK
    }

    data class ConflictInfo(
        val type: ConflictType,
        val details: String = ""
    )

    fun checkConflict(date: String, timeString: String, durationMinutes: Int, excludeApptId: String? = null): ConflictInfo {
        val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())

        val calendar = Calendar.getInstance()
        val parsedDate = try { sdfDate.parse(date) } catch (e: Exception) { null } ?: return ConflictInfo(ConflictType.NONE)
        calendar.time = parsedDate
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 1. Check Holiday
        val holiday = holidays.find { it.date == date }
        if (holiday != null && holiday.isClosed) {
            return ConflictInfo(ConflictType.HOLIDAY_OR_BREAK, "Business is closed for ${holiday.name}.")
        }

        // 2. Check Business Hours
        val hours = businessHours.find { it.dayOfWeek == dayOfWeek }
        if (hours == null || hours.isClosed) {
            return ConflictInfo(ConflictType.OUTSIDE_WORKING_HOURS, "Business is closed on this day of the week.")
        }

        val startWork = try { sdfTime.parse(hours.startTime) } catch (e: Exception) { null }
        val endWork = try { sdfTime.parse(hours.endTime) } catch (e: Exception) { null }
        val selectedTime = try { sdfTime.parse(timeString) } catch (e: Exception) { null }

        if (startWork == null || endWork == null || selectedTime == null) {
            return ConflictInfo(ConflictType.NONE)
        }

        val workStartCal = Calendar.getInstance().apply { time = startWork }
        val workEndCal = Calendar.getInstance().apply { time = endWork }
        val selectedStartCal = Calendar.getInstance().apply { time = selectedTime }
        val selectedEndCal = Calendar.getInstance().apply {
            time = selectedTime
            add(Calendar.MINUTE, durationMinutes)
        }

        fun normalize(cal: Calendar) {
            cal.set(Calendar.YEAR, 2000)
            cal.set(Calendar.MONTH, 0)
            cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        normalize(workStartCal)
        normalize(workEndCal)
        normalize(selectedStartCal)
        normalize(selectedEndCal)

        if (selectedStartCal.before(workStartCal) || selectedEndCal.after(workEndCal)) {
            return ConflictInfo(ConflictType.OUTSIDE_WORKING_HOURS, "Selected time falls outside business hours (${hours.startTime} - ${hours.endTime}).")
        }

        // 3. Check Breaks
        for (b in breaks) {
            val breakStart = try { sdfTime.parse(b.startTime) } catch (e: Exception) { null } ?: continue
            val breakEnd = try { sdfTime.parse(b.endTime) } catch (e: Exception) { null } ?: continue
            val bStartCal = Calendar.getInstance().apply { time = breakStart }
            val bEndCal = Calendar.getInstance().apply { time = breakEnd }
            normalize(bStartCal)
            normalize(bEndCal)

            if (selectedStartCal.before(bEndCal) && selectedEndCal.after(bStartCal)) {
                return ConflictInfo(ConflictType.HOLIDAY_OR_BREAK, "Time slot overlaps with break: ${b.name} (${b.startTime} - ${b.endTime}).")
            }
        }

        // 4. Check Existing Appointments (Duration overlap check)
        val sameDayAppts = AppointmentManager.appointments.filter { it.date == date && it.id != excludeApptId }
        for (appt in sameDayAppts) {
            val apptStart = try { sdfTime.parse(appt.time) } catch (e: Exception) { null } ?: continue
            val apptService = ServiceManager.services.find { it.name.equals(appt.serviceName, ignoreCase = true) }
            val apptDuration = apptService?.durationMinutes ?: 30

            val apptStartCal = Calendar.getInstance().apply { time = apptStart }
            val apptEndCal = Calendar.getInstance().apply {
                time = apptStart
                add(Calendar.MINUTE, apptDuration)
            }
            normalize(apptStartCal)
            normalize(apptEndCal)

            if (selectedStartCal.before(apptEndCal) && selectedEndCal.after(apptStartCal)) {
                val nextAvailableTime = sdfTime.format(apptEndCal.time)
                return ConflictInfo(
                    ConflictType.EXISTING_APPOINTMENT,
                    "Time slot conflicts with an existing appointment for ${appt.clientName} (${appt.serviceName} at ${appt.time}). Next available slot is likely at $nextAvailableTime."
                )
            }
        }

        return ConflictInfo(ConflictType.NONE)
    }

    fun isTimeAvailable(date: String, timeString: String, durationMinutes: Int): Boolean {
        return checkConflict(date, timeString, durationMinutes).type == ConflictType.NONE
    }
}
