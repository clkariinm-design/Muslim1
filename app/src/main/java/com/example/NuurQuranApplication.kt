package com.example

import android.app.Application
import com.example.audio.AudioPlaybackManager
import com.example.data.local.QuranDatabase
import com.example.data.local.SettingsRepository
import com.example.data.repository.AdhkarRepository
import com.example.data.repository.QuranRepository

class NuurQuranApplication : Application() {

    val database: QuranDatabase by lazy { QuranDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val quranRepository: QuranRepository by lazy { QuranRepository(this, database) }
    val adhkarRepository: AdhkarRepository by lazy { AdhkarRepository(this) }
    val audioPlaybackManager: AudioPlaybackManager by lazy {
        AudioPlaybackManager(this, database.audioDownloadDao())
    }
}
