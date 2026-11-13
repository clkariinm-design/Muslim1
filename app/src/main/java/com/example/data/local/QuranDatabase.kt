package com.nuur.muslim.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        ReadingPlanEntity::class,
        DownloadedAudioEntity::class,
        CachedTafsirEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun userStudyDao(): UserStudyDao
    abstract fun audioDownloadDao(): AudioDownloadDao
    abstract fun tafsirCacheDao(): TafsirCacheDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        private val json = Json { ignoreUnknownKeys = true }

        fun getInstance(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "nuur_quran.db"
                ).build().also { INSTANCE = it }
            }
        }

        /**
         * Verifies the bundled dataset integrity via SHA-256 and exact 114 surah / 6,236 ayah counts,
         * and seeds Room once if not yet populated.
         */
        suspend fun ensureSeededAndVerified(context: Context, db: QuranDatabase): DataManifest {
            val manifestBytes = context.assets.open("data_manifest.json").use { it.readBytes() }
            val manifest = json.decodeFromString<DataManifest>(manifestBytes.decodeToString())

            val dao = db.quranDao()
            // Verify the source asset on every check; row counts alone cannot detect altered rows.
            val uncompressedBytes = context.assets.open("quran_bundle.bin").use { raw ->
                GZIPInputStream(raw).use { gz -> gz.readBytes() }
            }

            val actualSha = sha256Hex(uncompressedBytes)
            check(actualSha == manifest.quranBundleUncompressedSha256) {
                "Qur'an dataset checksum mismatch! Expected ${manifest.quranBundleUncompressedSha256}, got $actualSha"
            }

            val bundle = json.decodeFromString<QuranBundleRoot>(uncompressedBytes.decodeToString())
            check(bundle.surahs.size == manifest.surahCount && manifest.surahCount == 114) {
                "Expected 114 surahs, found ${bundle.surahs.size}"
            }

            val surahEntities = ArrayList<SurahEntity>(manifest.surahCount)
            val ayahEntities = ArrayList<AyahEntity>(manifest.ayahCount)

            for (s in bundle.surahs) {
                surahEntities.add(
                    SurahEntity(
                        number = s.n,
                        arabicName = s.ar,
                        englishName = s.en,
                        englishTranslation = s.tr,
                        revelationType = s.rv,
                        ayahCount = s.ac
                    )
                )
                for (a in s.a) {
                    ayahEntities.add(
                        AyahEntity(
                            globalNumber = a.g,
                            surahNumber = s.n,
                            numberInSurah = a.n,
                            textUthmani = a.u,
                            textTajweed = a.tj,
                            textSomali = a.so,
                            textEnglish = a.en,
                            tafsirMuyassar = a.tf,
                            juz = a.j,
                            manzil = a.m,
                            page = a.p,
                            ruku = a.r,
                            hizbQuarter = a.h,
                            sajdah = a.sj,
                            searchArabicNormalized = a.an
                        )
                    )
                }
            }

            check(ayahEntities.size == manifest.ayahCount && manifest.ayahCount == 6236) {
                "Expected 6,236 ayahs, found ${ayahEntities.size}"
            }

            // Compare complete persisted rows, not only table counts. Re-seed atomically if any
            // Qur'an text or metadata has changed or a row is missing/extra.
            if (dao.getAllSurahsSync() != surahEntities || dao.getAllAyahsSync() != ayahEntities) {
                dao.seedAll(surahEntities, ayahEntities)
            }
            return manifest
        }

        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
