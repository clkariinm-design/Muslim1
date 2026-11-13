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
import com.example.data.local.SettingsRepository
import com.example.data.local.UserSettings
import com.example.domain.PrayerTimesCalculator
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer Time"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Nuur Qur’an"
        val stopLabel = intent.getStringExtra(EXTRA_STOP_LABEL) ?: "Stop Adhan"
        val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "Adhan is playing"

        // 1) Start full Adhan Foreground Service; fall back to high-priority notification if OS restricts FGS
        val startedService = runCatching {
            AdhanPlaybackService.startAdhan(
                context = context,
                title = title,
                prayerName = prayerName,
                prayerTime = prayerTime,
                stopLabel = stopLabel,
                statusText = statusText
            )
        }.isSuccess

        if (!startedService) {
            postFallbackNotification(context, title, prayerName, prayerTime)
        }

        // 2) Automatically refresh and chain all 5 daily prayers for the next 24h cycle
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context.applicationContext).settingsFlow.first()
                PrayerReminderScheduler.syncSchedule(
                    context = context.applicationContext,
                    settings = settings,
                    minDelayMillis = 70_000L
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postFallbackNotification(
        context: Context,
        title: String,
        prayerName: String,
        prayerTime: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        PrayerReminderScheduler.ensureFallbackChannel(manager)

        val openIntent = PendingIntent.getActivity(
            context,
            101,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PrayerReminderScheduler.FALLBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_notification)
            .setContentTitle(title)
            .setContentText(if (prayerTime.isNotBlank()) "$prayerName • $prayerTime" else prayerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_STOP_LABEL = "extra_stop_label"
        const val EXTRA_STATUS_TEXT = "extra_status_text"
        private const val NOTIFICATION_ID = 4102
    }
}

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val appCtx = context.applicationContext
                    val settings = SettingsRepository(appCtx).settingsFlow.first()
                    PrayerReminderScheduler.syncSchedule(appCtx, settings)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

object PrayerReminderScheduler {
    const val FALLBACK_CHANNEL_ID = "nuur_prayer_reminders_v2"
    private const val REQUEST_CODE_BASE = 4100
    private const val SHOW_INTENT_REQUEST_CODE = 4120
    private val OBLIGATORY_INDICES = listOf(0, 2, 3, 4, 5)

    fun ensureFallbackChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FALLBACK_CHANNEL_ID,
                "Prayer Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily prayer time reminders"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun syncSchedule(
        context: Context,
        settings: UserSettings,
        minDelayMillis: Long = 5_000L
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!settings.prayerNotificationsEnabled) {
            cancel(context, alarmManager)
            return
        }

        val upcomingPrayers = PrayerTimesCalculator.calculateAllUpcomingObligatoryPrayers(
            latitude = settings.customLatitude,
            longitude = settings.customLongitude,
            timezoneId = settings.timezoneId,
            method = settings.calculationMethod,
            asrMadhhab = settings.asrMadhhab,
            minDelayMillis = minDelayMillis
        )
        val strings = stringsFor(settings.language)
        val useExact = canScheduleExactAlarms(context)
        val now = System.currentTimeMillis()

        upcomingPrayers.forEach { prayer ->
            val prayerLabel = when (prayer.prayerIndex) {
                0 -> strings.fajrLabel
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
                putExtra(PrayerReminderReceiver.EXTRA_PRAYER_TIME, prayer.timeHHmm)
                putExtra(PrayerReminderReceiver.EXTRA_STOP_LABEL, strings.adhanStopPlay)
                putExtra(PrayerReminderReceiver.EXTRA_STATUS_TEXT, strings.adhanPlayingText)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + prayer.prayerIndex,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = prayer.triggerAtMillis.coerceAtLeast(now + minDelayMillis)
            if (useExact) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    SHOW_INTENT_REQUEST_CODE,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                runCatching {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                        pending
                    )
                }.onFailure {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }

    private fun cancel(context: Context, alarmManager: AlarmManager) {
        OBLIGATORY_INDICES.forEach { idx ->
            val intent = Intent(context, PrayerReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BASE + idx,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }
}
