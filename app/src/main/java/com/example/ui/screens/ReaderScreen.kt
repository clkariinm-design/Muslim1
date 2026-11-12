package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackUiState
import com.example.data.local.AyahEntity
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.localization.AppStrings
import com.example.ui.components.GoldOrnamentalDivider
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.OnlineTafsirState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    surah: SurahEntity?,
    ayahs: List<AyahEntity>,
    targetAyah: Int,
    settings: UserSettings,
    strings: AppStrings,
    bookmarkedGlobals: Set<Int>,
    downloadedCount: Int,
    audioState: PlaybackUiState,
    onlineTafsirState: OnlineTafsirState,
    onBack: () -> Unit,
    onPlaySurah: (SurahEntity, List<AyahEntity>, Int) -> Unit,
    onDownloadSurah: (SurahEntity) -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteSurahAudio: (Int) -> Unit,
    onToggleBookmark: (AyahEntity) -> Unit,
    onSaveNote: (AyahEntity, String, String) -> Unit,
    onMarkLastRead: (AyahEntity) -> Unit,
    onResetTafsir: () -> Unit,
    onLoadOnlineTafsir: (Int, Int) -> Unit,
    onArabicSizeChange: (Float) -> Unit,
    onTranslationSizeChange: (Float) -> Unit,
    onToggleSomali: (Boolean) -> Unit,
    onToggleEnglish: (Boolean) -> Unit,
    onToggleTajweed: (Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var tafsirAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var noteAyah by remember { mutableStateOf<AyahEntity?>(null) }
    var showTajweedGuide by remember { mutableStateOf(false) }
    var showDisplaySheet by remember { mutableStateOf(false) }

    LaunchedEffect(ayahs.size, targetAyah) {
        if (ayahs.isNotEmpty() && targetAyah > 1) {
            val idx = ayahs.indexOfFirst { it.numberInSurah == targetAyah }
            if (idx >= 0) {
                listState.scrollToItem(idx + 1)
            }
        }
    }

    // Follow currently playing ayah smoothly when reading the active surah
    LaunchedEffect(audioState.isActive, audioState.currentSurahNumber, audioState.currentGlobalAyah, ayahs.size) {
        if (audioState.isActive && surah != null && audioState.currentSurahNumber == surah.number) {
            val idx = ayahs.indexOfFirst { it.globalNumber == audioState.currentGlobalAyah }
            if (idx >= 0) {
                listState.animateScrollToItem(idx + 1)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = surah?.englishName ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (surah != null) {
                            Text(
                                text = "${surah.arabicName} • ${surah.ayahCount} ${strings.ayahsWord}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onToggleTajweed(!settings.showTajweedColors) },
                        modifier = Modifier.testTag("reader_tajweed_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = strings.tajweedColors,
                            tint = if (settings.showTajweedColors) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = { showTajweedGuide = true },
                        modifier = Modifier.testTag("reader_tajweed_guide_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = strings.tajweedGuideTitle)
                    }
                    IconButton(
                        onClick = { showDisplaySheet = true },
                        modifier = Modifier.testTag("reader_display_settings_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = strings.settingsTitle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 780.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (surah != null) {
                        val isOfflineReady = downloadedCount >= surah.ayahCount && surah.ayahCount > 0
                        val isDownloading = audioState.downloadingSurahNumber == surah.number
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = surah.arabicName,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontFamily = AmiriFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${surah.englishName} — ${surah.englishTranslation}",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val rev = if (surah.revelationType.equals("Meccan", true)) strings.meccan else strings.medinan
                                Text(
                                    text = "$rev • ${surah.ayahCount} ${strings.ayahsWord}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (surah.number != 1 && surah.number != 9) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    GoldOrnamentalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onPlaySurah(surah, ayahs, 0) },
                                        enabled = ayahs.isNotEmpty(),
                                        modifier = Modifier.testTag("reader_play_surah_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.playSurah)
                                    }
                                    when {
                                        isDownloading -> {
                                            OutlinedButton(
                                                onClick = onCancelDownload,
                                                modifier = Modifier.testTag("reader_cancel_download_button")
                                            ) {
                                                CircularProgressIndicator(
                                                    progress = { audioState.downloadProgress },
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${(audioState.downloadProgress * 100).toInt()}%")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = strings.cancelDownload,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        isOfflineReady -> {
                                            OutlinedButton(
                                                onClick = { onDeleteSurahAudio(surah.number) },
                                                modifier = Modifier.testTag("reader_delete_offline_button")
                                            ) {
                                                Icon(Icons.Default.CloudDone, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.DeleteOutline, contentDescription = strings.deleteOfflineAudio)
                                            }
                                        }
                                        else -> {
                                            OutlinedButton(
                                                onClick = { onDownloadSurah(surah) },
                                                modifier = Modifier.testTag("reader_download_surah_button")
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(strings.filterOffline)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                itemsIndexed(ayahs, key = { _, a -> a.globalNumber }) { index, ayah ->
                    val isBookmarked = ayah.globalNumber in bookmarkedGlobals
                    val isPlayingThis = audioState.isActive && audioState.currentGlobalAyah == ayah.globalNumber
                    ReaderAyahCard(
                        ayah = ayah,
                        settings = settings,
                        strings = strings,
                        isBookmarked = isBookmarked,
                        isPlayingThis = isPlayingThis,
                        onPlayAyah = {
                            if (surah != null) onPlaySurah(surah, ayahs, index)
                        },
                        onOpenTafsir = {
                            onResetTafsir()
                            tafsirAyah = ayah
                        },
                        onOpenNote = { noteAyah = ayah },
                        onToggleBookmark = {
                            onToggleBookmark(ayah)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isBookmarked) strings.bookmarkRemoved else strings.bookmarkAdded
                                )
                            }
                        },
                        onMarkLastRead = {
                            onMarkLastRead(ayah)
                            scope.launch {
                                snackbarHostState.showSnackbar("${strings.markAsLastRead}: ${ayah.surahNumber}:${ayah.numberInSurah}")
                            }
                        }
                    )
                }
            }
        }
    }

    tafsirAyah?.let { selected ->
        TafsirBottomSheet(
            ayah = selected,
            strings = strings,
            onlineState = onlineTafsirState,
            onLoadOnline = { onLoadOnlineTafsir(selected.surahNumber, selected.numberInSurah) },
            onDismiss = { tafsirAyah = null }
        )
    }

    noteAyah?.let { selected ->
        NoteEditorDialog(
            surahNumber = selected.surahNumber,
            ayahNumber = selected.numberInSurah,
            strings = strings,
            onSave = { title, body ->
                onSaveNote(selected, title, body)
                noteAyah = null
            },
            onDismiss = { noteAyah = null }
        )
    }

    if (showTajweedGuide) {
        TajweedGuideSheet(
            language = settings.language,
            strings = strings,
            onDismiss = { showTajweedGuide = false }
        )
    }

    if (showDisplaySheet) {
        ReaderDisplaySheet(
            settings = settings,
            strings = strings,
            onArabicSizeChange = onArabicSizeChange,
            onTranslationSizeChange = onTranslationSizeChange,
            onToggleSomali = onToggleSomali,
            onToggleEnglish = onToggleEnglish,
            onToggleTajweed = onToggleTajweed,
            onDismiss = { showDisplaySheet = false }
        )
    }
}
