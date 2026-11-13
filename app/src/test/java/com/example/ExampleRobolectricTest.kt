package com.nuur.muslim

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nuur.muslim.data.local.QuranDatabase
import com.nuur.muslim.data.repository.AdhkarRepository
import com.nuur.muslim.data.repository.QuranRepository
import com.nuur.muslim.domain.TajweedParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Nuur Quran`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nuur Qur’an", appName)
    }

    @Test
    fun `verified Quran dataset seeds all 114 surahs and 6236 ayahs cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val manifest = QuranDatabase.ensureSeededAndVerified(context, db)
            assertEquals(114, manifest.surahCount)
            assertEquals(6236, manifest.ayahCount)
            assertEquals(114, db.quranDao().getSurahCount())
            assertEquals(6236, db.quranDao().getAyahCount())

            var expectedGlobal = 1
            for (surahNum in 1..114) {
                val surah = db.quranDao().getSurahByNumber(surahNum)!!
                val ayahs = db.quranDao().getAyahsBySurahSync(surahNum)
                assertEquals("Surah $surahNum ayah count mismatch", surah.ayahCount, ayahs.size)
                ayahs.forEachIndexed { idx, ayah ->
                    assertEquals(expectedGlobal, ayah.globalNumber)
                    assertEquals(idx + 1, ayah.numberInSurah)
                    assertFalse(ayah.textUthmani.startsWith("\uFEFF"))
                    assertTrue(ayah.textUthmani.isNotBlank())
                    assertTrue(ayah.textSomali.isNotBlank())
                    assertTrue(ayah.textEnglish.isNotBlank())
                    assertTrue(ayah.tafsirMuyassar.isNotBlank())
                    assertTrue(ayah.juz in 1..30)
                    assertTrue(ayah.page in 1..604)
                    assertTrue(TajweedParser.stripTags(ayah.textTajweed).isNotBlank())
                    expectedGlobal++
                }
            }
            assertEquals(6237, expectedGlobal)
        } finally {
            db.close()
        }
    }

    @Test
    fun `repository supports surah-ayah lookup, bookmark toggle and note CRUD`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            QuranDatabase.ensureSeededAndVerified(context, db)
            val repo = QuranRepository(context, db)

            val refSearch = repo.search("2:255")
            assertEquals(1, refSearch.surahs.size)
            assertEquals(2, refSearch.surahs.first().number)
            assertEquals(1, refSearch.ayahs.size)
            val kursi = refSearch.ayahs.first()
            assertEquals(262, kursi.globalNumber)

            assertTrue(repo.toggleBookmark(kursi))
            assertTrue(262 in repo.observeBookmarkedGlobalNumbers().first())
            repo.removeBookmarkByGlobalAyah(262)
            assertFalse(262 in repo.observeBookmarkedGlobalNumbers().first())

            repo.saveNoteByRef(
                existingId = 0L,
                globalAyahNumber = 262,
                surahNumber = 2,
                ayahNumber = 255,
                title = "Ayat al-Kursi",
                body = "Tawhid reflection"
            )
            val notes = repo.observeNotesWithAyah().first()
            assertEquals(1, notes.size)
            assertEquals("Ayat al-Kursi", notes.first().note.title)

            repo.deleteNote(notes.first().note.id)
            assertTrue(repo.observeNotesWithAyah().first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `verified Adhkar dataset loads and passes sha256 verification`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = AdhkarRepository(context)
        val items = repo.loadAdhkar()
        assertEquals(34, items.size)
        assertTrue(items.all { it.arabic.isNotBlank() && it.english.isNotBlank() && it.count >= 1 })
    }
}
