package com.nuur.muslim.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nuur.muslim.MainActivity
import com.nuur.muslim.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopPlaybackAndService() }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        ) {
            stopPlaybackAndService()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ADHAN -> {
                stopPlaybackAndService()
                return START_NOT_STICKY
            }
            ACTION_START_ADHAN, null -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Nuur Qur’an"
                val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer Time"
                val prayerTime = intent?.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
                val stopLabel = intent?.getStringExtra(EXTRA_STOP_LABEL) ?: "Stop Adhan"
                val statusText = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: "Adhan is playing"

                val notification = buildForegroundNotification(
                    title = title,
                    prayerName = prayerName,
                    prayerTime = prayerTime,
                    stopLabel = stopLabel,
                    statusText = statusText
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startAdhanAudio()
                return START_NOT_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun buildForegroundNotification(
        title: String,
        prayerName: String,
        prayerTime: String,
        stopLabel: String,
        statusText: String
    ): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensurePlaybackChannel(manager)

        val openIntent = PendingIntent.getActivity(
            this,
            201,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            202,
            Intent(this, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP_ADHAN
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = buildString {
            if (prayerTime.isNotBlank()) {
                append(prayerTime)
                append(" • ")
            } else if (prayerName.isNotBlank()) {
                append(prayerName)
                append(" • ")
            }
            append(statusText)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, stopLabel, stopIntent)
            .setDeleteIntent(stopIntent)
            .build()
    }

    private fun startAdhanAudio() {
        releasePlayerOnly()
        if (!requestAudioFocus()) {
            stopPlaybackAndService()
            return
        }

        val player = MediaPlayer()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(attributes)

        val prepared = configureAudioSource(player)
        if (!prepared) {
            player.release()
            stopPlaybackAndService()
            return
        }

        player.setOnCompletionListener {
            stopPlaybackAndService()
        }
        player.setOnErrorListener { _, _, _ ->
            stopPlaybackAndService()
            true
        }

        runCatching {
            player.prepare()
            player.start()
            mediaPlayer = player
            _isPlaying.value = true
            mainHandler.removeCallbacks(autoStopRunnable)
            mainHandler.postDelayed(autoStopRunnable, MAX_PLAYBACK_MS)
        }.onFailure {
            player.release()
            stopPlaybackAndService()
        }
    }

    private fun configureAudioSource(player: MediaPlayer): Boolean {
        val downloadedFile = AdhanAudioRepository.cachedAdhanFile(this)
        if (downloadedFile.exists() && downloadedFile.length() > 0L) {
            val ok = runCatching {
                player.setDataSource(downloadedFile.absolutePath)
            }.isSuccess
            if (ok) return true
            player.reset()
        }

        val rawResId = bundledAdhanResId(this)
        if (rawResId != 0) {
            val ok = runCatching {
                resources.openRawResourceFd(rawResId)?.use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } ?: run {
                    val uri = Uri.parse("android.resource://$packageName/$rawResId")
                    player.setDataSource(this, uri)
                }
            }.isSuccess
            if (ok) return true
            player.reset()
        }

        val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: return false

        return runCatching {
            player.setDataSource(this, fallbackUri)
        }.isSuccess
    }

    private fun requestAudioFocus(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        audioManager = am
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusChangeListener)
        }
        audioFocusRequest = null
    }

    private fun releasePlayerOnly() {
        mainHandler.removeCallbacks(autoStopRunnable)
        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
            }
            runCatching { mp.release() }
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    private fun stopPlaybackAndService() {
        releasePlayerOnly()
        abandonAudioFocus()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayerOnly()
        abandonAudioFocus()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "nuur_adhan_playback_v2"
        private const val NOTIFICATION_ID = 4105
        private const val MAX_PLAYBACK_MS = 5 * 60 * 1000L

        const val ACTION_START_ADHAN = "com.nuur.muslim.notifications.ACTION_START_ADHAN"
        const val ACTION_STOP_ADHAN = "com.nuur.muslim.notifications.ACTION_STOP_ADHAN"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_STOP_LABEL = "extra_stop_label"
        const val EXTRA_STATUS_TEXT = "extra_status_text"

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        fun ensurePlaybackChannel(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Adhan Audio Playback",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Foreground notification with Stop control while Adhan is playing"
                    setSound(null, null)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }

        fun bundledAdhanResId(context: Context): Int =
            context.resources.getIdentifier("adhan", "raw", context.packageName)

        fun hasBundledAdhan(context: Context): Boolean =
            AdhanAudioRepository.cachedAdhanFile(context).let { it.isFile && it.length() > 0L } ||
                bundledAdhanResId(context) != 0

        fun startAdhan(
            context: Context,
            title: String,
            prayerName: String,
            prayerTime: String,
            stopLabel: String,
            statusText: String
        ) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START_ADHAN
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
                putExtra(EXTRA_STOP_LABEL, stopLabel)
                putExtra(EXTRA_STATUS_TEXT, statusText)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAdhan(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP_ADHAN
            }
            runCatching {
                context.startService(intent)
            }
        }
    }
}
