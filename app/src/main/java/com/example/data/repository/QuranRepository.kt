package com.example.data.repository

import android.content.Context
import com.example.data.local.AudioDownloadDao
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkWithAyah
import com.example.data.local.CachedTafsirEntity
import com.example.data.local.DataManifest
import com.example.data.local.NoteEntity
import com.example.data.local.NoteWithAyah
import com.example.data.local.QuranDao
import com.example.data.local.QuranDatabase
import com.example.data.local.ReadingPlanEntity
import com.example.data.local.SurahDownloadCount
import com.example.data.local.SurahEntity
import com.example.data.local.TafsirCacheDao
import com.example.data.local.UserStudyDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchResults(
    val surahs: List<SurahEntity> = emptyList(),
    val ayahs: List<AyahEntity> = emptyList()
)

class QuranRepository(
    private val context: Context,
    private val db: QuranDatabase
) {
    private val quranDao: QuranDao = db.quranDao()
    private val studyDao: UserStudyDao = db.userStudyDao()
    private val audioDao: AudioDownloadDao = db.audioDownloadDao()
    private val tafsirDao: TafsirCacheDao = db.tafsirCacheDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun initializeAndVerify(): DataManifest = withContext(Dispatchers.IO) {
        QuranDatabase.ensureSeededAndVerified(context, db)
    }

    fun observeAllSurahs(): Flow<List<SurahEntity>> = quranDao.observeAllSurahs()

    fun observeSurah(surahNumber: Int): Flow<SurahEntity?> = quranDao.observeSurah(surahNumber)

    fun observeAyahsBySurah(surahNumber: Int): Flow<List<AyahEntity>> =
        quranDao.observeAyahsBySurah(surahNumber)

    suspend fun getAyahsBySurahSync(surahNumber: Int): List<AyahEntity> =
        withContext(Dispatchers.IO) { quranDao.getAyahsBySurahSync(surahNumber) }

    suspend fun getSurahByNumber(surahNumber: Int): SurahEntity? =
        withContext(Dispatchers.IO) { quranDao.getSurahByNumber(surahNumber) }

    suspend fun getAyahByGlobalNumber(globalNumber: Int): AyahEntity? =
        withContext(Dispatchers.IO) { quranDao.getAyahByGlobalNumber(globalNumber) }

    fun observeQuranicDuas(): Flow<List<AyahEntity>> = quranDao.observeQuranicDuas()

    fun observeBookmarkedGlobalNumbers(): Flow<List<Int>> = studyDao.observeBookmarkedGlobalNumbers()

    fun observeBookmarksWithAyah(): Flow<List<BookmarkWithAyah>> = studyDao.observeBookmarksWithAyah()

    fun observeNotesWithAyah(): Flow<List<NoteWithAyah>> = studyDao.observeNotesWithAyah()

    fun observeReadingPlans(): Flow<List<ReadingPlanEntity>> = studyDao.observeReadingPlans()

    fun observeSurahDownloadCounts(reciterId: String): Flow<List<SurahDownloadCount>> =
        audioDao.observeSurahDownloadCounts(reciterId)

    suspend fun toggleBookmark(ayah: AyahEntity): Boolean = withContext(Dispatchers.IO) {
        val exists = studyDao.isBookmarked(ayah.globalNumber)
        if (exists) {
            studyDao.deleteBookmarkByGlobalAyah(ayah.globalNumber)
            false
        } else {
            studyDao.insertBookmark(
                BookmarkEntity(
                    globalAyahNumber = ayah.globalNumber,
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.numberInSurah
                )
            )
            true
        }
    }

    suspend fun saveNote(ayah: AyahEntity, existingId: Long = 0L, title: String, body: String) {
        withContext(Dispatchers.IO) {
            studyDao.insertOrUpdateNote(
                NoteEntity(
                    id = existingId,
                    globalAyahNumber = ayah.globalNumber,
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.numberInSurah,
                    title = title.trim(),
                    body = body.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteNote(noteId: Long) {
        withContext(Dispatchers.IO) { studyDao.deleteNoteById(noteId) }
    }

    suspend fun createReadingPlan(title: String, targetDays: Int, currentGlobalAyah: Int) {
        withContext(Dispatchers.IO) {
            studyDao.deactivateAllPlans()
            val epochDay = System.currentTimeMillis() / 86_400_000L
            studyDao.insertOrUpdatePlan(
                ReadingPlanEntity(
                    title = title.trim().ifEmpty { "Khatam (${targetDays}d)" },
                    targetDays = targetDays.coerceIn(7, 365),
                    startEpochDay = epochDay,
                    completedAyahsCount = currentGlobalAyah,
                    currentGlobalAyah = currentGlobalAyah,
                    isActive = true
                )
            )
        }
    }

    suspend fun deletePlan(planId: Long) {
        withContext(Dispatchers.IO) { studyDao.deletePlan(planId) }
    }

    suspend fun search(rawQuery: String): SearchResults = withContext(Dispatchers.IO) {
        val q = rawQuery.trim()
        if (q.isEmpty()) return@withContext SearchResults()

        // Direct surah:ayah reference jump e.g. "2:255"
        val refMatch = Regex("""^(\d{1,3})\s*[:\-]\s*(\d{1,3})$""").matchEntire(q)
        if (refMatch != null) {
            val s = refMatch.groupValues[1].toIntOrNull()
            val a = refMatch.groupValues[2].toIntOrNull()
            if (s != null && a != null) {
                val surah = quranDao.getSurahByNumber(s)
                val ayah = quranDao.getAyahByRef(s, a)
                return@withContext SearchResults(
                    surahs = listOfNotNull(surah),
                    ayahs = listOfNotNull(ayah)
                )
            }
        }

        val surahs = quranDao.searchSurahs(q)
        val normAr = normalizeArabicForSearch(q)
        val ayahs = if (q.length >= 2) quranDao.searchAyahs(q, normAr) else emptyList()
        SearchResults(surahs = surahs, ayahs = ayahs)
    }

    /**
     * Fetches Ibn Kathir (Abridged - English) from Quran.com API v4 over HTTPS and caches in Room.
     */
    suspend fun getOrFetchIbnKathirEnglish(surahNumber: Int, ayahNumber: Int): Result<String> =
        withContext(Dispatchers.IO) {
            val sourceId = "qurancom.169.ibn_kathir_en"
            val cached = tafsirDao.getCachedTafsir(surahNumber, ayahNumber, sourceId)
            if (cached != null) {
                return@withContext Result.success(cached.text)
            }

            runCatching {
                val url = "https://api.quran.com/api/v4/tafsirs/169/by_ayah/$surahNumber:$ayahNumber"
                val req = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body?.string() ?: error("Empty response")
                    val root = JSONObject(body)
                    val rawHtml = root.getJSONObject("tafsir").getString("text")
                    val cleanText = rawHtml
                        .replace(Regex("<[^>]+>"), " ")
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .replace("&apos;", "'")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    tafsirDao.insertTafsir(
                        CachedTafsirEntity(
                            surahNumber = surahNumber,
                            ayahNumber = ayahNumber,
                            sourceId = sourceId,
                            sourceName = "Tafsir Ibn Kathir (Abridged) - Quran.com API v4",
                            text = cleanText
                        )
                    )
                    cleanText
                }
            }
        }

    companion object {
        fun normalizeArabicForSearch(input: String): String {
            return input
                .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "")
                .replace('ٱ', 'ا')
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ى', 'ي')
                .replace('ة', 'ه')
                .trim()
        }
    }
}
