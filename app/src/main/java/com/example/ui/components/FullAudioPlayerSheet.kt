package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState
import com.example.data.local.AudioRepeatMode
import com.example.data.local.SurahEntity
import com.example.domain.RecitersCatalog
import com.example.localization.AppStrings
import com.example.ui.theme.AmiriFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FullAudioPlayerSheet(
    audioState: PlaybackUiState,
    surah: SurahEntity?,
    downloadedCount: Int,
    strings: AppStrings,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onReciterChange: (String) -> Unit,
    onDownloadSurah: (SurahEntity) -> Unit,
    onCancelDownload: () -> Unit,
    onOpenReader: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val activeReciter = RecitersCatalog.findById(audioState.reciterId)
    var reciterMenuExpanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
    val isOfflineReady = surah != null && surah.ayahCount > 0 && downloadedCount >= surah.ayahCount
    val isDownloading = surah != null && audioState.downloadingSurahNumber == surah.number

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("full_audio_player_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.nowPlayingLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IslamicNumberMedallion(number = audioState.currentSurahNumber, size = 54.dp)
                    if (surah != null) {
                        Text(
                            text = surah.arabicName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = AmiriFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "${audioState.currentSurahName} • ${audioState.currentSurahNumber}:${audioState.currentAyahNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = activeReciter.englishName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scrubber
            val progress = if (audioState.durationMs > 0L) {
                (audioState.positionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    enabled = audioState.durationMs > 0L,
                    onValueChange = { frac ->
                        onSeek((frac * audioState.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMs(audioState.positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDurationMs(audioState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(48.dp)
                ) {
                    val icon = if (audioState.repeatMode == AudioRepeatMode.AYAH) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    val tint = if (audioState.repeatMode == AudioRepeatMode.OFF) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Icon(icon, contentDescription = strings.repeatAyah, tint = tint)
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous ayah", modifier = Modifier.size(30.dp))
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("full_player_play_pause")
                ) {
                    if (audioState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (audioState.isPlaying) strings.pause else strings.playAyah,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next ayah", modifier = Modifier.size(30.dp))
                }

                val repeatText = when (audioState.repeatMode) {
                    AudioRepeatMode.OFF -> strings.repeatOff
                    AudioRepeatMode.AYAH -> strings.repeatAyah
                    AudioRepeatMode.SURAH -> strings.repeatSurah
                }
                Text(
                    text = repeatText.substringAfter(":").trim().take(6),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Playback Speed Pills
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.playbackSpeedLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speeds.forEach { sp ->
                        val selected = kotlin.math.abs(audioState.playbackSpeed - sp) < 0.05f
                        FilterChip(
                            selected = selected,
                            onClick = { onSpeedChange(sp) },
                            label = { Text("${sp}x") },
                            modifier = Modifier.testTag("speed_chip_$sp")
                        )
                    }
                }
            }

            // Reciter Selector inside Player
            ExposedDropdownMenuBox(
                expanded = reciterMenuExpanded,
                onExpandedChange = { reciterMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${activeReciter.englishName} (${activeReciter.bitrate}kbps)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.reciterLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = reciterMenuExpanded,
                    onDismissRequest = { reciterMenuExpanded = false }
                ) {
                    RecitersCatalog.reciters.forEach { rec ->
                        DropdownMenuItem(
                            text = { Text("${rec.englishName} • ${rec.arabicName}") },
                            onClick = {
                                onReciterChange(rec.id)
                                reciterMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Bottom action buttons: Download / Offline + Jump to Reader
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (surah != null) {
                    OutlinedButton(
                        onClick = {
                            when {
                                isDownloading -> onCancelDownload()
                                !isOfflineReady -> onDownloadSurah(surah)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    progress = { audioState.downloadProgress },
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${(audioState.downloadProgress * 100).toInt()}%")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Close, contentDescription = strings.cancelDownload, modifier = Modifier.size(16.dp))
                            }
                            isOfflineReady -> {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.downloadedOffline, maxLines = 1)
                            }
                            else -> {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.filterOffline, maxLines = 1)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onOpenReader(audioState.currentSurahNumber, audioState.currentAyahNumber)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${audioState.currentSurahNumber}:${audioState.currentAyahNumber}", maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
