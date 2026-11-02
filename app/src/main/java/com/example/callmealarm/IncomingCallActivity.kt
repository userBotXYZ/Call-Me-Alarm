package com.example.callmealarm

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class IncomingCallActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unlockAndTurnScreenOn()
        setContentView(R.layout.activity_incoming_call)

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Scheduled Event"
        val phone = intent.getStringExtra(AlarmScheduler.EXTRA_PHONE) ?: ""
        val eventHour = intent.getIntExtra(AlarmScheduler.EXTRA_EVENT_HOUR, 9)
        val eventMin = intent.getIntExtra(AlarmScheduler.EXTRA_EVENT_MIN, 0)
        val days = intent.getIntArrayExtra(AlarmScheduler.EXTRA_DAYS) ?: intArrayOf()
        val intervalMins = intent.getIntExtra(AlarmScheduler.EXTRA_INTERVAL, 0)
        val autoDial = intent.getBooleanExtra(AlarmScheduler.EXTRA_AUTO_DIAL, true)

        findViewById<TextView>(R.id.tvCallerName).text = title
        findViewById<TextView>(R.id.tvPhoneNumber).text =
            if (phone.isNotEmpty()) phone else "30-Minute Pre-Event Call"
        findViewById<TextView>(R.id.tvEventSubtext).text =
            String.format("Event starts at %02d:%02d (30 mins from now)", eventHour, eventMin)

        startRingtoneAndVibration()

        // ANSWER BUTTON -> Places direct phone call if number is provided
        findViewById<Button>(R.id.btnAnswerCall).setOnClickListener {
            stopRingtoneAndVibration()
            dismissNotification()

            if (phone.isNotBlank() && autoDial) {
                placeDirectPhoneCall(phone)
            }
            finish()
        }

        // REJECT BUTTON -> Stops ringtone and cancels any pending 5/10m repeat for today
        findViewById<Button>(R.id.btnRejectCall).setOnClickListener {
            stopRingtoneAndVibration()
            dismissNotification()
            if (days.isNotEmpty()) {
                AlarmScheduler.scheduleNextPreEventCall(
                    this, title, phone, eventHour, eventMin, days, 0, autoDial
                )
            } else {
                AlarmScheduler.cancelAlarm(this)
            }
            Toast.makeText(this, "Call Rejected", Toast.LENGTH_SHORT).show()
            finish()
        }

        // CALL AFTER 5 MINUTES BUTTON
        findViewById<Button>(R.id.btnSnooze5).setOnClickListener {
            stopRingtoneAndVibration()
            dismissNotification()
            AlarmScheduler.scheduleFollowUpCall(
                this, 5, title, phone, eventHour, eventMin, days, intervalMins, autoDial
            )
            Toast.makeText(this, "Will call again in 5 minutes", Toast.LENGTH_SHORT).show()
            finish()
        }

        // CALL AFTER 10 MINUTES BUTTON
        findViewById<Button>(R.id.btnSnooze10).setOnClickListener {
            stopRingtoneAndVibration()
            dismissNotification()
            AlarmScheduler.scheduleFollowUpCall(
                this, 10, title, phone, eventHour, eventMin, days, intervalMins, autoDial
            )
            Toast.makeText(this, "Will call again in 10 minutes", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun placeDirectPhoneCall(phoneNumber: String) {
        val uri = Uri.parse("tel:${Uri.encode(phoneNumber)}")
        val hasCallPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        // ACTION_CALL dials immediately with zero taps; fallback to ACTION_DIAL if permission denied
        val callIntent = Intent(
            if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            uri
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(callIntent)
    }

    private fun startRingtoneAndVibration() {
        try {
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopRingtoneAndVibration() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun dismissNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(AlarmReceiver.NOTIFICATION_ID)
    }

    private fun unlockAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    override fun onDestroy() {
        stopRingtoneAndVibration()
        super.onDestroy()
    }
}
