package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QuranDatabase
import com.example.data.repository.AdhkarRepository
import com.example.domain.TajweedParser
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
    fun `verified Quran dataset seeds 114 surahs and 6236 ayahs with matching sha256`() = runTest {
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

            val fatiha1 = db.quranDao().getAyahByRef(1, 1)!!
            assertFalse(fatiha1.textUthmani.startsWith("\uFEFF"))
            assertTrue(fatiha1.textSomali.isNotBlank())
            assertTrue(fatiha1.textEnglish.isNotBlank())
            assertTrue(fatiha1.tafsirMuyassar.isNotBlank())
            assertEquals(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                TajweedParser.stripTags(fatiha1.textTajweed)
            )

            val kursi = db.quranDao().getAyahByRef(2, 255)!!
            assertEquals(262, kursi.globalNumber)

            val nas6 = db.quranDao().getAyahByRef(114, 6)!!
            assertEquals(6236, nas6.globalNumber)
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
