package com.nuur.muslim

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.nuur.muslim.data.local.AdhkarItem
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.SurahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.data.repository.AdhkarRepository
import com.nuur.muslim.data.repository.SearchResults
import com.nuur.muslim.domain.CuratedCity
import com.nuur.muslim.localization.AppLanguage
import com.nuur.muslim.localization.stringsFor
import com.nuur.muslim.ui.screens.SearchScreen
import com.nuur.muslim.ui.screens.SettingsScreen
import com.nuur.muslim.ui.screens.StudyHubScreen
import com.nuur.muslim.ui.screens.ToolsScreen
import com.nuur.muslim.ui.theme.AppThemeMode
import com.nuur.muslim.ui.theme.NuurQuranTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EndToEndFeatureSuiteTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `location section omits coordinates and world cities picker selects Tokyo`() {
        val strings = stringsFor(AppLanguage.SOMALI)
        val settings = UserSettings(language = AppLanguage.SOMALI)
        var pickedCity: CuratedCity? = null

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    ToolsScreen(
                        initialTab = 0,
                        settings = settings,
                        strings = strings,
                        adhkarList = emptyList(),
                        adhkarCounters = emptyMap(),
                        quranicDuas = emptyList(),
                        deviceAzimuth = 0f,
                        hasCompassSensor = true,
                        locationFeedback = null,
                        onSelectCity = { pickedCity = it },
                        onSelectMethod = {},
                        onSelectTimezone = {},
                        onRequestDeviceLocation = {},
                        onLocationDenied = {},
                        onStartCompass = {},
                        onStopCompass = {},
                        onIncrementDhikr = { _, _ -> },
                        onOpenReader = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("enter_custom_coords_button").assertDoesNotExist()
        composeRule.onNodeWithTag("prayer_active_location_pill").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("city_search_input").assertIsDisplayed().performTextInput("Tokyo")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("city_option_tokyo").assertIsDisplayed().performClick()
        assertEquals("tokyo", pickedCity?.id)
        assertEquals("Asia/Tokyo", pickedCity?.timezoneId)
    }

    @Test
    fun `tools screen switches cleanly across all five worship tabs`() {
        val strings = stringsFor(AppLanguage.SOMALI)
        val settings = UserSettings(language = AppLanguage.SOMALI)
        val sampleAdhkar = listOf(
            AdhkarItem(
                order = 1,
                type = 1,
                arabic = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ",
                english = "We have reached the morning and the dominion belongs to Allah.",
                transliteration = "Asbahna wa asbahal-mulku lillah",
                count = 1,
                countDescAr = "مرة واحدة",
                countDescEn = "Once",
                fadlAr = "",
                fadlEn = "",
                sourceAr = "مسلم",
                sourceEn = "Muslim"
            )
        )
        val sampleDuas = listOf(
            AyahEntity(
                globalNumber = 208,
                surahNumber = 2,
                numberInSurah = 201,
                textUthmani = "رَبَّنَآ ءَاتِنَا فِى ٱلدُّنْيَا حَسَنَةً",
                textTajweed = "رَبَّنَآ ءَاتِنَا فِى ٱلدُّنْيَا حَسَنَةً",
                textSomali = "Rabbiyow adduunyada wanaag nagu sii",
                textEnglish = "Our Lord, give us in this world good",
                tafsirMuyassar = "",
                juz = 2,
                manzil = 1,
                page = 31,
                ruku = 26,
                hizbQuarter = 13,
                sajdah = false,
                searchArabicNormalized = "ربنا اتنا في الدنيا حسنة"
            )
        )

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                Box(modifier = Modifier.size(390.dp, 844.dp)) {
                    ToolsScreen(
                        initialTab = 0,
                        settings = settings,
                        strings = strings,
                        adhkarList = sampleAdhkar,
                        adhkarCounters = emptyMap(),
                        quranicDuas = sampleDuas,
                        deviceAzimuth = 45f,
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
                        onOpenReader = { _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tools_tab_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("qibla_calibration_card").assertIsDisplayed()

        composeRule.onNodeWithTag("tools_tab_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ").assertIsDisplayed()

        composeRule.onNodeWithTag("tools_tab_3").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tasbih_tap_button").assertIsDisplayed()

        composeRule.onNodeWithTag("tools_tab_4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("رَبَّنَآ ءَاتِنَا فِى ٱلدُّنْيَا حَسَنَةً").assertIsDisplayed()
    }

    @Test
    fun `search screen and study hub screen render and trigger navigation callbacks`() {
        val strings = stringsFor(AppLanguage.SOMALI)
        val surah = SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 7)
        var openedSurah = 0

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.LIGHT) {
                SearchScreen(
                    query = "Faatiha",
                    results = SearchResults(surahs = listOf(surah), ayahs = emptyList()),
                    isSearching = false,
                    strings = strings,
                    onQueryChange = {},
                    onOpenReader = { s, _ -> openedSurah = s }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Al-Faatiha").assertIsDisplayed().performClick()
        assertEquals(1, openedSurah)
    }

    @Test
    fun `study hub screen displays empty states cleanly`() {
        val strings = stringsFor(AppLanguage.ENGLISH)
        val settings = UserSettings(language = AppLanguage.ENGLISH)
        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                StudyHubScreen(
                    settings = settings,
                    strings = strings,
                    surahs = emptyList(),
                    bookmarks = emptyList(),
                    notes = emptyList(),
                    readingPlans = emptyList(),
                    downloadedCounts = emptyMap(),
                    onOpenReader = { _, _ -> },
                    onRemoveBookmark = { _, _, _ -> },
                    onSaveEditedNote = { _, _, _ -> },
                    onDeleteNote = {},
                    onCreatePlan = { _, _ -> },
                    onDeletePlan = {},
                    onResetKhatam = {},
                    onDeleteOfflineSurah = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(strings.noBookmarksYet).assertIsDisplayed()
    }

    @Test
    fun `settings screen triggers location settings shortcut when clicked`() {
        val strings = stringsFor(AppLanguage.SOMALI)
        val settings = UserSettings(language = AppLanguage.SOMALI)
        var openedLocation = false

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
                        onReciterChange = {},
                        onOpenLocationSettings = { openedLocation = true }
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lazy_list")
            .performScrollToNode(hasTestTag("settings_location_card"))
        composeRule.onNodeWithTag("settings_location_card").assertIsDisplayed().performClick()
        assertTrue(openedLocation)
    }

    @Test
    fun `bundled adhkar repository loads verified dataset cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val items = AdhkarRepository(context).loadAdhkar()
        assertTrue("Expected bundled adhkar items", items.size >= 30)
    }
}
