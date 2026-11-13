package com.nuur.muslim.domain

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

enum class AsrMadhhab(
    val id: String,
    val shadowFactor: Double,
    val nameSo: String,
    val nameAr: String,
    val nameEn: String
) {
    SHAFI("shafi", 1.0, "Shaafici / Jumhuur (1x)", "الشافعي / الجمهور", "Shafi'i / Standard (1x)"),
    HANAFI("hanafi", 2.0, "Xanafi (2x)", "الحنفي", "Hanafi (2x)");

    companion object {
        fun fromId(id: String): AsrMadhhab = entries.firstOrNull { it.id == id } ?: SHAFI
    }
}

data class CuratedCity(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val region: WorldRegion = WorldRegion.SOMALIA_HORN,
    val countryCode: String = ""
)

object CuratedCities {
    val cities: List<CuratedCity>
        get() = WorldCitiesCatalog.cities

    val defaultCity: CuratedCity
        get() = cities.first()

    fun findById(id: String): CuratedCity = cities.firstOrNull { it.id == id } ?: defaultCity

    fun findNearestCity(latitude: Double, longitude: Double, maxDistanceKm: Double = 75.0): CuratedCity? {
        var bestCity: CuratedCity? = null
        var bestDist = Double.MAX_VALUE
        for (city in cities) {
            val dist = distanceKm(latitude, longitude, city.latitude, city.longitude)
            if (dist <= maxDistanceKm && dist < bestDist) {
                bestDist = dist
                bestCity = city
            }
        }
        return bestCity
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2.0) * sin(dLon / 2.0)
        val c = 2.0 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt((1.0 - a).coerceAtLeast(0.0)))
        return r * c
    }
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

data class NextPrayerCountdown(
    val prayerIndex: Int,
    val timeHHmm: String,
    val remainingMillis: Long,
    val formattedCountdown: String
)

