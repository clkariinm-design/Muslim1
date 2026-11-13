package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState
import com.example.data.local.AudioRepeatMode
import com.example.domain.RecitersCatalog
import com.example.localization.AppStrings

@Composable
fun IslamicNumberMedallion(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val side = this.size.minDimension * 0.68f
            val topLeft = Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f)
            drawRect(
                color = surface,
                topLeft = topLeft,
                size = Size(side, side)
            )
            drawRect(
                color = primary,
                topLeft = topLeft,
                size = Size(side, side),
                style = Stroke(width = 2.5f)
            )
            rotate(45f) {
                drawRect(
                    color = primary,
                    topLeft = topLeft,
                    size = Size(side, side),
                    style = Stroke(width = 2.5f)
                )
            }
        }
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun GoldOrnamentalDivider(modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = outline)
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = outline)
    }
}

@Composable
fun MiniAudioPlayerBar(
    audioState: PlaybackUiState,
    strings: AppStrings,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onDismiss: () -> Unit,
    onOpenReader: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    onExpandPlayer: () -> Unit = {},
    onRetryDownload: (Int) -> Unit = {}
) {
    if (!audioState.isActive && audioState.lastFailedSurahNumber == null) return
    val reciter = RecitersCatalog.findById(audioState.reciterId)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_audio_player"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (audioState.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExpandPlayer,
                        modifier = Modifier.testTag("audio_expand_button")
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = strings.openFullPlayer,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onOpenReader(audioState.currentSurahNumber, audioState.currentAyahNumber)
                            }
                    ) {
                        Text(
                            text = "${audioState.currentSurahName} • ${audioState.currentSurahNumber}:${audioState.currentAyahNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${reciter.englishName} • ${formatSpeed(audioState.playbackSpeed)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.testTag("audio_prev_button")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous ayah")
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("audio_play_pause_button")
                    ) {
                        if (audioState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (audioState.isPlaying) strings.pause else strings.playAyah,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.testTag("audio_next_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next ayah")
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier.testTag("audio_repeat_button")
                    ) {
                        val repeatIcon = if (audioState.repeatMode == AudioRepeatMode.AYAH) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        }
                        val tint = if (audioState.repeatMode == AudioRepeatMode.OFF) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Icon(repeatIcon, contentDescription = strings.repeatAyah, tint = tint)
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("audio_close_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (audioState.durationMs > 0L) {
                    val progress = (audioState.positionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDurationMs(audioState.positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = progress,
                            onValueChange = { frac ->
                                onSeek((frac * audioState.durationMs).toLong())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = formatDurationMs(audioState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            audioState.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val failedSurah = audioState.lastFailedSurahNumber
                    if (failedSurah != null) {
                        TextButton(
                            onClick = { onRetryDownload(failedSurah) },
                            modifier = Modifier.testTag("audio_retry_download_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(strings.retry)
                        }
                    }
                }
            }
        }
    }
}

internal fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}

internal fun formatSpeed(speed: Float): String {
    val rounded = kotlin.math.round(speed * 100f) / 100f
    return if (rounded == 1.0f) "1.0x" else "${rounded}x"
}
