package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AsrMadhhab
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCities
import com.example.localization.AppLanguage
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ReadingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nuur_quran_settings")

enum class AudioRepeatMode(val id: String) {
    OFF("off"),
    AYAH("ayah"),
    SURAH("surah");

    companion object {
        fun fromId(id: String): AudioRepeatMode = entries.firstOrNull { it.id == id } ?: OFF
    }
}

data class UserSettings(
    val language: AppLanguage = AppLanguage.SOMALI,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val readingTheme: ReadingTheme = ReadingTheme.DEFAULT,
    val arabicFontSizeSp: Float = 28f,
    val translationFontSizeSp: Float = 15f,
    val showSomaliTranslation: Boolean = true,
    val showEnglishTranslation: Boolean = true,
    val showTajweedColors: Boolean = false,
    val selectedReciterId: String = "ar.alafasy",
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val lastReadGlobalAyah: Int = 1,
    val khatamAyahsRead: Int = 0,
    val selectedCityId: String = "mogadishu",
    val useCustomGpsCoords: Boolean = false,
    val customLatitude: Double = 2.0469,
    val customLongitude: Double = 45.3182,
    val timezoneId: String = "Africa/Mogadishu",
    val calculationMethod: CalculationMethod = CalculationMethod.MWL,
    val asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI,
    val prayerNotificationsEnabled: Boolean = false,
    val dailyAyahGoal: Int = 20,
    val todayEpochDay: Long = 0L,
    val todayAyahsRead: Int = 0,
    val streakDays: Int = 0,
    val lastStreakEpochDay: Long = -1L,
    val tasbihCount: Int = 0,
    val tasbihTarget: Int = 33,
    val tasbihTotalToday: Int = 0,
    val tasbihLabel: String = "سُبْحَانَ ٱللَّهِ",
    val tasbihHapticEnabled: Boolean = true,
    val favoriteAdhkarOrders: Set<Int> = emptySet(),
    val completedAdhkarToday: Set<Int> = emptySet()
)