data class NextObligatoryPrayer(
    val prayerIndex: Int, // 0=Fajr, 2=Dhuhr, 3=Asr, 4=Maghrib, 5=Isha (skips Sunrise)
    val timeHHmm: String,
    val triggerAtMillis: Long,
    val remainingMillis: Long
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
        epochMillis: Long = System.currentTimeMillis(),
        asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI
    ): DailyPrayerTimes {
        val tz = TimeZone.getTimeZone(timezoneId)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val tzHours = tz.getOffset(epochMillis) / 3_600_000.0
        val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd + 0.5)

        val dhuhr = fixHour(12.0 + tzHours - longitude / 15.0 - eqTime)
        val sunrise = dhuhr - hourAngle(0.833, latitude, declination)
        val maghrib = dhuhr + hourAngle(0.833, latitude, declination)
        val fajr = dhuhr - hourAngle(method.fajrAngle, latitude, declination)
        val asr = dhuhr + asrHourAngle(asrMadhhab.shadowFactor, latitude, declination)
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

    fun calculateNextPrayerCountdown(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        method: CalculationMethod,
        asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI,
        epochMillis: Long = System.currentTimeMillis()
    ): NextPrayerCountdown {
        val tz = TimeZone.getTimeZone(timezoneId)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
        val todayTimes = calculatePrayerTimes(latitude, longitude, timezoneId, method, epochMillis, asrMadhhab)
        val nowSecondsOfDay = cal.get(Calendar.HOUR_OF_DAY) * 3600L +
            cal.get(Calendar.MINUTE) * 60L +
            cal.get(Calendar.SECOND)

        val prayerStrings = listOf(
            todayTimes.fajr, todayTimes.sunrise, todayTimes.dhuhr,
            todayTimes.asr, todayTimes.maghrib, todayTimes.isha
        )
        for (idx in prayerStrings.indices) {
            val targetSec = parseHHmmSeconds(prayerStrings[idx]) ?: continue
            if (targetSec > nowSecondsOfDay) {
                val diffSec = targetSec - nowSecondsOfDay
                return NextPrayerCountdown(
                    prayerIndex = idx,
                    timeHHmm = prayerStrings[idx],
                    remainingMillis = diffSec * 1000L,
                    formattedCountdown = formatCountdown(diffSec)
                )
            }
        }
        // Past Isha: target tomorrow's Fajr
        val tomorrowTimes = calculatePrayerTimes(
            latitude, longitude, timezoneId, method, epochMillis + 86_400_000L, asrMadhhab
        )
        val fajrSec = parseHHmmSeconds(tomorrowTimes.fajr) ?: 0L
        val diffSec = ((86_400L - nowSecondsOfDay) + fajrSec).coerceAtLeast(0L)
        return NextPrayerCountdown(
            prayerIndex = 0,
            timeHHmm = tomorrowTimes.fajr,
            remainingMillis = diffSec * 1000L,
            formattedCountdown = formatCountdown(diffSec)
        )
    }

    /**
     * Finds the next obligatory prayer (Fajr, Dhuhr, Asr, Maghrib, Isha — skipping Sunrise)
     * strictly at least [minDelayMillis] in the future, returning its exact epochMillis trigger time.
     */
    fun calculateNextObligatoryPrayer(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        method: CalculationMethod,
        asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI,
        epochMillis: Long = System.currentTimeMillis(),
        minDelayMillis: Long = 5_000L
    ): NextObligatoryPrayer {
        val tz = TimeZone.getTimeZone(timezoneId)
        val minTriggerMillis = epochMillis + minDelayMillis.coerceAtLeast(1_000L)

        // Check today (dayOffset = 0) and tomorrow (dayOffset = 1)
        for (dayOffset in 0..1) {
            val dayBase = Calendar.getInstance(tz).apply {
                timeInMillis = epochMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val times = calculatePrayerTimes(
                latitude = latitude,
                longitude = longitude,
                timezoneId = timezoneId,
                method = method,
                epochMillis = dayBase.timeInMillis,
                asrMadhhab = asrMadhhab
            )
            val obligatory = listOf(
                0 to times.fajr,
                2 to times.dhuhr,
                3 to times.asr,
                4 to times.maghrib,
                5 to times.isha
            )
            for ((idx, hhmm) in obligatory) {
                val triggerMillis = parseHHmmEpochMillis(dayBase, hhmm) ?: continue
                if (triggerMillis >= minTriggerMillis) {
                    return NextObligatoryPrayer(
                        prayerIndex = idx,
                        timeHHmm = hhmm,
                        triggerAtMillis = triggerMillis,
                        remainingMillis = (triggerMillis - epochMillis).coerceAtLeast(0L)
                    )
                }
            }
        }

        // Defensive fallback: 60s from now
        return NextObligatoryPrayer(
            prayerIndex = 0,
            timeHHmm = "--:--",
            triggerAtMillis = minTriggerMillis,
            remainingMillis = minDelayMillis
        )
    }

    /**
     * Returns the next upcoming occurrence (at least [minDelayMillis] in the future)
     * for each of the 5 obligatory prayers: Fajr (0), Dhuhr (2), Asr (3), Maghrib (4), Isha (5).
     */
    fun calculateAllUpcomingObligatoryPrayers(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        method: CalculationMethod,
        asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI,
        epochMillis: Long = System.currentTimeMillis(),
        minDelayMillis: Long = 5_000L
    ): List<NextObligatoryPrayer> {
        val tz = TimeZone.getTimeZone(timezoneId)
        val minTriggerMillis = epochMillis + minDelayMillis.coerceAtLeast(1_000L)
        val obligatoryIndices = listOf(0, 2, 3, 4, 5)
        val found = linkedMapOf<Int, NextObligatoryPrayer>()

        for (dayOffset in 0..1) {
            val dayBase = Calendar.getInstance(tz).apply {
                timeInMillis = epochMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val times = calculatePrayerTimes(
                latitude = latitude,
                longitude = longitude,
                timezoneId = timezoneId,
                method = method,
                epochMillis = dayBase.timeInMillis,
                asrMadhhab = asrMadhhab
            )
            val entries = listOf(
                0 to times.fajr,
                2 to times.dhuhr,
                3 to times.asr,
                4 to times.maghrib,
                5 to times.isha
            )
            for ((idx, hhmm) in entries) {
                if (found.containsKey(idx)) continue
                val triggerMillis = parseHHmmEpochMillis(dayBase, hhmm) ?: continue
                if (triggerMillis >= minTriggerMillis) {
                    found[idx] = NextObligatoryPrayer(
                        prayerIndex = idx,
                        timeHHmm = hhmm,
                        triggerAtMillis = triggerMillis,
                        remainingMillis = (triggerMillis - epochMillis).coerceAtLeast(0L)
                    )
                }
            }
            if (found.size == obligatoryIndices.size) break
        }

        return found.values.sortedBy { it.triggerAtMillis }
    }

    private fun parseHHmmEpochMillis(dayBase: Calendar, hhmm: String): Long? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return (dayBase.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun parseHHmmSeconds(hhmm: String): Long? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toLongOrNull() ?: return null
        val m = parts[1].toLongOrNull() ?: return null
        return h * 3600L + m * 60L
    }

    private fun formatCountdown(totalSec: Long): String {
        val clamped = totalSec.coerceAtLeast(0L)
        val h = clamped / 3600L
        val m = (clamped % 3600L) / 60L
        val s = clamped % 60L
        return "%02d:%02d:%02d".format(h, m, s)
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
        if (abs(den) < 1e-12) return Double.NaN

        val ratio = num / den
        // Do not clamp an impossible solar crossing into a fabricated 0h/12h hour angle.
        // NaN is rendered as "--:--" by formatHour and skipped by next-prayer selection.
        if (!ratio.isFinite() || ratio !in -1.0..1.0) return Double.NaN

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
