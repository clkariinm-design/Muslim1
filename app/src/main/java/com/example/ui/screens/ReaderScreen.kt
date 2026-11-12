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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackUiState
import com.example.data.local.AyahEntity
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.domain.TajweedParser
import com.example.localization.AppStrings
import com.example.ui.components.GoldOrnamentalDivider
import com.example.ui.components.IslamicNumberMedallion
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
                // Surah Banner Header
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
                                            OutlinedButton(onClick = {}) {
                                                CircularProgressIndicator(
                                                    progress = { audioState.downloadProgress },
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${(audioState.downloadProgress * 100).toInt()}%")
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

                // Ayah Cards
                itemsIndexed(ayahs, key = { _, a -> a.globalNumber }) { index, ayah ->
                    val isBookmarked = ayah.globalNumber in bookmarkedGlobals
                    val isPlayingThis = audioState.isActive && audioState.currentGlobalAyah == ayah.globalNumber
                    val borderStroke = if (isPlayingThis) {
                        BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ayah_card_${ayah.surahNumber}_${ayah.numberInSurah}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlayingThis) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = borderStroke
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row: Medallion + Arabic Metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IslamicNumberMedallion(number = ayah.numberInSurah, size = 36.dp)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${strings.juzLabel} ${ayah.juz} • ${strings.pageLabel} ${ayah.page}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (ayah.sajdah) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = strings.sajdahLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${ayah.surahNumber}:${ayah.numberInSurah}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Uthmani Arabic (with optional Tajweed)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                val lineHeightSp = (settings.arabicFontSizeSp * 1.75f).sp
                                if (settings.showTajweedColors) {
                                    val annotated = remember(ayah.textTajweed) {
                                        TajweedParser.parseToAnnotatedString(ayah.textTajweed)
                                    }
                                    Text(
                                        text = annotated,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = settings.arabicFontSizeSp.sp,
                                        lineHeight = lineHeightSp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = ayah.textUthmani,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = settings.arabicFontSizeSp.sp,
                                        lineHeight = lineHeightSp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (settings.showSomaliTranslation) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = ayah.textSomali,
                                    fontSize = settings.translationFontSizeSp.sp,
                                    lineHeight = (settings.translationFontSizeSp * 1.48f).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (settings.showEnglishTranslation) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = ayah.textEnglish,
                                    fontSize = (settings.translationFontSizeSp - 0.5f).coerceAtLeast(12f).sp,
                                    lineHeight = (settings.translationFontSizeSp * 1.42f).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Row (48dp touch targets)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        if (surah != null) onPlaySurah(surah, ayahs, index)
                                    },
                                    modifier = Modifier.testTag("play_ayah_${ayah.numberInSurah}")
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = strings.playAyah,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onResetTafsir()
                                        tafsirAyah = ayah
                                    },
                                    modifier = Modifier.testTag("tafsir_ayah_${ayah.numberInSurah}")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = strings.tafsirTitle
                                    )
                                }
                                IconButton(
                                    onClick = { noteAyah = ayah },
                                    modifier = Modifier.testTag("note_ayah_${ayah.numberInSurah}")
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = strings.addNote)
                                }
                                IconButton(
                                    onClick = {
                                        onToggleBookmark(ayah)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isBookmarked) strings.bookmarkRemoved else strings.bookmarkAdded
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("bookmark_ayah_${ayah.numberInSurah}")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = strings.bookmarksTab,
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onMarkLastRead(ayah)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${strings.markAsLastRead}: ${ayah.surahNumber}:${ayah.numberInSurah}")
                                        }
                                    },
                                    modifier = Modifier.testTag("lastread_ayah_${ayah.numberInSurah}")
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircleOutline,
                                        contentDescription = strings.markAsLastRead
                                    )
                                }
                            }
                        }
                    }
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
