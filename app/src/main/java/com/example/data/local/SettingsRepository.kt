package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCities
import com.example.localization.AppLanguage
import com.example.ui.theme.AppThemeMode
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
    val arabicFontSizeSp: Float = 28f,
    val translationFontSizeSp: Float = 15f,
    val showSomaliTranslation: Boolean = true,
    val showEnglishTranslation: Boolean = true,
    val showTajweedColors: Boolean = false,
    val selectedReciterId: String = "ar.alafasy",
    val repeatMode: AudioRepeatMode = AudioRepeatMode.OFF,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val lastReadGlobalAyah: Int = 1,
    val khatamAyahsRead: Int = 0,
    val selectedCityId: String = "mogadishu",
    val useCustomGpsCoords: Boolean = false,
    val customLatitude: Double = 2.0469,
    val customLongitude: Double = 45.3182,
    val timezoneId: String = "Africa/Mogadishu",
    val calculationMethod: CalculationMethod = CalculationMethod.MWL
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ARABIC_FONT_SP = floatPreferencesKey("arabic_font_sp")
        val TRANSLATION_FONT_SP = floatPreferencesKey("translation_font_sp")
        val SHOW_SOMALI = booleanPreferencesKey("show_somali")
        val SHOW_ENGLISH = booleanPreferencesKey("show_english")
        val SHOW_TAJWEED = booleanPreferencesKey("show_tajweed")
        val RECITER_ID = stringPreferencesKey("reciter_id")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
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
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val defaultCity = CuratedCities.defaultCity
        UserSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.SOMALI.code),
            themeMode = AppThemeMode.fromId(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.id),
            arabicFontSizeSp = (prefs[Keys.ARABIC_FONT_SP] ?: 28f).coerceIn(18f, 44f),
            translationFontSizeSp = (prefs[Keys.TRANSLATION_FONT_SP] ?: 15f).coerceIn(12f, 24f),
            showSomaliTranslation = prefs[Keys.SHOW_SOMALI] ?: true,
            showEnglishTranslation = prefs[Keys.SHOW_ENGLISH] ?: true,
            showTajweedColors = prefs[Keys.SHOW_TAJWEED] ?: false,
            selectedReciterId = prefs[Keys.RECITER_ID] ?: "ar.alafasy",
            repeatMode = AudioRepeatMode.fromId(prefs[Keys.REPEAT_MODE] ?: AudioRepeatMode.OFF.id),
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
            )
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.id }
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

    suspend fun updateLastRead(surah: Int, ayah: Int, globalAyah: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SURAH] = surah
            prefs[Keys.LAST_AYAH] = ayah
            prefs[Keys.LAST_GLOBAL_AYAH] = globalAyah
            val currentKhatam = prefs[Keys.KHATAM_AYAHS] ?: 0
            if (globalAyah > currentKhatam) {
                prefs[Keys.KHATAM_AYAHS] = globalAyah.coerceAtMost(6236)
            }
        }
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
}
