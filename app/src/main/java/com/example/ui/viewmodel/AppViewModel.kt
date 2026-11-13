package com.nuur.muslim.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nuur.muslim.NuurQuranApplication
import com.nuur.muslim.audio.AudioPlaybackManager
import com.nuur.muslim.audio.PlaybackUiState
import com.nuur.muslim.data.backup.BackupManager
import com.nuur.muslim.data.backup.BackupRestoreSummary
import com.nuur.muslim.data.local.AudioRepeatMode
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.BookmarkWithAyah
import com.nuur.muslim.data.local.DataManifest
import com.nuur.muslim.data.local.HizbIndexEntry
import com.nuur.muslim.data.local.JuzIndexEntry
import com.nuur.muslim.data.local.NoteWithAyah
import com.nuur.muslim.data.local.QuranDatabase
import com.nuur.muslim.data.local.ReadingPlanEntity
import com.nuur.muslim.data.local.SettingsRepository
import com.nuur.muslim.data.local.SurahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.data.repository.QuranRepository
import com.nuur.muslim.data.repository.SearchResults
import com.nuur.muslim.localization.AppLanguage
import com.nuur.muslim.ui.theme.AppThemeMode
import com.nuur.muslim.ui.theme.ReadingTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataInitState {
    data object Loading : DataInitState
    data class Ready(val manifest: DataManifest) : DataInitState
    data class Error(val message: String) : DataInitState
}

