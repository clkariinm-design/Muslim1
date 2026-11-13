package com.example

import com.example.data.local.UserSettings
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCities
import com.example.domain.PrayerTimesCalculator
import com.example.domain.RecitersCatalog
import com.example.domain.TajweedParser
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            assertTrue(s.adhanPickAudio.isNotBlank())
            assertTrue(s.adhanStopPlay.isNotBlank())
        }
    }

    @Test
    fun `next obligatory prayer skips sunrise and chains cleanly across all five prayers`() {
        var cursor = 1762905600000L // 2025-11-12T00:00:00Z
        val seenIndices = mutableListOf<Int>()
        repeat(6) {
            val next = PrayerTimesCalculator.calculateNextObligatoryPrayer(
                latitude = 2.0469,
                longitude = 45.3182,
                timezoneId = "Africa/Mogadishu",
                method = CalculationMethod.MWL,
                epochMillis = cursor,
                minDelayMillis = 70_000L
            )
            assertTrue("Sunrise (index 1) must be skipped for Adhan alarms", next.prayerIndex != 1)
            assertTrue(next.prayerIndex in listOf(0, 2, 3, 4, 5))
            assertTrue("Trigger must be strictly in the future", next.triggerAtMillis >= cursor + 70_000L)
            seenIndices.add(next.prayerIndex)
            cursor = next.triggerAtMillis
        }
        assertEquals(listOf(0, 2, 3, 4, 5, 0), seenIndices)
    }

    @Test
    fun `all upcoming obligatory prayers returns all five daily prayers excluding sunrise`() {
        val now = 1762934400000L // 2025-11-12T08:00:00Z (11:00 Mogadishu)
        val prayers = PrayerTimesCalculator.calculateAllUpcomingObligatoryPrayers(
            latitude = 2.0469,
            longitude = 45.3182,
            timezoneId = "Africa/Mogadishu",
            method = CalculationMethod.MWL,
            epochMillis = now,
            minDelayMillis = 5_000L
        )
        assertEquals(5, prayers.size)
        assertEquals(setOf(0, 2, 3, 4, 5), prayers.map { it.prayerIndex }.toSet())
        assertTrue(prayers.none { it.prayerIndex == 1 })
        assertTrue(prayers.all { it.triggerAtMillis >= now + 5_000L })
        assertTrue(prayers.zipWithNext().all { (a, b) -> a.triggerAtMillis < b.triggerAtMillis })
    }

    @Test
    fun `curated cities and active location display name resolve accurately`() {
        assertTrue(CuratedCities.cities.size >= 150)
        assertEquals(CuratedCities.cities.size, CuratedCities.cities.map { it.id }.toSet().size)
        assertEquals(
            com.example.domain.WorldRegion.entries.toSet(),
            CuratedCities.cities.map { it.region }.toSet()
        )

        val nearHargeisa = CuratedCities.findNearestCity(9.55, 44.08)
        assertEquals("hargeisa", nearHargeisa?.id)

        val midOcean = CuratedCities.findNearestCity(0.0, -30.0)
        assertNull(midOcean)

        val labeledGps = UserSettings(
            useCustomGpsCoords = true,
            customLatitude = 9.55,
            customLongitude = 44.08,
            customLocationLabel = "Hargeysa, Somalia"
        )
        assertEquals("Hargeysa, Somalia", labeledGps.activeLocationDisplayName())

        val nearestFallbackGps = UserSettings(
            useCustomGpsCoords = true,
            customLatitude = 2.05,
            customLongitude = 45.32,
            customLocationLabel = null
        )
        assertTrue(nearestFallbackGps.activeLocationDisplayName().contains("Muqdisho"))

        val farGpsNeverShowsRawCoords = UserSettings(
            useCustomGpsCoords = true,
            customLatitude = 0.0,
            customLongitude = -30.0,
            customLocationLabel = null
        )
        assertTrue(!farGpsNeverShowsRawCoords.activeLocationDisplayName().contains("GPS ("))

        assertEquals(
            "Europe/Stockholm",
            com.example.domain.WorldCitiesCatalog.inferTimezoneId("SE", 59.33, 18.06)
        )
        assertEquals(
            "America/Chicago",
            com.example.domain.WorldCitiesCatalog.inferTimezoneId("US", 44.97, -93.26)
        )
    }
}
