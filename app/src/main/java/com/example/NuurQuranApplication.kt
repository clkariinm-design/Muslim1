package com.nuur.muslim

import android.app.Application
import com.nuur.muslim.audio.AudioPlaybackManager
import com.nuur.muslim.data.local.QuranDatabase
import com.nuur.muslim.data.local.SettingsRepository
import com.nuur.muslim.data.repository.AdhkarRepository
import com.nuur.muslim.data.repository.QuranRepository
import com.nuur.muslim.notifications.PrayerReminderScheduler
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
