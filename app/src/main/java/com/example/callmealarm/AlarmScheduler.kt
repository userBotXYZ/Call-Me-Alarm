package com.example.callmealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_TITLE = "EXTRA_TITLE"
    const val EXTRA_PHONE = "EXTRA_PHONE"
    const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
    const val EXTRA_EVENT_HOUR = "EXTRA_EVENT_HOUR"
    const val EXTRA_EVENT_MIN = "EXTRA_EVENT_MIN"
    const val EXTRA_DAYS = "EXTRA_DAYS"
    const val EXTRA_AUTO_DIAL = "EXTRA_AUTO_DIAL"

    private const val PRE_EVENT_OFFSET_MS = 30 * 60 * 1000L // 30 minutes before event
    private const val ALARM_REQ_CODE = 1001

    /**
     * Finds the next selected day of the week, subtracts 30 minutes from the event time,
     * and schedules a Doze-piercing AlarmClock intent.
     */
    fun scheduleNextPreEventCall(
        context: Context,
        title: String,
        phone: String,
        eventHour: Int,
        eventMinute: Int,
        selectedDays: IntArray, // Calendar.SUNDAY (1) .. Calendar.SATURDAY (7)
        repeatIntervalMins: Int, // 0, 5, or 10
        autoDialOnAnswer: Boolean
    ): Long {
        val now = System.currentTimeMillis()
        var bestTriggerMillis = Long.MAX_VALUE

        for (dayOfWeek in selectedDays) {
            val eventCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, eventHour)
                set(Calendar.MINUTE, eventMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Advance to the target day of the week
            val currentDay = eventCal.get(Calendar.DAY_OF_WEEK)
            val daysDiff = (dayOfWeek - currentDay + 7) % 7
            eventCal.add(Calendar.DAY_OF_YEAR, daysDiff)

            // Subtract 30 minutes for the pre-event call
            var callTriggerMillis = eventCal.timeInMillis - PRE_EVENT_OFFSET_MS

            // If 30-min-prior time has already passed today, push to next week
            if (callTriggerMillis <= now) {
                callTriggerMillis += 7 * 24 * 60 * 60 * 1000L
            }

            if (callTriggerMillis < bestTriggerMillis) {
                bestTriggerMillis = callTriggerMillis
            }
        }

        setExactAlarm(
            context = context,
            triggerAtMillis = bestTriggerMillis,
            title = title,
            phone = phone,
            eventHour = eventHour,
            eventMinute = eventMinute,
            selectedDays = selectedDays,
            repeatIntervalMins = repeatIntervalMins,
            autoDialOnAnswer = autoDialOnAnswer
        )

        return bestTriggerMillis
    }

    /**
     * Schedules a 5-minute or 10-minute follow-up call.
     */
    fun scheduleFollowUpCall(
        context: Context,
        delayMinutes: Int,
        title: String,
        phone: String,
        eventHour: Int,
        eventMinute: Int,
        selectedDays: IntArray,
        repeatIntervalMins: Int,
        autoDialOnAnswer: Boolean
    ): Long {
        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        setExactAlarm(
            context = context,
            triggerAtMillis = triggerAtMillis,
            title = title,
            phone = phone,
            eventHour = eventHour,
            eventMinute = eventMinute,
            selectedDays = selectedDays,
            repeatIntervalMins = repeatIntervalMins,
            autoDialOnAnswer = autoDialOnAnswer
        )
        return triggerAtMillis
    }

    private fun setExactAlarm(
        context: Context,
        triggerAtMillis: Long,
        title: String,
        phone: String,
        eventHour: Int,
        eventMinute: Int,
        selectedDays: IntArray,
        repeatIntervalMins: Int,
        autoDialOnAnswer: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PHONE, phone)
            putExtra(EXTRA_EVENT_HOUR, eventHour)
            putExtra(EXTRA_EVENT_MIN, eventMinute)
            putExtra(EXTRA_DAYS, selectedDays)
            putExtra(EXTRA_INTERVAL, repeatIntervalMins)
            putExtra(EXTRA_AUTO_DIAL, autoDialOnAnswer)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setAlarmClock wakes the device out of deep Doze and grants full-screen launch privileges
        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(clockInfo, pendingIntent)
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
