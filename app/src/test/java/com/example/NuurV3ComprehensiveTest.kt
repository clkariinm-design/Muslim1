package com.example

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioPlaybackManager
import com.example.audio.PlaybackUiState
import com.example.data.backup.BackupManager
import com.example.data.local.BookmarkEntity
import com.example.data.local.NoteEntity
import com.example.data.local.QuranDatabase
import com.example.data.local.ReadingPlanEntity
import com.example.data.local.SettingsRepository
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.domain.AsrMadhhab
import com.example.domain.CalculationMethod
import com.example.domain.PrayerTimesCalculator
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.ui.components.FullAudioPlayerSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SurahListScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.NuurQuranTheme
import com.example.ui.theme.ReadingTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NuurV3ComprehensiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `asr madhhab hanafi calculates later asr than shafi and countdown wraps past isha`() {
        // 2026-06-15 12:00 UTC
        val middayUtc = 1781524800000L
        val shafi = PrayerTimesCalculator.calculatePrayerTimes(
            latitude = 51.5072,
            longitude = -0.1276,
            timezoneId = "Europe/London",
            method = CalculationMethod.MWL,
            epochMillis = middayUtc,
            asrMadhhab = AsrMadhhab.SHAFI
        )
        val hanafi = PrayerTimesCalculator.calculatePrayerTimes(
            latitude = 51.5072,
            longitude = -0.1276,
            timezoneId = "Europe/London",
            method = CalculationMethod.MWL,
            epochMillis = middayUtc,
            asrMadhhab = AsrMadhhab.HANAFI
        )
        assertTrue(
            "Expected Hanafi Asr (${hanafi.asr}) to be later than Shafi Asr (${shafi.asr})",
            hanafi.asr > shafi.asr
        )

        val middayCountdown = PrayerTimesCalculator.calculateNextPrayerCountdown(
            latitude = 2.0469,
            longitude = 45.3182,
            timezoneId = "Africa/Mogadishu",
            method = CalculationMethod.MWL,
            asrMadhhab = AsrMadhhab.SHAFI,
            epochMillis = middayUtc
        )
        assertTrue(Regex("""^\d{2}:\d{2}:\d{2}$""").matches(middayCountdown.formattedCountdown))
        assertTrue(middayCountdown.remainingMillis > 0L)

        // 2026-06-15 23:30 UTC+3 (20:30 UTC) — after Isha in Mogadishu, must wrap to tomorrow Fajr (0)
        val lateNightMogadishu = 1781555400000L
        val nightCountdown = PrayerTimesCalculator.calculateNextPrayerCountdown(
            latitude = 2.0469,
            longitude = 45.3182,
            timezoneId = "Africa/Mogadishu",
            method = CalculationMethod.MWL,
            asrMadhhab = AsrMadhhab.SHAFI,
            epochMillis = lateNightMogadishu
        )
        assertEquals(0, nightCountdown.prayerIndex)
        assertTrue(nightCountdown.remainingMillis > 3_600_000L)
    }

    @Test
    fun `daily progress streak logic accumulates same day increments consecutive days and resets on gap`() {
        val sameDay = SettingsRepository.computeUpdatedDailyProgress(
            storedEpochDay = 20_000L,
            storedTodayAyahs = 12,
            storedStreakDays = 4,
            storedLastStreakDay = 20_000L,
            currentEpochDay = 20_000L,
            ayahsIncrement = 5
        )
        assertEquals(17, sameDay.todayAyahsRead)
        assertEquals(4, sameDay.streakDays)

        val nextDay = SettingsRepository.computeUpdatedDailyProgress(
            storedEpochDay = 20_000L,
            storedTodayAyahs = 25,
            storedStreakDays = 4,
            storedLastStreakDay = 20_000L,
            currentEpochDay = 20_001L,
            ayahsIncrement = 8
        )
        assertEquals(8, nextDay.todayAyahsRead)
        assertEquals(5, nextDay.streakDays)

        val missedDay = SettingsRepository.computeUpdatedDailyProgress(
            storedEpochDay = 20_000L,
            storedTodayAyahs = 25,
            storedStreakDays = 9,
            storedLastStreakDay = 20_000L,
            currentEpochDay = 20_003L,
            ayahsIncrement = 3
        )
        assertEquals(3, missedDay.todayAyahsRead)
        assertEquals(1, missedDay.streakDays)
    }

    @Test
    fun `juz and hizb sql queries return all 30 juz and 60 hizb totaling 6236 ayahs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            QuranDatabase.ensureSeededAndVerified(context, db)
            val dao = db.quranDao()
            val juzList = dao.observeJuzEntries().first()
            val hizbList = dao.observeHizbEntries().first()

            assertEquals(30, juzList.size)
            assertEquals(1..30, juzList.first().juz..juzList.last().juz)
            assertEquals(6236, juzList.sumOf { it.ayahCount })
            assertEquals(1, juzList.first().startSurah)
            assertEquals(1, juzList.first().startAyah)

            assertEquals(60, hizbList.size)
            assertEquals(1..60, hizbList.first().hizb..hizbList.last().hizb)
            assertEquals(6236, hizbList.sumOf { it.ayahCount })
        } finally {
            db.close()
        }
    }

    @Test
    fun `backup manager exports and restores bookmarks notes plans and settings cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val destDb = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settingsRepo = SettingsRepository(context)
        try {
            sourceDb.userStudyDao().insertBookmark(
                BookmarkEntity(globalAyahNumber = 262, surahNumber = 2, ayahNumber = 255, label = "Kursi", createdAt = 1000L)
            )
            sourceDb.userStudyDao().insertOrUpdateNote(
                NoteEntity(globalAyahNumber = 7, surahNumber = 1, ayahNumber = 7, title = "Fatiha", body = "Guidance", updatedAt = 2000L)
            )
            sourceDb.userStudyDao().insertOrUpdatePlan(
                ReadingPlanEntity(title = "Ramadan Plan", targetDays = 30, startEpochDay = 20000L, completedAyahsCount = 120, currentGlobalAyah = 120, isActive = true)
            )
            val customSettings = UserSettings(
                language = AppLanguage.ENGLISH,
                themeMode = AppThemeMode.LIGHT,
                readingTheme = ReadingTheme.WARM_SEPIA,
                arabicFontSizeSp = 32f,
                translationFontSizeSp = 17f,
                dailyAyahGoal = 40,
                lastReadSurah = 18,
                lastReadAyah = 10,
                lastReadGlobalAyah = 2150,
                khatamAyahsRead = 2150
            )

            val json = BackupManager.exportJson(sourceDb, customSettings)
            val restoreResult = BackupManager.importJson(json, destDb, settingsRepo)
            assertTrue(restoreResult.isSuccess)
            val summary = restoreResult.getOrThrow()
            assertEquals(1, summary.bookmarksRestored)
            assertEquals(1, summary.notesRestored)
            assertEquals(1, summary.plansRestored)

            assertEquals(262, destDb.userStudyDao().getBookmarksSync().single().globalAyahNumber)
            assertEquals("Guidance", destDb.userStudyDao().getNotesSync().single().body)
            assertEquals("Ramadan Plan", destDb.userStudyDao().getPlansSync().single().title)

            val restoredSettings = settingsRepo.settingsFlow.first()
            assertEquals(AppLanguage.ENGLISH, restoredSettings.language)
            assertEquals(ReadingTheme.WARM_SEPIA, restoredSettings.readingTheme)
            assertEquals(40, restoredSettings.dailyAyahGoal)
            assertEquals(18, restoredSettings.lastReadSurah)
            assertEquals(2150, restoredSettings.khatamAyahsRead)
        } finally {
            sourceDb.close()
            destDb.close()
        }
    }

    @Test
    fun `audio playback manager updates speed and clears download state on cancel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val manager = AudioPlaybackManager(context, db.audioDownloadDao())
            manager.setPlaybackSpeed(1.25f)
            assertEquals(1.25f, manager.state.value.playbackSpeed, 0.01f)
            manager.setPlaybackSpeed(2.5f) // should clamp at 1.5f
            assertEquals(1.5f, manager.state.value.playbackSpeed, 0.01f)
            manager.cancelDownload()
            assertNull(manager.state.value.downloadingSurahNumber)
            assertTrue(
                "Bundled res/raw/adhan audio must be present",
                com.example.notifications.AdhanPlaybackService.hasBundledAdhan(context)
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `privacy policy has zero draft warnings across Somali Arabic and English`() {
        AppLanguage.entries.forEach { lang ->
            val strings = stringsFor(lang)
            assertFalse("Privacy title for $lang must not contain Draft", strings.privacyPolicyTitle.contains("Draft", ignoreCase = true))
            assertFalse("Privacy title for $lang must not contain Qabyo", strings.privacyPolicyTitle.contains("Qabyo", ignoreCase = true))
            assertFalse("Privacy title for $lang must not contain مسودة", strings.privacyPolicyTitle.contains("مسودة"))
            assertTrue(strings.privacyPolicyBody.length > 100)
        }
    }

    @Test
    fun `home screen v3 renders next prayer countdown worship overview and tasbih tile`() {
        val settings = UserSettings(
            language = AppLanguage.ENGLISH,
            dailyAyahGoal = 30,
            todayAyahsRead = 18,
            streakDays = 6,
            tasbihCount = 12,
            tasbihTarget = 33
        )
        val strings = stringsFor(AppLanguage.ENGLISH)
        val surahs = listOf(
            SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 7)
        )

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    HomeScreen(
                        settings = settings,
                        strings = strings,
                        surahs = surahs,
                        readingPlans = emptyList(),
                        onOpenReader = { _, _ -> },
                        onBrowseSurahs = {},
                        onOpenToolsTab = {},
                        onOpenStudy = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_continue_reading_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_next_prayer_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_lazy_list").performScrollToNode(hasTestTag("home_worship_overview_card"))
        composeRule.onNodeWithTag("home_worship_overview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_lazy_list").performScrollToNode(hasTestTag("quick_tasbih_tile"))
        composeRule.onNodeWithTag("quick_tasbih_tile").assertIsDisplayed()
    }

    @Test
    fun `surah list screen switches between Surah Juz and Hizb browsing modes`() {
        val strings = stringsFor(AppLanguage.ENGLISH)
        val surahs = listOf(SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 7))
        val juzList = listOf(com.example.data.local.JuzIndexEntry(1, 1, 1, 1, "Al-Faatiha", "الفاتحة", 148))
        val hizbList = listOf(com.example.data.local.HizbIndexEntry(1, 1, 1, 1, "Al-Faatiha", "الفاتحة", 74))

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                SurahListScreen(
                    surahs = surahs,
                    downloadedCounts = emptyMap(),
                    audioState = PlaybackUiState(),
                    strings = strings,
                    onOpenSurah = {},
                    onDownloadSurah = {},
                    juzEntries = juzList,
                    hizbEntries = hizbList
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mode_chip_juz").performClick()
        composeRule.onNodeWithTag("juz_card_1").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_chip_hizb").performClick()
        composeRule.onNodeWithTag("hizb_card_1").assertIsDisplayed()
    }

    @Test
    fun `tools screen renders tasbih counter and qibla calibration in Arabic RTL`() {
        val settings = UserSettings(language = AppLanguage.ARABIC, tasbihCount = 15, tasbihTarget = 33)
        val strings = stringsFor(AppLanguage.ARABIC)
        var tapped = false

        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                    Box(modifier = Modifier.size(390.dp, 844.dp)) {
                        ToolsScreen(
                            initialTab = 3,
                            settings = settings,
                            strings = strings,
                            adhkarList = emptyList(),
                            adhkarCounters = emptyMap(),
                            quranicDuas = emptyList(),
                            deviceAzimuth = 120f,
                            hasCompassSensor = true,
                            locationFeedback = null,
                            onSelectCity = {},
                            onSelectMethod = {},
                            onSelectTimezone = {},
                            onRequestDeviceLocation = {},
                            onLocationDenied = {},
                            onStartCompass = {},
                            onStopCompass = {},
                            onIncrementDhikr = { _, _ -> },
                            onOpenReader = { _, _ -> },
                            compassAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW,
                            onIncrementTasbih = { tapped = true }
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_tap_button").assertIsDisplayed().performClick()
        assertTrue(tapped)
        composeRule.onNodeWithTag("tools_tab_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("qibla_calibration_card").assertIsDisplayed()
    }

    @Test
    fun `settings screen shows backup buttons and privacy policy card`() {
        val strings = stringsFor(AppLanguage.ENGLISH)
        val settings = UserSettings(language = AppLanguage.ENGLISH)

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    SettingsScreen(
                        settings = settings,
                        strings = strings,
                        manifest = null,
                        onLanguageChange = {},
                        onThemeChange = {},
                        onArabicSizeChange = {},
                        onTranslationSizeChange = {},
                        onToggleSomali = {},
                        onToggleEnglish = {},
                        onToggleTajweed = {},
                        onReciterChange = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lazy_list").performScrollToNode(hasTestTag("export_backup_button"))
        composeRule.onNodeWithTag("export_backup_button").assertIsDisplayed()
        composeRule.onNodeWithTag("import_backup_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_lazy_list").performScrollToNode(hasTestTag("privacy_policy_card"))
        composeRule.onNodeWithTag("privacy_policy_card").assertIsDisplayed()
        composeRule.onNodeWithText(strings.privacyPolicyTitle).assertIsDisplayed()
    }

    @Test
    fun `full audio player sheet triggers playback speed change on chip click`() {
        val strings = stringsFor(AppLanguage.ENGLISH)
        var selectedSpeed = 1.0f
        val sampleSurah = SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 7)

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                FullAudioPlayerSheet(
                    audioState = PlaybackUiState(
                        isActive = true,
                        isPlaying = true,
                        currentSurahNumber = 1,
                        currentSurahName = "Al-Faatiha",
                        currentAyahNumber = 1,
                        currentGlobalAyah = 1,
                        playbackSpeed = selectedSpeed
                    ),
                    surah = sampleSurah,
                    downloadedCount = 0,
                    strings = strings,
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeek = {},
                    onCycleRepeat = {},
                    onSpeedChange = { selectedSpeed = it },
                    onReciterChange = {},
                    onDownloadSurah = {},
                    onCancelDownload = {},
                    onOpenReader = { _, _ -> },
                    onDismiss = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("speed_chip_1.25").performScrollTo().performClick()
        assertEquals(1.25f, selectedSpeed, 0.01f)
    }
}
