package com.example.callmealarm

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestRequiredPermissions()

        val etTitle = findViewById<EditText>(R.id.etEventTitle)
        val etPhone = findViewById<EditText>(R.id.etPhoneNumber)
        val timePicker = findViewById<TimePicker>(R.id.timePickerEvent).apply {
            setIs24HourView(false)
        }
        val cbAutoDial = findViewById<CheckBox>(R.id.cbAutoDial)
        val rgInterval = findViewById<RadioGroup>(R.id.rgRepeatInterval)
        val tvStatus = findViewById<TextView>(R.id.tvScheduleStatus)

        val dayCheckBoxes = mapOf(
            Calendar.MONDAY to findViewById<CheckBox>(R.id.cbMon),
            Calendar.TUESDAY to findViewById<CheckBox>(R.id.cbTue),
            Calendar.WEDNESDAY to findViewById<CheckBox>(R.id.cbWed),
            Calendar.THURSDAY to findViewById<CheckBox>(R.id.cbThu),
            Calendar.FRIDAY to findViewById<CheckBox>(R.id.cbFri),
            Calendar.SATURDAY to findViewById<CheckBox>(R.id.cbSat),
            Calendar.SUNDAY to findViewById<CheckBox>(R.id.cbSun)
        )

        findViewById<Button>(R.id.btnSaveAlarm).setOnClickListener {
            val title = etTitle.text.toString().ifBlank { "Important Event" }
            val phone = etPhone.text.toString().trim()
            val eventHour = timePicker.hour
            val eventMinute = timePicker.minute

            val selectedDays = dayCheckBoxes.filter { it.value.isChecked }.keys.toIntArray()
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Select at least one day of the week", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intervalMins = when (rgInterval.checkedRadioButtonId) {
                R.id.rbInterval5 -> 5
                R.id.rbInterval10 -> 10
                else -> 0
            }

            val nextCallMillis = AlarmScheduler.scheduleNextPreEventCall(
                context = this,
                title = title,
                phone = phone,
                eventHour = eventHour,
                eventMinute = eventMinute,
                selectedDays = selectedDays,
                repeatIntervalMins = intervalMins,
                autoDialOnAnswer = cbAutoDial.isChecked
            )

            val formatter = SimpleDateFormat("EEE, MMM d 'at' hh:mm a", Locale.getDefault())
            tvStatus.text = "Next Call Scheduled (30m before event):\n${formatter.format(Date(nextCallMillis))}"
            Toast.makeText(this, "30-Min Pre-Event Call Scheduled!", Toast.LENGTH_LONG).show()
        }

        // Instant Test Call Button so you can test the full-screen lock UI & auto-call immediately
        findViewById<Button>(R.id.btnTestCallNow).setOnClickListener {
            val title = etTitle.text.toString().ifBlank { "Test Event Call" }
            val phone = etPhone.text.toString().trim()
            AlarmScheduler.scheduleFollowUpCall(
                context = this,
                delayMinutes = 0,
                title = title,
                phone = phone,
                eventHour = timePicker.hour,
                eventMinute = timePicker.minute,
                selectedDays = intArrayOf(),
                repeatIntervalMins = 0,
                autoDialOnAnswer = cbAutoDial.isChecked
            )
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
    }
}
