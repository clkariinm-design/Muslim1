package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState
import com.example.data.local.HizbIndexEntry
import com.example.data.local.JuzIndexEntry
import com.example.data.local.SurahEntity
import com.example.localization.AppStrings
import com.example.ui.components.IslamicNumberMedallion
import com.example.ui.theme.AmiriFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SurahListScreen(
    surahs: List<SurahEntity>,
    downloadedCounts: Map<Int, Int>,
    audioState: PlaybackUiState,
    strings: AppStrings,
    onOpenSurah: (Int) -> Unit,
    onDownloadSurah: (SurahEntity) -> Unit,
    onCancelDownload: () -> Unit = {},
    juzEntries: List<JuzIndexEntry> = emptyList(),
    hizbEntries: List<HizbIndexEntry> = emptyList(),
    onOpenReader: (Int, Int) -> Unit = { s, _ -> onOpenSurah(s) },
    onOpenStudy: () -> Unit = {}
) {
    var browseMode by remember { mutableIntStateOf(0) } // 0 = Surahs, 1 = Juz, 2 = Hizb
    var filterQuery by remember { mutableStateOf("") }
    var onlyOffline by remember { mutableStateOf(false) }

    val filteredSurahs = remember(surahs, filterQuery, onlyOffline, downloadedCounts) {
        surahs.filter { s ->
            val matchesText = filterQuery.isBlank() ||
                s.englishName.contains(filterQuery, ignoreCase = true) ||
                s.englishTranslation.contains(filterQuery, ignoreCase = true) ||
                s.arabicName.contains(filterQuery) ||
                s.number.toString() == filterQuery.trim()
            val isOffline = (downloadedCounts[s.number] ?: 0) >= s.ayahCount
            matchesText && (!onlyOffline || isOffline)
        }
    }

    val filteredJuz = remember(juzEntries, filterQuery) {
        if (filterQuery.isBlank()) juzEntries
        else juzEntries.filter { j ->
            j.juz.toString() == filterQuery.trim() ||
                j.startSurahName.contains(filterQuery, ignoreCase = true)
        }
    }

    val filteredHizb = remember(hizbEntries, filterQuery) {
        if (filterQuery.isBlank()) hizbEntries
        else hizbEntries.filter { h ->
            h.hizb.toString() == filterQuery.trim() ||
                h.startSurahName.contains(filterQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.surahsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onOpenStudy,
                    modifier = Modifier.testTag("quran_open_library_button")
                ) {
                    Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openLibrary, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("surah_filter_input"),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(strings.searchPlaceholder) },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Mode Chips: Surahs / Juz / Hizb + Offline filter
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = browseMode == 0 && !onlyOffline,
                    onClick = {
                        browseMode = 0
                        onlyOffline = false
                    },
                    label = { Text("${strings.surahLabel} (${surahs.size})") },
                    modifier = Modifier.testTag("mode_chip_surahs")
                )
                FilterChip(
                    selected = browseMode == 1,
                    onClick = {
                        browseMode = 1
                        onlyOffline = false
                    },
                    label = { Text("${strings.juzLabel} (${juzEntries.size.coerceAtLeast(30)})") },
                    modifier = Modifier.testTag("mode_chip_juz")
                )
                FilterChip(
                    selected = browseMode == 2,
                    onClick = {
                        browseMode = 2
                        onlyOffline = false
                    },
                    label = { Text("${strings.hizbLabel} (${hizbEntries.size.coerceAtLeast(60)})") },
                    modifier = Modifier.testTag("mode_chip_hizb")
                )
                if (browseMode == 0) {
                    FilterChip(
                        selected = onlyOffline,
                        onClick = { onlyOffline = !onlyOffline },
                        label = { Text(strings.filterOffline) },
                        modifier = Modifier.testTag("mode_chip_offline")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (browseMode) {
                    0 -> items(filteredSurahs, key = { it.number }) { surah ->
                        val dlCount = downloadedCounts[surah.number] ?: 0
                        val isOfflineReady = dlCount >= surah.ayahCount && surah.ayahCount > 0
                        val isDownloading = audioState.downloadingSurahNumber == surah.number
                        val isFailed = audioState.lastFailedSurahNumber == surah.number
                        val revLabel = if (surah.revelationType.equals("Meccan", ignoreCase = true)) {
                            strings.meccan
                        } else {
                            strings.medinan
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSurah(surah.number) }
                                .testTag("surah_card_${surah.number}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IslamicNumberMedallion(number = surah.number, size = 44.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = surah.englishName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${surah.englishTranslation} • $revLabel • ${surah.ayahCount} ${strings.ayahsWord}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = surah.arabicName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = AmiriFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        when {
                                            isDownloading -> onCancelDownload()
                                            !isOfflineReady -> onDownloadSurah(surah)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("surah_download_${surah.number}")
                                ) {
                                    when {
                                        isDownloading -> Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                progress = { audioState.downloadProgress },
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                        }
                                        isFailed -> Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = strings.retry,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        isOfflineReady -> Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = strings.downloadedOffline,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = strings.downloadSurahAudio,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> items(filteredJuz, key = { it.juz }) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReader(entry.startSurah, entry.startAyah) }
                                .testTag("juz_card_${entry.juz}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IslamicNumberMedallion(number = entry.juz, size = 44.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${strings.juzLabel} ${entry.juz}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${entry.startSurahName} (${entry.startSurah}:${entry.startAyah}) • ${entry.ayahCount} ${strings.ayahsWord}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = entry.startSurahArabicName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = AmiriFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    else -> items(filteredHizb, key = { it.hizb }) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReader(entry.startSurah, entry.startAyah) }
                                .testTag("hizb_card_${entry.hizb}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IslamicNumberMedallion(number = entry.hizb, size = 44.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${strings.hizbLabel} ${entry.hizb}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${entry.startSurahName} (${entry.startSurah}:${entry.startAyah}) • ${entry.ayahCount} ${strings.ayahsWord}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = entry.startSurahArabicName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = AmiriFontFamily,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
