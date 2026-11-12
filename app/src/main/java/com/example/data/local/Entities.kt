package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishTranslation: String,
    val revelationType: String,
    val ayahCount: Int
)

@Entity(
    tableName = "ayahs",
    indices = [
        Index(value = ["surahNumber", "numberInSurah"]),
        Index(value = ["juz"]),
        Index(value = ["page"])
    ]
)
data class AyahEntity(
    @PrimaryKey val globalNumber: Int,
    val surahNumber: Int,
    val numberInSurah: Int,
    val textUthmani: String,
    val textTajweed: String,
    val textSomali: String,
    val textEnglish: String,
    val tafsirMuyassar: String,
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    val sajdah: Boolean,
    val searchArabicNormalized: String
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["globalAyahNumber"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["globalAyahNumber"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val title: String,
    val body: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_plans")
data class ReadingPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDays: Int,
    val startEpochDay: Long,
    val completedAyahsCount: Int,
    val currentGlobalAyah: Int,
    val isActive: Boolean = true
)

@Entity(
    tableName = "downloaded_audio",
    primaryKeys = ["reciterId", "globalAyahNumber"],
    indices = [Index(value = ["reciterId", "surahNumber"])]
)
data class DownloadedAudioEntity(
    val reciterId: String,
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val localPath: String,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_tafsir",
    primaryKeys = ["surahNumber", "ayahNumber", "sourceId"]
)
data class CachedTafsirEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val sourceId: String,
    val sourceName: String,
    val text: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

data class BookmarkWithAyah(
    @Embedded val bookmark: BookmarkEntity,
    val textUthmani: String,
    val textSomali: String,
    val textEnglish: String,
    val surahArabicName: String,
    val surahEnglishName: String
)

data class NoteWithAyah(
    @Embedded val note: NoteEntity,
    val textUthmani: String,
    val surahArabicName: String,
    val surahEnglishName: String
)

data class SurahDownloadCount(
    val surahNumber: Int,
    val downloadedCount: Int
)

// Bundle JSON DTOs for gzipped verified assets
@Serializable
data class QuranBundleRoot(
    val surahs: List<QuranBundleSurah>
)

@Serializable
data class QuranBundleSurah(
    val n: Int,
    val ar: String,
    val en: String,
    val tr: String,
    val rv: String,
    val ac: Int,
    val a: List<QuranBundleAyah>
)

@Serializable
data class QuranBundleAyah(
    val g: Int,
    val n: Int,
    val u: String,
    val tj: String,
    val so: String,
    val en: String,
    val tf: String,
    val j: Int,
    val m: Int,
    val p: Int,
    val r: Int,
    val h: Int,
    val sj: Boolean,
    val an: String
)

@Serializable
data class DataManifest(
    val surahCount: Int,
    val ayahCount: Int,
    val adhkarCount: Int,
    val quranBundleUncompressedSha256: String,
    val adhkarBundleUncompressedSha256: String,
    val editions: Map<String, String>
)

@Serializable
data class AdhkarItem(
    val order: Int,
    val type: Int, // 0=Both, 1=Morning, 2=Evening
    val arabic: String,
    val english: String,
    val transliteration: String,
    val count: Int,
    val countDescAr: String,
    val countDescEn: String,
    val fadlAr: String,
    val fadlEn: String,
    val sourceAr: String,
    val sourceEn: String
)