data class DailyStreakSnapshot(
    val todayEpochDay: Long,
    val todayAyahsRead: Int,
    val streakDays: Int,
    val lastStreakEpochDay: Long
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val READING_THEME = stringPreferencesKey("reading_theme")
        val ARABIC_FONT_SP = floatPreferencesKey("arabic_font_sp")
        val TRANSLATION_FONT_SP = floatPreferencesKey("translation_font_sp")
        val SHOW_SOMALI = booleanPreferencesKey("show_somali")
        val SHOW_ENGLISH = booleanPreferencesKey("show_english")
        val SHOW_TAJWEED = booleanPreferencesKey("show_tajweed")
        val RECITER_ID = stringPreferencesKey("reciter_id")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
        val LAST_GLOBAL_AYAH = intPreferencesKey("last_global_ayah")
        val KHATAM_AYAHS = intPreferencesKey("khatam_ayahs")
        val CITY_ID = stringPreferencesKey("city_id")
        val USE_CUSTOM_GPS = booleanPreferencesKey("use_custom_gps")
        val CUSTOM_LAT = doublePreferencesKey("custom_lat")
        val CUSTOM_LON = doublePreferencesKey("custom_lon")
        val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val ASR_MADHHAB = stringPreferencesKey("asr_madhhab")
        val PRAYER_NOTIFICATIONS = booleanPreferencesKey("prayer_notifications")
        val DAILY_AYAH_GOAL = intPreferencesKey("daily_ayah_goal")
        val TODAY_EPOCH_DAY = longPreferencesKey("today_epoch_day")
        val TODAY_AYAHS_READ = intPreferencesKey("today_ayahs_read")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_STREAK_EPOCH_DAY = longPreferencesKey("last_streak_epoch_day")
        val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val TASBIH_TOTAL_TODAY = intPreferencesKey("tasbih_total_today")
        val TASBIH_LABEL = stringPreferencesKey("tasbih_label")
        val TASBIH_HAPTIC = booleanPreferencesKey("tasbih_haptic")
        val FAVORITE_ADHKAR = stringSetPreferencesKey("favorite_adhkar")
        val COMPLETED_ADHKAR_TODAY = stringSetPreferencesKey("completed_adhkar_today")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val defaultCity = CuratedCities.defaultCity
        val currentDay = currentEpochDay()
        val storedDay = prefs[Keys.TODAY_EPOCH_DAY] ?: currentDay
        val isSameDay = storedDay == currentDay
        val storedLastStreakDay = prefs[Keys.LAST_STREAK_EPOCH_DAY] ?: -1L
        val storedStreak = prefs[Keys.STREAK_DAYS] ?: 0
        val effectiveStreak = if (storedLastStreakDay >= 0L && currentDay - storedLastStreakDay > 1L) 0 else storedStreak

        UserSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.SOMALI.code),
            themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.id),
            readingTheme = ReadingTheme.fromId(prefs[Keys.READING_THEME] ?: ReadingTheme.DEFAULT.id),
            arabicFontSizeSp = (prefs[Keys.ARABIC_FONT_SP] ?: 28f).coerceIn(18f, 44f),
            translationFontSizeSp = (prefs[Keys.TRANSLATION_FONT_SP] ?: 15f).coerceIn(12f, 24f),
            showSomaliTranslation = prefs[Keys.SHOW_SOMALI] ?: true,
            showEnglishTranslation = prefs[Keys.SHOW_ENGLISH] ?: true,
            showTajweedColors = prefs[Keys.SHOW_TAJWEED] ?: false,
            selectedReciterId = prefs[Keys.RECITER_ID] ?: "ar.alafasy",
            repeatMode = AudioRepeatMode.fromId(prefs[Keys.REPEAT_MODE] ?: AudioRepeatMode.OFF.id),
            playbackSpeed = (prefs[Keys.PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.75f, 1.5f),
            lastReadSurah = prefs[Keys.LAST_SURAH] ?: 1,
            lastReadAyah = prefs[Keys.LAST_AYAH] ?: 1,
            lastReadGlobalAyah = prefs[Keys.LAST_GLOBAL_AYAH] ?: 1,
            khatamAyahsRead = prefs[Keys.KHATAM_AYAHS] ?: 0,
            selectedCityId = prefs[Keys.CITY_ID] ?: defaultCity.id,
            useCustomGpsCoords = prefs[Keys.USE_CUSTOM_GPS] ?: false,
            customLatitude = prefs[Keys.CUSTOM_LAT] ?: defaultCity.latitude,
            customLongitude = prefs[Keys.CUSTOM_LON] ?: defaultCity.longitude,
            timezoneId = prefs[Keys.TIMEZONE_ID] ?: defaultCity.timezoneId,
            calculationMethod = CalculationMethod.fromId(
                prefs[Keys.CALC_METHOD] ?: CalculationMethod.MWL.id
            ),
            asrMadhhab = AsrMadhhab.fromId(prefs[Keys.ASR_MADHHAB] ?: AsrMadhhab.SHAFI.id),
            prayerNotificationsEnabled = prefs[Keys.PRAYER_NOTIFICATIONS] ?: false,
            dailyAyahGoal = (prefs[Keys.DAILY_AYAH_GOAL] ?: 20).coerceIn(5, 200),
            todayEpochDay = currentDay,
            todayAyahsRead = if (isSameDay) (prefs[Keys.TODAY_AYAHS_READ] ?: 0) else 0,
            streakDays = effectiveStreak,
            lastStreakEpochDay = storedLastStreakDay,
            tasbihCount = prefs[Keys.TASBIH_COUNT] ?: 0,
            tasbihTarget = prefs[Keys.TASBIH_TARGET] ?: 33,
            tasbihTotalToday = if (isSameDay) (prefs[Keys.TASBIH_TOTAL_TODAY] ?: 0) else 0,
            tasbihLabel = prefs[Keys.TASBIH_LABEL] ?: "سُبْحَانَ ٱللَّهِ",
            tasbihHapticEnabled = prefs[Keys.TASBIH_HAPTIC] ?: true,
            favoriteAdhkarOrders = (prefs[Keys.FAVORITE_ADHKAR] ?: emptySet())
                .mapNotNull { it.toIntOrNull() }
                .toSet(),
            completedAdhkarToday = if (isSameDay) {
                (prefs[Keys.COMPLETED_ADHKAR_TODAY] ?: emptySet())
                    .mapNotNull { it.toIntOrNull() }
                    .toSet()
            } else {
                emptySet()
            }
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.id }
    }

    suspend fun setReadingTheme(theme: ReadingTheme) {
        context.dataStore.edit { it[Keys.READING_THEME] = theme.id }
    }

    suspend fun setArabicFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.ARABIC_FONT_SP] = sizeSp.coerceIn(18f, 44f) }
    }

    suspend fun setTranslationFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.TRANSLATION_FONT_SP] = sizeSp.coerceIn(12f, 24f) }
    }

    suspend fun setShowSomaliTranslation(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SOMALI] = show }
    }

    suspend fun setShowEnglishTranslation(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ENGLISH] = show }
    }

    suspend fun setShowTajweedColors(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_TAJWEED] = show }
    }

    suspend fun setReciter(reciterId: String) {
        context.dataStore.edit { it[Keys.RECITER_ID] = reciterId }
    }

    suspend fun setRepeatMode(mode: AudioRepeatMode) {
        context.dataStore.edit { it[Keys.REPEAT_MODE] = mode.id }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.75f, 1.5f) }
    }

    suspend fun updateLastRead(surah: Int, ayah: Int, globalAyah: Int) {
        val today = currentEpochDay()
        context.dataStore.edit { prefs ->
            val prevGlobal = prefs[Keys.LAST_GLOBAL_AYAH] ?: 0
            val delta = if (globalAyah > prevGlobal) (globalAyah - prevGlobal).coerceIn(1, 30) else 1
            val prevDay = prefs[Keys.TODAY_EPOCH_DAY] ?: today
            val prevTodayRead = if (prevDay == today) (prefs[Keys.TODAY_AYAHS_READ] ?: 0) else 0
            val updated = computeUpdatedDailyProgress(
                storedEpochDay = prevDay,
                storedTodayAyahs = prevTodayRead,
                storedStreakDays = prefs[Keys.STREAK_DAYS] ?: 0,
                storedLastStreakDay = prefs[Keys.LAST_STREAK_EPOCH_DAY] ?: -1L,
                currentEpochDay = today,
                ayahsIncrement = delta
            )

            prefs[Keys.LAST_SURAH] = surah
            prefs[Keys.LAST_AYAH] = ayah
            prefs[Keys.LAST_GLOBAL_AYAH] = globalAyah
            val currentKhatam = prefs[Keys.KHATAM_AYAHS] ?: 0
            if (globalAyah > currentKhatam) {
                prefs[Keys.KHATAM_AYAHS] = globalAyah.coerceAtMost(6236)
            }
            prefs[Keys.TODAY_EPOCH_DAY] = updated.todayEpochDay
            prefs[Keys.TODAY_AYAHS_READ] = updated.todayAyahsRead
            prefs[Keys.STREAK_DAYS] = updated.streakDays
            prefs[Keys.LAST_STREAK_EPOCH_DAY] = updated.lastStreakEpochDay
        }
    }

    suspend fun setDailyAyahGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_AYAH_GOAL] = goal.coerceIn(5, 200) }
    }

    suspend fun resetKhatamProgress() {
        context.dataStore.edit { prefs ->
            prefs[Keys.KHATAM_AYAHS] = 0
        }
    }

    suspend fun setManualCity(cityId: String, latitude: Double, longitude: Double, timezoneId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CITY_ID] = cityId
            prefs[Keys.USE_CUSTOM_GPS] = false
            prefs[Keys.CUSTOM_LAT] = latitude
            prefs[Keys.CUSTOM_LON] = longitude
            prefs[Keys.TIMEZONE_ID] = timezoneId
        }
    }

    suspend fun setDeviceCoordinates(latitude: Double, longitude: Double, timezoneId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_CUSTOM_GPS] = true
            prefs[Keys.CUSTOM_LAT] = latitude
            prefs[Keys.CUSTOM_LON] = longitude
            prefs[Keys.TIMEZONE_ID] = timezoneId
        }
    }

    suspend fun setTimezone(timezoneId: String) {
        context.dataStore.edit { it[Keys.TIMEZONE_ID] = timezoneId }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[Keys.CALC_METHOD] = method.id }
    }

    suspend fun setAsrMadhhab(madhhab: AsrMadhhab) {
        context.dataStore.edit { it[Keys.ASR_MADHHAB] = madhhab.id }
    }

    suspend fun setPrayerNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRAYER_NOTIFICATIONS] = enabled }
    }

    suspend fun incrementTasbih() {
        val today = currentEpochDay()
        context.dataStore.edit { prefs ->
            val storedDay = prefs[Keys.TODAY_EPOCH_DAY] ?: today
            val todayTotal = if (storedDay == today) (prefs[Keys.TASBIH_TOTAL_TODAY] ?: 0) else 0
            val target = prefs[Keys.TASBIH_TARGET] ?: 33
            val current = prefs[Keys.TASBIH_COUNT] ?: 0
            val next = if (target > 0 && current >= target) 1 else current + 1
            prefs[Keys.TODAY_EPOCH_DAY] = today
            prefs[Keys.TASBIH_COUNT] = next
            prefs[Keys.TASBIH_TOTAL_TODAY] = todayTotal + 1
        }
    }

    suspend fun resetTasbih() {
        context.dataStore.edit { it[Keys.TASBIH_COUNT] = 0 }
    }

    suspend fun setTasbihTarget(target: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TASBIH_TARGET] = target.coerceAtLeast(0)
            prefs[Keys.TASBIH_COUNT] = 0
        }
    }

    suspend fun setTasbihLabel(label: String) {
        val clean = label.trim().ifEmpty { "سُبْحَانَ ٱللَّهِ" }
        context.dataStore.edit { it[Keys.TASBIH_LABEL] = clean }
    }

    suspend fun setTasbihHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TASBIH_HAPTIC] = enabled }
    }

    suspend fun toggleFavoriteAdhkar(order: Int) {
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.FAVORITE_ADHKAR] ?: emptySet()).toMutableSet()
            val key = order.toString()
            if (!current.add(key)) current.remove(key)
            prefs[Keys.FAVORITE_ADHKAR] = current
        }
    }

    suspend fun markAdhkarCompleted(order: Int) {
        val today = currentEpochDay()
        context.dataStore.edit { prefs ->
            val storedDay = prefs[Keys.TODAY_EPOCH_DAY] ?: today
            val current = if (storedDay == today) {
                (prefs[Keys.COMPLETED_ADHKAR_TODAY] ?: emptySet()).toMutableSet()
            } else {
                mutableSetOf()
            }
            current.add(order.toString())
            prefs[Keys.TODAY_EPOCH_DAY] = today
            prefs[Keys.COMPLETED_ADHKAR_TODAY] = current
        }
    }

    suspend fun restoreFromBackup(
        languageCode: String,
        themeModeId: String,
        readingThemeId: String,
        arabicFontSizeSp: Float,
        translationFontSizeSp: Float,
        dailyAyahGoal: Int,
        lastReadSurah: Int,
        lastReadAyah: Int,
        lastReadGlobalAyah: Int,
        khatamAyahsRead: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = AppLanguage.fromCode(languageCode).code
            prefs[Keys.THEME_MODE] = AppThemeMode.fromId(themeModeId).id
            prefs[Keys.READING_THEME] = ReadingTheme.fromId(readingThemeId).id
            prefs[Keys.ARABIC_FONT_SP] = arabicFontSizeSp.coerceIn(18f, 44f)
            prefs[Keys.TRANSLATION_FONT_SP] = translationFontSizeSp.coerceIn(12f, 24f)
            prefs[Keys.DAILY_AYAH_GOAL] = dailyAyahGoal.coerceIn(5, 200)
            prefs[Keys.LAST_SURAH] = lastReadSurah.coerceIn(1, 114)
            prefs[Keys.LAST_AYAH] = lastReadAyah.coerceAtLeast(1)
            prefs[Keys.LAST_GLOBAL_AYAH] = lastReadGlobalAyah.coerceIn(1, 6236)
            prefs[Keys.KHATAM_AYAHS] = khatamAyahsRead.coerceIn(0, 6236)
        }
    }

    companion object {
        fun currentEpochDay(epochMillis: Long = System.currentTimeMillis()): Long =
            epochMillis / 86_400_000L

        fun computeUpdatedDailyProgress(
            storedEpochDay: Long,
            storedTodayAyahs: Int,
            storedStreakDays: Int,
            storedLastStreakDay: Long,
            currentEpochDay: Long,
            ayahsIncrement: Int
        ): DailyStreakSnapshot {
            val baseToday = if (storedEpochDay == currentEpochDay) storedTodayAyahs else 0
            val nextToday = (baseToday + ayahsIncrement.coerceAtLeast(0)).coerceAtLeast(0)
            val nextStreak = when {
                storedLastStreakDay == currentEpochDay -> storedStreakDays.coerceAtLeast(1)
                storedLastStreakDay == currentEpochDay - 1L -> storedStreakDays + 1
                else -> 1
            }
            return DailyStreakSnapshot(
                todayEpochDay = currentEpochDay,
                todayAyahsRead = nextToday,
                streakDays = nextStreak,
                lastStreakEpochDay = currentEpochDay
            )
        }
    }
}
