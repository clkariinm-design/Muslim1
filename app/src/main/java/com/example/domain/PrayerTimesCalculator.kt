package com.example.domain

import android.os.Build
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

enum class CalculationMethod(
    val id: String,
    val nameSo: String,
    val nameAr: String,
    val nameEn: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    MWL("mwl", "Raabidada Caalamka Islaamka (MWL)", "رابطة العالم الإسلامي", "Muslim World League", 18.0, 17.0),
    UMM_AL_QURA("umm_al_qura", "Ummu al-Quraa (Makka)", "أم القرى", "Umm al-Qura (Makkah)", 18.5, null, 90),
    EGYPT("egypt", "Masar (Egyptian Authority)", "الهيئة المصرية العامة للمساحة", "Egyptian General Authority", 19.5, 17.5),
    KARACHI("karachi", "Karachi (Hanafi/Shafi)", "جامعة العلوم الإسلامية بكراتشي", "Univ. of Islamic Sciences, Karachi", 18.0, 18.0),
    ISNA("isna", "ISNA (Waqooyiga Ameerika)", "الجمعية الإسلامية لأمريكا الشمالية", "ISNA (North America)", 15.0, 15.0);

    companion object {
        fun fromId(id: String): CalculationMethod = entries.firstOrNull { it.id == id } ?: MWL
    }
}

data class CuratedCity(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String
)

object CuratedCities {
    val cities: List<CuratedCity> = listOf(
        CuratedCity("mogadishu", "Muqdisho / Mogadishu", "Soomaaliya", 2.0469, 45.3182, "Africa/Mogadishu"),
        CuratedCity("hargeisa", "Hargeysa / Hargeisa", "Somaliland / Soomaaliya", 9.5624, 44.0770, "Africa/Mogadishu"),
        CuratedCity("garowe", "Garoowe / Garowe", "Soomaaliya", 8.4020, 48.4845, "Africa/Mogadishu"),
        CuratedCity("bosaso", "Boosaaso / Bosaso", "Soomaaliya", 11.2842, 49.1816, "Africa/Mogadishu"),
        CuratedCity("kismayo", "Kismaayo / Kismayo", "Soomaaliya", -0.3582, 42.5454, "Africa/Mogadishu"),
        CuratedCity("djibouti", "Jabuuti / Djibouti", "Djibouti", 11.5721, 43.1456, "Africa/Djibouti"),
        CuratedCity("nairobi", "Nairobi", "Kenya", -1.2921, 36.8219, "Africa/Nairobi"),
        CuratedCity("addis", "Addis Ababa", "Ethiopia", 9.0320, 38.7469, "Africa/Addis_Ababa"),
        CuratedCity("makkah", "Makka al-Mukarrama", "Saudi Arabia", 21.4225, 39.8262, "Asia/Riyadh"),
        CuratedCity("madinah", "Madiina al-Munawwara", "Saudi Arabia", 24.4672, 39.6112, "Asia/Riyadh"),
        CuratedCity("riyadh", "Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh"),
        CuratedCity("cairo", "Cairo / Qaahira", "Egypt", 30.0444, 31.2357, "Africa/Cairo"),
        CuratedCity("istanbul", "Istanbul", "Türkiye", 41.0082, 28.9784, "Europe/Istanbul"),
        CuratedCity("london", "London", "United Kingdom", 51.5072, -0.1276, "Europe/London"),
        CuratedCity("minneapolis", "Minneapolis", "USA", 44.9778, -93.2650, "America/Chicago"),
        CuratedCity("toronto", "Toronto", "Canada", 43.6532, -79.3832, "America/Toronto"),
        CuratedCity("kuala_lumpur", "Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur")
    )

    val defaultCity: CuratedCity = cities.first()

    fun findById(id: String): CuratedCity = cities.firstOrNull { it.id == id } ?: defaultCity
}

data class DailyPrayerTimes(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val nextPrayerIndex: Int // 0=Fajr..5=Isha
)

data class HijriDateDisplay(
    val day: Int,
    val monthNumber: Int,
    val monthNameAr: String,
    val monthNameLatin: String,
    val year: Int
)

object PrayerTimesCalculator {

    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262