sealed interface OnlineTafsirState {
    data object Idle : OnlineTafsirState
    data object Loading : OnlineTafsirState
    data class Success(val text: String) : OnlineTafsirState
    data class Error(val message: String) : OnlineTafsirState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AppViewModel(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val audioManager: AudioPlaybackManager,
    private val database: QuranDatabase? = null
) : ViewModel() {

    private val _initState = MutableStateFlow<DataInitState>(DataInitState.Loading)
    val initState: StateFlow<DataInitState> = _initState.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val surahs: StateFlow<List<SurahEntity>> = quranRepository.observeAllSurahs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val juzEntries: StateFlow<List<JuzIndexEntry>> = quranRepository.observeJuzEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hizbEntries: StateFlow<List<HizbIndexEntry>> = quranRepository.observeHizbEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedGlobalAyahs: StateFlow<Set<Int>> = quranRepository.observeBookmarkedGlobalNumbers()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val bookmarks: StateFlow<List<BookmarkWithAyah>> = quranRepository.observeBookmarksWithAyah()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteWithAyah>> = quranRepository.observeNotesWithAyah()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingPlans: StateFlow<List<ReadingPlanEntity>> = quranRepository.observeReadingPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quranicDuas: StateFlow<List<AyahEntity>> = quranRepository.observeQuranicDuas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedCounts: StateFlow<Map<Int, Int>> = settings
        .map { it.selectedReciterId }
        .distinctUntilChanged()
        .flatMapLatest { reciterId ->
            quranRepository.observeSurahDownloadCounts(reciterId)
                .map { rows -> rows.associate { it.surahNumber to it.downloadedCount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val audioState: StateFlow<PlaybackUiState> = audioManager.state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _onlineTafsirState = MutableStateFlow<OnlineTafsirState>(OnlineTafsirState.Idle)
    val onlineTafsirState: StateFlow<OnlineTafsirState> = _onlineTafsirState.asStateFlow()

    init {
        initializeDataset()
        viewModelScope.launch {
            _searchQuery
                .debounce(220L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = SearchResults()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        _searchResults.value = quranRepository.search(query)
                        _isSearching.value = false
                    }
                }
        }
    }

    fun initializeDataset() {
        viewModelScope.launch {
            _initState.value = DataInitState.Loading
            runCatching {
                quranRepository.initializeAndVerify()
            }.onSuccess { manifest ->
                _initState.value = DataInitState.Ready(manifest)
            }.onFailure { err ->
                _initState.value = DataInitState.Error(err.localizedMessage ?: "Failed to load Qur'an dataset")
            }
        }
    }

    fun observeSurah(surahNumber: Int): Flow<SurahEntity?> = quranRepository.observeSurah(surahNumber)

    fun observeAyahs(surahNumber: Int): Flow<List<AyahEntity>> = quranRepository.observeAyahsBySurah(surahNumber)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun markLastRead(surahNumber: Int, ayahNumber: Int, globalAyah: Int) {
        viewModelScope.launch {
            settingsRepository.updateLastRead(surahNumber, ayahNumber, globalAyah)
        }
    }

    fun resetKhatam() {
        viewModelScope.launch { settingsRepository.resetKhatamProgress() }
    }

    fun toggleBookmark(ayah: AyahEntity) {
        viewModelScope.launch { quranRepository.toggleBookmark(ayah) }
    }

    fun removeBookmark(globalAyahNumber: Int) {
        viewModelScope.launch { quranRepository.removeBookmarkByGlobalAyah(globalAyahNumber) }
    }

    fun saveNote(ayah: AyahEntity, existingId: Long = 0L, title: String, body: String) {
        viewModelScope.launch { quranRepository.saveNote(ayah, existingId, title, body) }
    }

    fun updateNote(
        noteId: Long,
        globalAyahNumber: Int,
        surahNumber: Int,
        ayahNumber: Int,
        title: String,
        body: String
    ) {
        viewModelScope.launch {
            quranRepository.saveNoteByRef(noteId, globalAyahNumber, surahNumber, ayahNumber, title, body)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { quranRepository.deleteNote(noteId) }
    }

    fun createReadingPlan(title: String, targetDays: Int) {
        viewModelScope.launch {
            quranRepository.createReadingPlan(
                title = title,
                targetDays = targetDays,
                currentGlobalAyah = settings.value.khatamAyahsRead
            )
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch { quranRepository.deletePlan(planId) }
    }

    // Audio actions
    fun playSurah(surah: SurahEntity, ayahs: List<AyahEntity>, startAyahIndex: Int = 0) {
        val s = settings.value
        audioManager.playSurahFromAyah(
            surah = surah,
            ayahs = ayahs,
            startIndex = startAyahIndex,
            reciterId = s.selectedReciterId,
            repeatMode = s.repeatMode,
            playbackSpeed = s.playbackSpeed
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        audioManager.setPlaybackSpeed(clamped)
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(clamped) }
    }

    fun switchReciterAndReloadIfActive(reciterId: String) {
        viewModelScope.launch {
            settingsRepository.setReciter(reciterId)
            val currentAudio = audioState.value
            if (currentAudio.isActive) {
                val surah = quranRepository.getSurahByNumber(currentAudio.currentSurahNumber) ?: return@launch
                val ayahs = quranRepository.getAyahsBySurahSync(currentAudio.currentSurahNumber)
                val idx = ayahs.indexOfFirst { it.numberInSurah == currentAudio.currentAyahNumber }.coerceAtLeast(0)
                audioManager.playSurahFromAyah(
                    surah = surah,
                    ayahs = ayahs,
                    startIndex = idx,
                    reciterId = reciterId,
                    repeatMode = settings.value.repeatMode,
                    playbackSpeed = settings.value.playbackSpeed
                )
            }
        }
    }

    fun togglePlayPause() = audioManager.togglePlayPause()
    fun seekTo(ms: Long) = audioManager.seekTo(ms)
    fun nextAyah() = audioManager.nextAyah()
    fun previousAyah() = audioManager.previousAyah()
    fun dismissAudio() = audioManager.stopAndDismiss()
    fun clearAudioError() = audioManager.clearError()

    fun cycleRepeatMode() {
        val next = when (settings.value.repeatMode) {
            AudioRepeatMode.OFF -> AudioRepeatMode.AYAH
            AudioRepeatMode.AYAH -> AudioRepeatMode.SURAH
            AudioRepeatMode.SURAH -> AudioRepeatMode.OFF
        }
        viewModelScope.launch {
            settingsRepository.setRepeatMode(next)
            audioManager.setRepeatMode(next)
        }
    }

    fun downloadSurahAudio(surah: SurahEntity) {
        viewModelScope.launch {
            val ayahs = quranRepository.getAyahsBySurahSync(surah.number)
            audioManager.downloadSurahAudio(surah, ayahs, settings.value.selectedReciterId)
        }
    }

    fun cancelSurahDownload() {
        audioManager.cancelDownload()
    }

    fun deleteSurahAudio(surahNumber: Int) {
        audioManager.deleteSurahAudio(surahNumber, settings.value.selectedReciterId)
    }

    // Tafsir online fetch (with Room cache)
    fun resetOnlineTafsir() {
        _onlineTafsirState.value = OnlineTafsirState.Idle
    }

    fun loadOnlineIbnKathir(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            _onlineTafsirState.value = OnlineTafsirState.Loading
            val res = quranRepository.getOrFetchIbnKathirEnglish(surahNumber, ayahNumber)
            _onlineTafsirState.value = res.fold(
                onSuccess = { OnlineTafsirState.Success(it) },
                onFailure = { OnlineTafsirState.Error(it.localizedMessage ?: "Network error") }
            )
        }
    }

    // Settings updates
    fun setLanguage(lang: AppLanguage) = viewModelScope.launch { settingsRepository.setLanguage(lang) }
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setReadingTheme(theme: ReadingTheme) = viewModelScope.launch { settingsRepository.setReadingTheme(theme) }
    fun setDailyAyahGoal(goal: Int) = viewModelScope.launch { settingsRepository.setDailyAyahGoal(goal) }
    fun setArabicFontSize(sp: Float) = viewModelScope.launch { settingsRepository.setArabicFontSize(sp) }
    fun setTranslationFontSize(sp: Float) = viewModelScope.launch { settingsRepository.setTranslationFontSize(sp) }
    fun setShowSomali(show: Boolean) = viewModelScope.launch { settingsRepository.setShowSomaliTranslation(show) }
    fun setShowEnglish(show: Boolean) = viewModelScope.launch { settingsRepository.setShowEnglishTranslation(show) }
    fun setShowTajweed(show: Boolean) = viewModelScope.launch { settingsRepository.setShowTajweedColors(show) }
    fun setReciter(id: String) = viewModelScope.launch { settingsRepository.setReciter(id) }

    suspend fun exportBackupJson(): Result<String> {
        val db = database ?: return Result.failure(IllegalStateException("Database unavailable"))
        return runCatching { BackupManager.exportJson(db, settings.value) }
    }

    suspend fun importBackupJson(rawJson: String): Result<BackupRestoreSummary> {
        val db = database ?: return Result.failure(IllegalStateException("Database unavailable"))
        return BackupManager.importJson(rawJson, db, settingsRepository)
    }

    class Factory(private val app: NuurQuranApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                app.quranRepository,
                app.settingsRepository,
                app.audioPlaybackManager,
                app.database
            ) as T
        }
    }
}
