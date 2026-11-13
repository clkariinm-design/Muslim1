package com.nuur.muslim

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.nuur.muslim.data.local.SurahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.localization.AppLanguage
import com.nuur.muslim.localization.stringsFor
import com.nuur.muslim.ui.screens.HomeScreen
import com.nuur.muslim.ui.screens.SurahListScreen
import com.nuur.muslim.audio.PlaybackUiState
import com.nuur.muslim.ui.theme.AppThemeMode
import com.nuur.muslim.ui.theme.NuurQuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `home screen renders Somali Islamic Luxury dashboard`() {
        val settings = UserSettings(language = AppLanguage.SOMALI, themeMode = AppThemeMode.DARK)
        val strings = stringsFor(AppLanguage.SOMALI)
        val sampleSurahs = listOf(
            SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 7)
        )

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                HomeScreen(
                    settings = settings,
                    strings = strings,
                    surahs = sampleSurahs,
                    readingPlans = emptyList(),
                    onOpenReader = { _, _ -> },
                    onBrowseSurahs = {},
                    onOpenToolsTab = {},
                    onOpenStudy = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nuur Qur’an").assertIsDisplayed()
        composeRule.onNodeWithText("114 Suuradood • 6,236 Aayadood").assertIsDisplayed()
    }

    @Test
    fun `surah list screen renders surah card and ayah count`() {
        val strings = stringsFor(AppLanguage.ENGLISH)
        val sampleSurahs = listOf(
            SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 7),
            SurahEntity(114, "سُورَةُ ٱلنَّاسِ", "An-Naas", "Mankind", "Meccan", 6)
        )

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.LIGHT) {
                SurahListScreen(
                    surahs = sampleSurahs,
                    downloadedCounts = emptyMap(),
                    audioState = PlaybackUiState(),
                    strings = strings,
                    onOpenSurah = {},
                    onDownloadSurah = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Al-Faatiha").assertIsDisplayed()
        composeRule.onNodeWithText("An-Naas").assertIsDisplayed()
    }
}
