package com.nuur.muslim

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nuur.muslim.audio.AudioPlaybackManager
import com.nuur.muslim.audio.PlaybackUiState
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.SurahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.data.repository.QuranRepository
import com.nuur.muslim.localization.AppLanguage
import com.nuur.muslim.localization.stringsFor
import com.nuur.muslim.ui.screens.ReaderScreen
import com.nuur.muslim.ui.screens.SettingsScreen
import com.nuur.muslim.ui.theme.AppThemeMode
import com.nuur.muslim.ui.theme.NuurQuranTheme
import com.nuur.muslim.ui.viewmodel.OnlineTafsirState
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class P1FixesVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ibn kathir html entity decoder decodes named and numeric entities without altering text`() {
        val raw = "<p>Allah&#39;s mercy &amp; &ldquo;grace&rdquo; &rsquo;s &lsquo;light&rsquo;&nbsp;&mdash;&nbsp;&lt;verse&gt; &quot;peace&quot; &apos;end&apos;</p>"
        val decoded = QuranRepository.cleanTafsirHtml(raw)
        assertEquals(
            "Allah's mercy & “grace” ’s ‘light’ — <verse> \"peace\" 'end'",
            decoded
        )
    }

    @Test
    fun `audio stream writer writes full bytes atomically and cleans tmp file`() {
        val tempDir = Files.createTempDirectory("audio_stream_ok").toFile()
        try {
            val dest = File(tempDir, "001001.mp3")
            val payload = ByteArray(65_536) { index -> (index % 251).toByte() }

            AudioPlaybackManager.writeStreamAtomically(
                inputStream = ByteArrayInputStream(payload),
                dest = dest,
                checkActive = {}
            )

            assertTrue("Expected final MP3 file to exist", dest.exists())
            assertFalse("Temp file should not remain after completion", File(tempDir, "001001.mp3.tmp").exists())
            assertArrayEquals(payload, dest.readBytes())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `audio stream writer deletes tmp file and leaves dest absent on cancellation`() {
        val tempDir = Files.createTempDirectory("audio_stream_cancel").toFile()
        try {
            val dest = File(tempDir, "001002.mp3")
            val tmp = File(tempDir, "001002.mp3.tmp")
            val chunkedStream = object : InputStream() {
                private var reads = 0
                override fun read(): Int = -1
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    reads++
                    if (reads > 3) return -1
                    b.fill(0x2A, off, off + minOf(len, 4096))
                    return minOf(len, 4096)
                }
            }
            var checks = 0
            var caughtCancel = false
            try {
                AudioPlaybackManager.writeStreamAtomically(
                    inputStream = chunkedStream,
                    dest = dest,
                    checkActive = {
                        checks++
                        if (checks == 2) throw CancellationException("User cancelled download")
                    }
                )
            } catch (_: CancellationException) {
                caughtCancel = true
            }

            assertTrue("CancellationException should propagate", caughtCancel)
            assertFalse("Partial temp file must be cleaned up on cancellation", tmp.exists())
            assertFalse("Destination file must not exist after cancellation", dest.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `reader screen does not auto scroll while audio is paused and scrolls when playing`() {
        val surah = SurahEntity(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Faatiha", "The Opening", "Meccan", 30)
        val ayahs = (1..30).map { num ->
            AyahEntity(
                globalNumber = num,
                surahNumber = 1,
                numberInSurah = num,
                textUthmani = "بِسْمِ ٱللَّهِ $num",
                textTajweed = "بِسْمِ ٱللَّهِ $num",
                textSomali = "Aayad $num",
                textEnglish = "Verse $num",
                tafsirMuyassar = "Tafsir $num",
                juz = 1,
                manzil = 1,
                page = 1,
                ruku = 1,
                hizbQuarter = 1,
                sajdah = false,
                searchArabicNormalized = "بسم الله"
            )
        }
        val settings = UserSettings(language = AppLanguage.SOMALI, themeMode = AppThemeMode.DARK)
        val strings = stringsFor(AppLanguage.SOMALI)
        var audioState by mutableStateOf(
            PlaybackUiState(
                isActive = true,
                isPlaying = false,
                currentSurahNumber = 1,
                currentAyahNumber = 25,
                currentGlobalAyah = 25
            )
        )

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                Box(modifier = Modifier.size(360.dp, 640.dp)) {
                    ReaderScreen(
                        surah = surah,
                        ayahs = ayahs,
                        targetAyah = 1,
                        settings = settings,
                        strings = strings,
                        bookmarkedGlobals = emptySet(),
                        downloadedCount = 0,
                        audioState = audioState,
                        onlineTafsirState = OnlineTafsirState.Idle,
                        onBack = {},
                        onPlaySurah = { _, _, _ -> },
                        onDownloadSurah = {},
                        onCancelDownload = {},
                        onDeleteSurahAudio = {},
                        onToggleBookmark = {},
                        onSaveNote = { _, _, _ -> },
                        onMarkLastRead = {},
                        onResetTafsir = {},
                        onLoadOnlineTafsir = { _, _ -> },
                        onArabicSizeChange = {},
                        onTranslationSizeChange = {},
                        onToggleSomali = {},
                        onToggleEnglish = {},
                        onToggleTajweed = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        // When paused, reader must stay at the top and must not auto-scroll to ayah 25
        composeRule.onNodeWithTag("ayah_card_1_1").assertIsDisplayed()
        composeRule.onNodeWithTag("ayah_card_1_25").assertDoesNotExist()

        // Once playback starts, reader auto-scrolls to the active ayah 25
        audioState = audioState.copy(isPlaying = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ayah_card_1_25").assertIsDisplayed()
    }

    @Test
    fun `settings language and theme chips wrap cleanly within narrow viewport and large font scale`() {
        val settings = UserSettings(language = AppLanguage.SOMALI, themeMode = AppThemeMode.DARK)
        val strings = stringsFor(AppLanguage.SOMALI)

        composeRule.setContent {
            NuurQuranTheme(themeMode = AppThemeMode.DARK) {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = baseDensity.density, fontScale = 1.6f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .height(640.dp)
                            .testTag("narrow_settings_container")
                    ) {
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
        }

        composeRule.waitForIdle()
        val containerBounds = composeRule.onNodeWithTag("narrow_settings_container")
            .fetchSemanticsNode()
            .boundsInRoot

        val chipTags = listOf(
            "lang_chip_so",
            "lang_chip_ar",
            "lang_chip_en",
            "theme_chip_dark",
            "theme_chip_light",
            "theme_chip_system"
        )
        chipTags.forEach { tag ->
            val node = composeRule.onNodeWithTag(tag)
            node.assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("$tag should have non-zero width", bounds.width > 10f)
            assertTrue(
                "$tag right edge (${bounds.right}) should stay inside narrow container (${containerBounds.right})",
                bounds.right <= containerBounds.right + 1f
            )
            assertTrue(
                "$tag left edge (${bounds.left}) should stay inside narrow container (${containerBounds.left})",
                bounds.left >= containerBounds.left - 1f
            )
        }
    }
}
