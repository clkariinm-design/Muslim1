package com.example

import android.app.Application
import com.example.audio.AudioPlaybackManager
import com.example.data.local.QuranDatabase
import com.example.data.local.SettingsRepository
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.QuranRepository
import com.example.notifications.PrayerReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NuurQuranApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: QuranDatabase by lazy { QuranDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val quranRepository: QuranRepository by lazy { QuranRepository(this, database) }
    val adhkarRepository: AdhkarRepository by lazy { AdhkarRepository(this) }
    val audioPlaybackManager: AudioPlaybackManager by lazy {
        AudioPlaybackManager(this, database.audioDownloadDao())
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching {
                val settings = settingsRepository.settingsFlow.first()
                if (settings.prayerNotificationsEnabled) {
                    PrayerReminderScheduler.syncSchedule(this@NuurQuranApplication, settings)
                }
            }
        }
    }
}
