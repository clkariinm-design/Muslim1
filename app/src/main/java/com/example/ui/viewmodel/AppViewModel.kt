package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NuurQuranApplication
import com.example.audio.AudioPlaybackManager
import com.example.audio.PlaybackUiState
import com.example.data.local.AudioRepeatMode
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkWithAyah
import com.example.data.local.DataManifest
import com.example.data.local.NoteWithAyah
import com.example.data.local.ReadingPlanEntity
import com.example.data.local.SettingsRepository
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.data.repository.QuranRepository
import com.example.data.repository.SearchResults
import com.example.localization.AppLanguage
import com.example.ui.theme.AppThemeMode
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
    private val audioManager: AudioPlaybackManager
) : ViewModel() {

    private val _initState = MutableStateFlow<DataInitState>(DataInitState.Loading)
    val initState: StateFlow<DataInitState> = _initState.asStateFlow()

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val surahs: StateFlow<List<SurahEntity>> = quranRepository.observeAllSurahs()
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

    fun saveNote(ayah: AyahEntity, existingId: Long = 0L, title: String, body: String) {
        viewModelScope.launch { quranRepository.saveNote(ayah, existingId, title, body) }
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
            repeatMode = s.repeatMode
        )
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
    fun setArabicFontSize(sp: Float) = viewModelScope.launch { settingsRepository.setArabicFontSize(sp) }
    fun setTranslationFontSize(sp: Float) = viewModelScope.launch { settingsRepository.setTranslationFontSize(sp) }
    fun setShowSomali(show: Boolean) = viewModelScope.launch { settingsRepository.setShowSomaliTranslation(show) }
    fun setShowEnglish(show: Boolean) = viewModelScope.launch { settingsRepository.setShowEnglishTranslation(show) }
    fun setShowTajweed(show: Boolean) = viewModelScope.launch { settingsRepository.setShowTajweedColors(show) }
    fun setReciter(id: String) = viewModelScope.launch { settingsRepository.setReciter(id) }

    class Factory(private val app: NuurQuranApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                app.quranRepository,
                app.settingsRepository,
                app.audioPlaybackManager
            ) as T
        }
    }
}
