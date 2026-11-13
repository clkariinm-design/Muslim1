package com.nuur.muslim.data.backup

import com.nuur.muslim.data.local.BookmarkEntity
import com.nuur.muslim.data.local.NoteEntity
import com.nuur.muslim.data.local.QuranDatabase
import com.nuur.muslim.data.local.ReadingPlanEntity
import com.nuur.muslim.data.local.SettingsRepository
import com.nuur.muslim.data.local.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupBookmark(
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val label: String = "",
    val createdAt: Long = 0L
)

@Serializable
data class BackupNote(
    val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val title: String,
    val body: String,
    val updatedAt: Long = 0L
)

@Serializable
data class BackupPlan(
    val title: String,
    val targetDays: Int,
    val startEpochDay: Long,
    val completedAyahsCount: Int,
    val currentGlobalAyah: Int,
    val isActive: Boolean
)

@Serializable
data class BackupSettings(
    val languageCode: String,
    val themeModeId: String,
    val readingThemeId: String,
    val arabicFontSizeSp: Float,
    val translationFontSizeSp: Float,
    val dailyAyahGoal: Int,
    val lastReadSurah: Int,
    val lastReadAyah: Int,
    val lastReadGlobalAyah: Int,
    val khatamAyahsRead: Int
)

@Serializable
data class NuurBackupBundle(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val settings: BackupSettings,
    val bookmarks: List<BackupBookmark>,
    val notes: List<BackupNote>,
    val readingPlans: List<BackupPlan>
)

data class BackupRestoreSummary(
    val bookmarksRestored: Int,
    val notesRestored: Int,
    val plansRestored: Int
)

object BackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(
        db: QuranDatabase,
        settings: UserSettings
    ): String = withContext(Dispatchers.IO) {
        val studyDao = db.userStudyDao()
        val bundle = NuurBackupBundle(
            settings = BackupSettings(
                languageCode = settings.language.code,
                themeModeId = settings.themeMode.id,
                readingThemeId = settings.readingTheme.id,
                arabicFontSizeSp = settings.arabicFontSizeSp,
                translationFontSizeSp = settings.translationFontSizeSp,
                dailyAyahGoal = settings.dailyAyahGoal,
                lastReadSurah = settings.lastReadSurah,
                lastReadAyah = settings.lastReadAyah,
                lastReadGlobalAyah = settings.lastReadGlobalAyah,
                khatamAyahsRead = settings.khatamAyahsRead
            ),
            bookmarks = studyDao.getBookmarksSync().map {
                BackupBookmark(
                    globalAyahNumber = it.globalAyahNumber,
                    surahNumber = it.surahNumber,
                    ayahNumber = it.ayahNumber,
                    label = it.label,
                    createdAt = it.createdAt
                )
            },
            notes = studyDao.getNotesSync().map {
                BackupNote(
                    globalAyahNumber = it.globalAyahNumber,
                    surahNumber = it.surahNumber,
                    ayahNumber = it.ayahNumber,
                    title = it.title,
                    body = it.body,
                    updatedAt = it.updatedAt
                )
            },
            readingPlans = studyDao.getPlansSync().map {
                BackupPlan(
                    title = it.title,
                    targetDays = it.targetDays,
                    startEpochDay = it.startEpochDay,
                    completedAyahsCount = it.completedAyahsCount,
                    currentGlobalAyah = it.currentGlobalAyah,
                    isActive = it.isActive
                )
            }
        )
        json.encodeToString(bundle)
    }

    suspend fun importJson(
        rawJson: String,
        db: QuranDatabase,
        settingsRepository: SettingsRepository
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = json.decodeFromString<NuurBackupBundle>(rawJson)
            require(bundle.schemaVersion == 1) { "Unsupported backup schema v${bundle.schemaVersion}" }
            val studyDao = db.userStudyDao()

            bundle.bookmarks.forEach { b ->
                studyDao.insertBookmark(
                    BookmarkEntity(
                        globalAyahNumber = b.globalAyahNumber,
                        surahNumber = b.surahNumber,
                        ayahNumber = b.ayahNumber,
                        label = b.label,
                        createdAt = b.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
            }

            bundle.notes.forEach { n ->
                studyDao.insertOrUpdateNote(
                    NoteEntity(
                        globalAyahNumber = n.globalAyahNumber,
                        surahNumber = n.surahNumber,
                        ayahNumber = n.ayahNumber,
                        title = n.title,
                        body = n.body,
                        updatedAt = n.updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
            }

            if (bundle.readingPlans.any { it.isActive }) {
                studyDao.deactivateAllPlans()
            }
            bundle.readingPlans.forEach { p ->
                studyDao.insertOrUpdatePlan(
                    ReadingPlanEntity(
                        title = p.title,
                        targetDays = p.targetDays.coerceIn(7, 365),
                        startEpochDay = p.startEpochDay,
                        completedAyahsCount = p.completedAyahsCount,
                        currentGlobalAyah = p.currentGlobalAyah,
                        isActive = p.isActive
                    )
                )
            }

            settingsRepository.restoreFromBackup(
                languageCode = bundle.settings.languageCode,
                themeModeId = bundle.settings.themeModeId,
                readingThemeId = bundle.settings.readingThemeId,
                arabicFontSizeSp = bundle.settings.arabicFontSizeSp,
                translationFontSizeSp = bundle.settings.translationFontSizeSp,
                dailyAyahGoal = bundle.settings.dailyAyahGoal,
                lastReadSurah = bundle.settings.lastReadSurah,
                lastReadAyah = bundle.settings.lastReadAyah,
                lastReadGlobalAyah = bundle.settings.lastReadGlobalAyah,
                khatamAyahsRead = bundle.settings.khatamAyahsRead
            )

            BackupRestoreSummary(
                bookmarksRestored = bundle.bookmarks.size,
                notesRestored = bundle.notes.size,
                plansRestored = bundle.readingPlans.size
            )
        }
    }
}
