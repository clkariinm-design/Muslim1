package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class JuzIndexEntry(
    val juz: Int,
    val startSurah: Int,
    val startAyah: Int,
    val startGlobal: Int,
    val startSurahName: String,
    val startSurahArabicName: String,
    val ayahCount: Int
)

data class HizbIndexEntry(
    val hizb: Int,
    val startSurah: Int,
    val startAyah: Int,
    val startGlobal: Int,
    val startSurahName: String,
    val startSurahArabicName: String,
    val ayahCount: Int
)

@Dao
interface QuranDao {
    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getSurahCount(): Int

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun getAyahCount(): Int

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun observeAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :surahNumber LIMIT 1")
    fun observeSurah(surahNumber: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM surahs WHERE number = :surahNumber LIMIT 1")
    suspend fun getSurahByNumber(surahNumber: Int): SurahEntity?

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    fun observeAyahsBySurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    suspend fun getAyahsBySurahSync(surahNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE globalNumber = :globalNumber LIMIT 1")
    suspend fun getAyahByGlobalNumber(globalNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber AND numberInSurah = :ayahNumber LIMIT 1")
    suspend fun getAyahByRef(surahNumber: Int, ayahNumber: Int): AyahEntity?

    @Query(
        """
        SELECT * FROM ayahs
        WHERE textSomali LIKE '%' || :query || '%'
           OR textEnglish LIKE '%' || :query || '%'
           OR searchArabicNormalized LIKE '%' || :normalizedArabic || '%'
           OR textUthmani LIKE '%' || :query || '%'
        ORDER BY globalNumber ASC
        LIMIT 80
        """
    )
    suspend fun searchAyahs(query: String, normalizedArabic: String): List<AyahEntity>

    @Query(
        """
        SELECT * FROM surahs
        WHERE englishName LIKE '%' || :query || '%'
           OR englishTranslation LIKE '%' || :query || '%'
           OR arabicName LIKE '%' || :query || '%'
           OR CAST(number AS TEXT) = :query
        ORDER BY number ASC
        """
    )
    suspend fun searchSurahs(query: String): List<SurahEntity>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE globalNumber IN (
            128, 134, 207, 256, 292, 293, 301, 310, 487, 672,
            976, 1419, 1458, 1791, 1810, 2242, 2358, 2500, 2786, 2919, 4908, 5134
        )
        ORDER BY globalNumber ASC
        """
    )
    fun observeQuranicDuas(): Flow<List<AyahEntity>>

    @Query(
        """
        SELECT a.juz AS juz,
               a.surahNumber AS startSurah,
               a.numberInSurah AS startAyah,
               a.globalNumber AS startGlobal,
               s.englishName AS startSurahName,
               s.arabicName AS startSurahArabicName,
               m.ayahCount AS ayahCount
        FROM ayahs a
        INNER JOIN (
            SELECT juz, MIN(globalNumber) AS minGlobal, COUNT(*) AS ayahCount
            FROM ayahs
            GROUP BY juz
        ) m ON a.globalNumber = m.minGlobal
        INNER JOIN surahs s ON s.number = a.surahNumber
        ORDER BY a.juz ASC
        """
    )
    fun observeJuzEntries(): Flow<List<JuzIndexEntry>>

    @Query(
        """
        SELECT ((a.hizbQuarter - 1) / 4) + 1 AS hizb,
               a.surahNumber AS startSurah,
               a.numberInSurah AS startAyah,
               a.globalNumber AS startGlobal,
               s.englishName AS startSurahName,
               s.arabicName AS startSurahArabicName,
               m.ayahCount AS ayahCount
        FROM ayahs a
        INNER JOIN (
            SELECT ((hizbQuarter - 1) / 4) + 1 AS hizbNum, MIN(globalNumber) AS minGlobal, COUNT(*) AS ayahCount
            FROM ayahs
            GROUP BY ((hizbQuarter - 1) / 4) + 1
        ) m ON a.globalNumber = m.minGlobal
        INNER JOIN surahs s ON s.number = a.surahNumber
        ORDER BY hizb ASC
        """
    )
    fun observeHizbEntries(): Flow<List<HizbIndexEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Transaction
    suspend fun seedAll(surahs: List<SurahEntity>, ayahs: List<AyahEntity>) {
        insertSurahs(surahs)
        ayahs.chunked(500).forEach { chunk ->
            insertAyahs(chunk)
        }
    }
}

@Dao
interface UserStudyDao {
    @Query("SELECT globalAyahNumber FROM bookmarks")
    fun observeBookmarkedGlobalNumbers(): Flow<List<Int>>

    @Query(
        """
        SELECT b.*, a.textUthmani, a.textSomali, a.textEnglish,
               s.arabicName AS surahArabicName, s.englishName AS surahEnglishName
        FROM bookmarks b
        INNER JOIN ayahs a ON a.globalNumber = b.globalAyahNumber
        INNER JOIN surahs s ON s.number = b.surahNumber
        ORDER BY b.createdAt DESC
        """
    )
    fun observeBookmarksWithAyah(): Flow<List<BookmarkWithAyah>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE globalAyahNumber = :globalAyahNumber")
    suspend fun deleteBookmarkByGlobalAyah(globalAyahNumber: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE globalAyahNumber = :globalAyahNumber)")
    suspend fun isBookmarked(globalAyahNumber: Int): Boolean

    @Query(
        """
        SELECT n.*, a.textUthmani,
               s.arabicName AS surahArabicName, s.englishName AS surahEnglishName
        FROM notes n
        INNER JOIN ayahs a ON a.globalNumber = n.globalAyahNumber
        INNER JOIN surahs s ON s.number = n.surahNumber
        ORDER BY n.updatedAt DESC
        """
    )
    fun observeNotesWithAyah(): Flow<List<NoteWithAyah>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("SELECT * FROM reading_plans ORDER BY id DESC")
    fun observeReadingPlans(): Flow<List<ReadingPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlan(plan: ReadingPlanEntity)

    @Query("UPDATE reading_plans SET isActive = 0")
    suspend fun deactivateAllPlans()

    @Query("DELETE FROM reading_plans WHERE id = :planId")
    suspend fun deletePlan(planId: Long)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getBookmarksSync(): List<BookmarkEntity>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getNotesSync(): List<NoteEntity>

    @Query("SELECT * FROM reading_plans ORDER BY id DESC")
    suspend fun getPlansSync(): List<ReadingPlanEntity>
}

@Dao
interface AudioDownloadDao {
    @Query(
        """
        SELECT surahNumber, COUNT(*) AS downloadedCount
        FROM downloaded_audio
        WHERE reciterId = :reciterId
        GROUP BY surahNumber
        """
    )
    fun observeSurahDownloadCounts(reciterId: String): Flow<List<SurahDownloadCount>>

    @Query("SELECT * FROM downloaded_audio WHERE reciterId = :reciterId AND globalAyahNumber = :globalAyahNumber LIMIT 1")
    suspend fun getDownloadedAyah(reciterId: String, globalAyahNumber: Int): DownloadedAudioEntity?

    @Query("SELECT * FROM downloaded_audio WHERE reciterId = :reciterId AND surahNumber = :surahNumber")
    suspend fun getDownloadedForSurah(reciterId: String, surahNumber: Int): List<DownloadedAudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(entry: DownloadedAudioEntity)

    @Query("DELETE FROM downloaded_audio WHERE reciterId = :reciterId AND surahNumber = :surahNumber")
    suspend fun deleteForSurah(reciterId: String, surahNumber: Int)
}

@Dao
interface TafsirCacheDao {
    @Query("SELECT * FROM cached_tafsir WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber AND sourceId = :sourceId LIMIT 1")
    suspend fun getCachedTafsir(surahNumber: Int, ayahNumber: Int, sourceId: String): CachedTafsirEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTafsir(entry: CachedTafsirEntity)
}
