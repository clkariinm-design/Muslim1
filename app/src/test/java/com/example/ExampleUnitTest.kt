package com.nuur.muslim

import com.nuur.muslim.domain.CalculationMethod
import com.nuur.muslim.domain.PrayerTimesCalculator
import com.nuur.muslim.domain.TajweedParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `tajweed parser strips rule brackets accurately`() {
        val sample = "[h:1[ٱ]للَّهِ ٱل[l[رَّ]حْمَ[n[ـٰ]نِ"
        assertEquals("ٱللَّهِ ٱلرَّحْمَـٰنِ", TajweedParser.stripTags(sample))
        val parsed = TajweedParser.parseToAnnotatedString(sample)
        assertEquals("ٱللَّهِ ٱلرَّحْمَـٰنِ", parsed.text)
    }

    @Test
    fun `qibla bearing for Mogadishu points north-northwest toward Makkah`() {
        val bearing = PrayerTimesCalculator.calculateQiblaBearing(2.0469, 45.3182)
        // Expected initial great-circle bearing from Mogadishu to Kaaba is ~344-345 deg
        assertTrue("Expected bearing in 340..350, got $bearing", bearing in 340.0..350.0)
    }

    @Test
    fun `prayer times calculation produces valid ordered HH mm strings`() {
        val result = PrayerTimesCalculator.calculatePrayerTimes(
            latitude = 2.0469,
            longitude = 45.3182,
            timezoneId = "Africa/Mogadishu",
            method = CalculationMethod.MWL,
            epochMillis = 1_767_225_600_000L // 2026-01-01T00:00:00Z
        )
        val hhmm = Regex("""^\d{2}:\d{2}$""")
        listOf(result.fajr, result.sunrise, result.dhuhr, result.asr, result.maghrib, result.isha).forEach {
            assertTrue("Invalid time format: $it", hhmm.matches(it))
        }
        assertTrue(result.fajr < result.sunrise)
        assertTrue(result.sunrise < result.dhuhr)
        assertTrue(result.dhuhr < result.asr)
        assertTrue(result.asr < result.maghrib)
        assertTrue(result.maghrib < result.isha)
    }
}
