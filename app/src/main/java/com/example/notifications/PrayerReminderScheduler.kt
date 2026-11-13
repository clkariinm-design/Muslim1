package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserSettings
import com.example.domain.PrayerTimesCalculator
import com.example.localization.AppLanguage
import com.example.localization.stringsFor

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer Time"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Nuur Qur’an"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        PrayerReminderScheduler.ensureChannel(context, manager)

        val openIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PrayerReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(if (prayerTime.isNotBlank()) "$prayerName • $prayerTime" else prayerName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_TITLE = "extra_title"
        private const val NOTIFICATION_ID = 4102
    }
}

object PrayerReminderScheduler {
    const val CHANNEL_ID = "nuur_prayer_reminders"
    private const val REQUEST_CODE = 4100

    fun ensureChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Optional local reminders for daily prayer times"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun syncSchedule(context: Context, settings: UserSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!settings.prayerNotificationsEnabled) {
            cancel(context, alarmManager)
            return
        }
        val next = PrayerTimesCalculator.calculateNextPrayerCountdown(
            latitude = settings.customLatitude,
            longitude = settings.customLongitude,
            timezoneId = settings.timezoneId,
            method = settings.calculationMethod,
            asrMadhhab = settings.asrMadhhab
        )
        val strings = stringsFor(settings.language)
        val prayerLabel = when (next.prayerIndex) {
            0 -> strings.fajrLabel
            1 -> strings.sunriseLabel
            2 -> strings.dhuhrLabel
            3 -> strings.asrLabel
            4 -> strings.maghribLabel
            else -> strings.ishaLabel
        }
        val title = when (settings.language) {
            AppLanguage.SOMALI -> "Waqtiga Salaadda • $prayerLabel"
            AppLanguage.ARABIC -> "موعد الصلاة • $prayerLabel"
            AppLanguage.ENGLISH -> "Prayer Reminder • $prayerLabel"
        }

        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(PrayerReminderReceiver.EXTRA_TITLE, title)
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_NAME, prayerLabel)
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_TIME, next.timeHHmm)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + next.remainingMillis.coerceAtLeast(60_000L)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    private fun cancel(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, PrayerReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }
}