    private val hijriMonthsAr = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val hijriMonthsLatin = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    /**
     * Great-circle initial bearing in degrees [0, 360) from given coordinates to the Kaaba.
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLambda = Math.toRadians(KAABA_LON - longitude)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg + 360.0) % 360.0
    }

    /**
     * Astronomical solar calculation for daily prayer times using NOAA/PrayTimes equations.
     * Results are local standard/DST clock hours formatted HH:mm.
     */
    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        method: CalculationMethod,
        epochMillis: Long = System.currentTimeMillis()
    ): DailyPrayerTimes {
        val tz = TimeZone.getTimeZone(timezoneId)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzHours = tz.getOffset(epochMillis) / 3_600_000.0
        val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd + 0.5)

        val dhuhr = fixHour(12.0 + tzHours - longitude / 15.0 - eqTime)
        val sunrise = dhuhr - hourAngle(0.833, latitude, declination)
        val maghrib = dhuhr + hourAngle(0.833, latitude, declination)
        val fajr = dhuhr - hourAngle(method.fajrAngle, latitude, declination)
        val asr = dhuhr + asrHourAngle(1.0, latitude, declination) // Shafi/Jumhoor shadow = 1x
        val isha = if (method.ishaMinutesAfterMaghrib != null) {
            maghrib + method.ishaMinutesAfterMaghrib / 60.0
        } else {
            dhuhr + hourAngle(method.ishaAngle ?: 17.0, latitude, declination)
        }

        val hours = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
        val nextIdx = hours.indexOfFirst { !it.isNaN() && it > currentHour }.let { if (it == -1) 0 else it }

        return DailyPrayerTimes(
            fajr = formatHour(fajr),
            sunrise = formatHour(sunrise),
            dhuhr = formatHour(dhuhr),
            asr = formatHour(asr),
            maghrib = formatHour(maghrib),
            isha = formatHour(isha),
            nextPrayerIndex = nextIdx
        )
    }

    fun currentHijriDate(timezoneId: String, epochMillis: Long = System.currentTimeMillis()): HijriDateDisplay {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val zone = runCatching { ZoneId.of(timezoneId) }.getOrElse { ZoneId.systemDefault() }
            val localDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
            val hijrah = HijrahChronology.INSTANCE.date(localDate)
            val m = hijrah.get(ChronoField.MONTH_OF_YEAR)
            val d = hijrah.get(ChronoField.DAY_OF_MONTH)
            val y = hijrah.get(ChronoField.YEAR_OF_ERA)
            HijriDateDisplay(
                day = d,
                monthNumber = m,
                monthNameAr = hijriMonthsAr.getOrElse(m - 1) { "" },
                monthNameLatin = hijriMonthsLatin.getOrElse(m - 1) { "" },
                year = y
            )
        } else {
            val cal = Calendar.getInstance(TimeZone.getTimeZone(timezoneId)).apply { timeInMillis = epochMillis }
            val jd = floor(julianDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)) + 0.5)
            val l0 = jd - 1948440.0 + 10632.0
            val n = floor((l0 - 1.0) / 10631.0)
            val l1 = l0 - 10631.0 * n + 354.0
            val j = floor((10985.0 - l1) / 5316.0) * floor((50.0 * l1) / 17719.0) +
                floor(l1 / 5670.0) * floor((43.0 * l1) / 15238.0)
            val l2 = l1 - floor((30.0 - j) / 15.0) * floor((17719.0 * j) / 50.0) -
                floor(j / 16.0) * floor((15238.0 * j) / 43.0) + 29.0
            val m = floor((24.0 * l2) / 709.0).toInt().coerceIn(1, 12)
            val d = (l2 - floor((709.0 * m) / 24.0)).toInt().coerceIn(1, 30)
            val y = (30.0 * n + j - 30.0).toInt()
            HijriDateDisplay(
                day = d,
                monthNumber = m,
                monthNameAr = hijriMonthsAr.getOrElse(m - 1) { "" },
                monthNameLatin = hijriMonthsLatin.getOrElse(m - 1) { "" },
                year = y
            )
        }
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716.0)) + floor(30.6001 * (m + 1.0)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = Math.toDegrees(asin(dsin(e) * dsin(l)))
        return decl to eqt
    }

    private fun hourAngle(angle: Double, lat: Double, decl: Double): Double {
        val num = -dsin(angle) - dsin(lat) * dsin(decl)
        val den = dcos(lat) * dcos(decl)
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(ratio)) / 15.0
    }

    private fun asrHourAngle(factor: Double, lat: Double, decl: Double): Double {
        val angle = -Math.toDegrees(atan(1.0 / (factor + tan(Math.toRadians(abs(lat - decl))))))
        return hourAngle(angle, lat, decl)
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double): Double = a - 24.0 * floor(a / 24.0)

    private fun formatHour(hourFloat: Double): String {
        if (hourFloat.isNaN() || hourFloat.isInfinite()) return "--:--"
        val normalized = fixHour(hourFloat + 0.5 / 60.0)
        val h = floor(normalized).toInt().coerceIn(0, 23)
        val m = floor((normalized - h) * 60.0).toInt().coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }
}
