package com.example

import com.example.domain.CalculationMethod
import com.example.domain.PrayerTimesCalculator
import com.example.domain.RecitersCatalog
import com.example.domain.TajweedParser
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerAndTajweedTest {

    @Test
    fun `tajweed parser parses tags and strips cleanly`() {
        val sample = "بِسْمِ [h:1[ٱ]للَّهِ [h:2[ٱ][l[ل]رَّحْمَ[n[ـٰ]نِ [h:3[ٱ][l[ل]رَّح[p[ِي]مِ"
        val stripped = TajweedParser.stripTags(sample)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", stripped)

        val annotated = TajweedParser.parseToAnnotatedString(sample)
        assertEquals(stripped, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }

    @Test
    fun `qibla bearing for Mogadishu and London is astronomically accurate`() {
        val mogadishuQibla = PrayerTimesCalculator.calculateQiblaBearing(2.0469, 45.3182)
        // Mogadishu is SSE of Makkah -> bearing ~342-345 deg (NNW)
        assertTrue("Expected ~343 deg, got $mogadishuQibla", mogadishuQibla in 335.0..352.0)

        val londonQibla = PrayerTimesCalculator.calculateQiblaBearing(51.5072, -0.1276)
        // London is NW of Makkah -> bearing ~118-120 deg (ESE)
        assertTrue("Expected ~119 deg, got $londonQibla", londonQibla in 115.0..124.0)
    }

    @Test
    fun `prayer times calculator produces valid HH_mm strings for all methods`() {
        val hhmm = Regex("""^\d{2}:\d{2}$""")
        CalculationMethod.entries.forEach { method ->
            val times = PrayerTimesCalculator.calculatePrayerTimes(
                latitude = 2.0469,
                longitude = 45.3182,
                timezoneId = "Africa/Mogadishu",
                method = method,
                epochMillis = 1762905600000L
            )
            assertTrue(hhmm.matches(times.fajr))
            assertTrue(hhmm.matches(times.sunrise))
            assertTrue(hhmm.matches(times.dhuhr))
            assertTrue(hhmm.matches(times.asr))
            assertTrue(hhmm.matches(times.maghrib))
            assertTrue(hhmm.matches(times.isha))
        }
    }

    @Test
    fun `all 17 tajweed rule specs and all languages and reciters are populated`() {
        assertEquals(17, TajweedParser.rules.size)
        assertEquals(7, RecitersCatalog.reciters.size)
        AppLanguage.entries.forEach { lang ->
            val s = stringsFor(lang)
            assertTrue(s.appTitle.isNotBlank())
            assertTrue(s.prayerVerificationNotice.isNotBlank())
        }
    }
}
