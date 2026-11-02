package com.example.callmealarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "INCOMING_CALL_ALARM_CHANNEL"
        const val NOTIFICATION_ID = 999
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Upcoming Event"
        val phone = intent.getStringExtra(AlarmScheduler.EXTRA_PHONE) ?: ""
        val eventHour = intent.getIntExtra(AlarmScheduler.EXTRA_EVENT_HOUR, 9)
        val eventMin = intent.getIntExtra(AlarmScheduler.EXTRA_EVENT_MIN, 0)
        val days = intent.getIntArrayExtra(AlarmScheduler.EXTRA_DAYS) ?: intArrayOf()
        val intervalMins = intent.getIntExtra(AlarmScheduler.EXTRA_INTERVAL, 0)
        val autoDial = intent.getBooleanExtra(AlarmScheduler.EXTRA_AUTO_DIAL, true)

        // If user selected automatic 5 or 10 min repeat during setup, queue the follow-up automatically
        if (intervalMins == 5 || intervalMins == 10) {
            AlarmScheduler.scheduleFollowUpCall(
                context, intervalMins, title, phone, eventHour, eventMin, days, intervalMins, autoDial
            )
        } else if (days.isNotEmpty()) {
            // Otherwise queue the next scheduled day of the week
            AlarmScheduler.scheduleNextPreEventCall(
                context, title, phone, eventHour, eventMin, days, intervalMins, autoDial
            )
        }

        val callIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtras(intent)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incoming Alarm Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen incoming call reminder 30 minutes before event"
                setSound(null, null) // Ringtone handled directly by IncomingCallActivity
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming Reminder Call: $title")
            .setContentText("Event in 30 mins • Tap to Answer ($phone)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Start activity directly for immediate foreground presentation
        context.startActivity(callIntent)
    }
}
