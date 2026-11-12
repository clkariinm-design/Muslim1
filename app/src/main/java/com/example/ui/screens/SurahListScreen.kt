package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState
import com.example.data.local.SurahEntity
import com.example.localization.AppStrings
import com.example.ui.components.IslamicNumberMedallion
import com.example.ui.theme.AmiriFontFamily

@Composable
fun SurahListScreen(
    surahs: List<SurahEntity>,
    downloadedCounts: Map<Int, Int>,
    audioState: PlaybackUiState,
    strings: AppStrings,
    onOpenSurah: (Int) -> Unit,
    onDownloadSurah: (SurahEntity) -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    var onlyOffline by remember { mutableStateOf(false) }

    val filtered = remember(surahs, filterQuery, onlyOffline, downloadedCounts) {
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.surahsTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !onlyOffline,
                    onClick = { onlyOffline = false },
                    label = { Text("${strings.filterAll} (${surahs.size})") }
                )
                FilterChip(
                    selected = onlyOffline,
                    onClick = { onlyOffline = true },
                    label = { Text(strings.filterOffline) }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.number }) { surah ->
                    val dlCount = downloadedCounts[surah.number] ?: 0
                    val isOfflineReady = dlCount >= surah.ayahCount && surah.ayahCount > 0
                    val isDownloading = audioState.downloadingSurahNumber == surah.number
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
                                onClick = { if (!isOfflineReady && !isDownloading) onDownloadSurah(surah) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("surah_download_${surah.number}")
                            ) {
                                when {
                                    isDownloading -> CircularProgressIndicator(
                                        progress = { audioState.downloadProgress },
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
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
            }
        }
    }
}
