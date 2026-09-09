package com.gaiazeye.businessscheduler

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gaiazeye.businessscheduler.R
import java.text.SimpleDateFormat
import java.util.*

class create_appointment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_appointment, container, false)

        view.findViewById<View>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val clientAutocomplete = view.findViewById<AutoCompleteTextView>(R.id.clientAutocomplete)
        val serviceAutocomplete = view.findViewById<AutoCompleteTextView>(R.id.serviceAutocomplete)
        val dateEdit = view.findViewById<TextInputEditText>(R.id.dateEditText)
        val timeEdit = view.findViewById<TextInputEditText>(R.id.timeEditText)
        val timeInputLayout = view.findViewById<TextInputLayout>(R.id.timeInputLayout)
        val priceEdit = view.findViewById<TextInputEditText>(R.id.priceEditText)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveAppointmentButton)

        fun findNextAvailableSlot(date: String, requestedTime: String?, serviceDuration: Int): String? {
            val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
            val todayStr = sdfDate.format(Date())

            val calendar = Calendar.getInstance()
            val parsedDate = try { sdfDate.parse(date) } catch (e: Exception) { null } ?: return null
            calendar.time = parsedDate
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            val hours = AvailabilityManager.businessHours.find { it.dayOfWeek == dayOfWeek }
            if (hours == null || hours.isClosed) return null

            val openTime = try { sdfTime.parse(hours.startTime) } catch (e: Exception) { null } ?: return null
            val closeTime = try { sdfTime.parse(hours.endTime) } catch (e: Exception) { null } ?: return null

            val startSearchCal = Calendar.getInstance().apply { time = openTime }
            val endSearchCal = Calendar.getInstance().apply { time = closeTime }

            fun normalize(c: Calendar) {
                c.set(Calendar.YEAR, 2000)
                c.set(Calendar.MONTH, 0)
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)
            }
            normalize(startSearchCal)
            normalize(endSearchCal)

            val currentCandidate = Calendar.getInstance().apply { time = startSearchCal.time }

            // If date is today: begin intervals directly from current time (if after opening time)
            if (date == todayStr) {
                val now = Calendar.getInstance()
                val nowTimeCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                }
                normalize(nowTimeCal)

                if (nowTimeCal.after(currentCandidate)) {
                    currentCandidate.time = nowTimeCal.time
                }
            }

            // If requestedTime is provided: start searching from requestedTime
            if (!requestedTime.isNullOrEmpty()) {
                val reqParsed = try { sdfTime.parse(requestedTime) } catch (e: Exception) { null }
                if (reqParsed != null) {
                    val reqCal = Calendar.getInstance().apply { time = reqParsed }
                    normalize(reqCal)
                    if (reqCal.after(currentCandidate)) {
                        currentCandidate.time = reqCal.time
                    }
                }
            }

            // Search loop up to closing time
            while (!currentCandidate.after(endSearchCal)) {
                val candidateStr = sdfTime.format(currentCandidate.time)
                val conflict = AvailabilityManager.checkConflict(date, candidateStr, serviceDuration)

                if (conflict.type == AvailabilityManager.ConflictType.NONE) {
                    return candidateStr
                }
                currentCandidate.add(Calendar.MINUTE, 15)
            }

            return null
        }

        fun autofillNextAvailableTime(date: String, duration: Int) {
            val nextAvailable = findNextAvailableSlot(date, null, duration)
            if (nextAvailable != null) {
                timeEdit.setText(nextAvailable)
            } else if (timeEdit.text.isNullOrEmpty()) {
                timeEdit.setText("09:00 AM")
            }
        }

        fun validateAndAutoCorrectAvailability() {
            val date = dateEdit.text.toString()
            val time = timeEdit.text.toString()
            val serviceName = serviceAutocomplete.text.toString()

            if (date.isEmpty() || time.isEmpty()) return

            val sdf = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault())
            try {
                val selectedDateTime = sdf.parse("$date $time")
                if (selectedDateTime != null && selectedDateTime.before(Date())) {
                    timeInputLayout.error = "Cannot book appointments in the past!"
                    return
                }
            } catch (e: Exception) {}

            val service = ServiceManager.services.find { it.name.equals(serviceName, ignoreCase = true) }
            val duration = service?.durationMinutes ?: 30

            val conflict = AvailabilityManager.checkConflict(date, time, duration)
            if (conflict.type != AvailabilityManager.ConflictType.NONE) {
                val nextAvailable = findNextAvailableSlot(date, time, duration)
                if (nextAvailable != null && nextAvailable != time) {
                    timeEdit.setText(nextAvailable)
                    timeInputLayout.error = null
                    Toast.makeText(
                        requireContext(),
                        "Selected time ($time) was unavailable. Automatically updated to next available time ($nextAvailable).",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    timeInputLayout.error = "Time Slot Unavailable"
                }
            } else {
                timeInputLayout.error = null
            }
        }

        priceEdit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val text = priceEdit.text.toString()
                if (text == "0.0" || text == "0") {
                    priceEdit.setText("")
                }
            }
        }

        // Validate Date Autofill from Calendar
        val sdfDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val todayStr = sdfDate.format(Date())
        val todayStart = try { sdfDate.parse(todayStr) } catch (e: Exception) { null }

        var initialDate = AppointmentManager.selectedDateForAppointment ?: todayStr
        val parsedInitial = try { sdfDate.parse(initialDate) } catch (e: Exception) { null }

        if (parsedInitial != null && todayStart != null && parsedInitial.before(todayStart)) {
            Toast.makeText(requireContext(), "Appointments cannot be made for past dates. Date reset to today ($todayStr).", Toast.LENGTH_LONG).show()
            initialDate = todayStr
            AppointmentManager.selectedDateForAppointment = todayStr
        }

        dateEdit.setText(initialDate)
        autofillNextAvailableTime(initialDate, 30)

        // Client Selection
        val clientNames = ClientManager.clients.map { it.name }
        val clientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, clientNames)
        clientAutocomplete.setAdapter(clientAdapter)

        // Service Selection
        val serviceNames = ServiceManager.services.map { it.name }
        val serviceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, serviceNames)
        serviceAutocomplete.setAdapter(serviceAdapter)

        serviceAutocomplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedServiceName = parent.getItemAtPosition(position) as String
            val service = ServiceManager.services.find { it.name == selectedServiceName }
            service?.let {
                priceEdit.setText(String.format(Locale.getDefault(), "%.2f", it.price))
                val date = dateEdit.text.toString()
                if (date.isNotEmpty()) {
                    autofillNextAvailableTime(date, it.durationMinutes)
                }
            }
            validateAndAutoCorrectAvailability()
        }

        serviceAutocomplete.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = serviceAutocomplete.text.toString()
                val service = ServiceManager.services.find { it.name.equals(input, ignoreCase = true) }
                if (service != null && priceEdit.text.isNullOrEmpty()) {
                    priceEdit.setText(String.format(Locale.getDefault(), "%.2f", service.price))
                }
                validateAndAutoCorrectAvailability()
            }
        }

        val calendar = Calendar.getInstance()

        dateEdit.setOnClickListener {
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
                val selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", month + 1, day, year)
                dateEdit.setText(selectedDate)
                val service = ServiceManager.services.find { it.name.equals(serviceAutocomplete.text.toString(), ignoreCase = true) }
                val duration = service?.durationMinutes ?: 30
                autofillNextAvailableTime(selectedDate, duration)
                validateAndAutoCorrectAvailability()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        timeEdit.setOnClickListener {
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                timeEdit.setText(String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm))
                validateAndAutoCorrectAvailability()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        fun saveAppointmentDirectly(client: String, service: String, date: String, time: String, price: Double) {
            val newAppointment = Appointment(
                id = UUID.randomUUID().toString(),
                clientName = client,
                serviceName = service,
                date = date,
                time = time,
                price = price
            )
            AppointmentManager.appointments.add(newAppointment)
            AppointmentManager.sortAppointments()
            AppointmentManager.saveAppointments(requireContext())
            Toast.makeText(requireContext(), "Appointment Saved!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        saveButton.setOnClickListener {
            val client = clientAutocomplete.text.toString()
            val service = serviceAutocomplete.text.toString()
            val date = dateEdit.text.toString()
            val time = timeEdit.text.toString()
            val priceStr = priceEdit.text.toString()

            if (client.isEmpty() || service.isEmpty() || date.isEmpty() || time.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0

            // Validation: Past date/time
            val sdf = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault())
            try {
                val selectedDateTime = sdf.parse("$date $time")
                if (selectedDateTime != null && selectedDateTime.before(Date())) {
                    Toast.makeText(requireContext(), "Cannot book appointments on past dates or times!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (e: Exception) {}

            val serviceObj = ServiceManager.services.find { it.name.equals(service, ignoreCase = true) }
            val duration = serviceObj?.durationMinutes ?: 30

            val conflict = AvailabilityManager.checkConflict(date, time, duration)
            if (conflict.type != AvailabilityManager.ConflictType.NONE) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Time Slot Conflict")
                    .setMessage("${conflict.details}\n\nDo you want to continue making this appointment?")
                    .setPositiveButton("Yes, Book Anyway") { _, _ ->
                        saveAppointmentDirectly(client, service, date, time, price)
                    }
                    .setNegativeButton("Change Time", null)
                    .show()
            } else {
                saveAppointmentDirectly(client, service, date, time, price)
            }
        }

        return view
    }
}
