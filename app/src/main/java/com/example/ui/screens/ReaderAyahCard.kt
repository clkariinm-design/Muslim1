package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AyahEntity
import com.example.data.local.UserSettings
import com.example.domain.TajweedParser
import com.example.localization.AppStrings
import com.example.ui.components.IslamicNumberMedallion
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.ReaderThemePalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderAyahCard(
    ayah: AyahEntity,
    settings: UserSettings,
    strings: AppStrings,
    isBookmarked: Boolean,
    isPlayingThis: Boolean,
    onPlayAyah: () -> Unit,
    onOpenTafsir: () -> Unit,
    onOpenNote: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkLastRead: () -> Unit,
    palette: ReaderThemePalette? = null,
    onCopyAyah: () -> Unit = {},
    onShareAyah: () -> Unit = {}
) {
    val cardBg = palette?.cardSurface ?: MaterialTheme.colorScheme.surface
    val cardBorder = palette?.cardBorder ?: MaterialTheme.colorScheme.outline
    val arabicColor = palette?.arabicText ?: MaterialTheme.colorScheme.onSurface
    val translationColor = palette?.translationText ?: MaterialTheme.colorScheme.onSurface
    val mutedColor = palette?.secondaryText ?: MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = palette?.accent ?: MaterialTheme.colorScheme.primary

    val borderStroke = if (isPlayingThis) {
        BorderStroke(1.8.dp, accentColor)
    } else {
        BorderStroke(1.dp, cardBorder)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ayah_card_${ayah.surahNumber}_${ayah.numberInSurah}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingThis) {
                accentColor.copy(alpha = 0.14f)
            } else {
                cardBg
            }
        ),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${strings.juzLabel} ${ayah.juz} • ${strings.pageLabel} ${ayah.page}",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (ayah.sajdah) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = strings.sajdahLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${ayah.surahNumber}:${ayah.numberInSurah}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                        color = arabicColor,
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
                    color = translationColor
                )
            }

            if (settings.showEnglishTranslation) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ayah.textEnglish,
                    fontSize = (settings.translationFontSizeSp - 0.5f).coerceAtLeast(12f).sp,
                    lineHeight = (settings.translationFontSizeSp * 1.42f).sp,
                    color = mutedColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onPlayAyah,
                    modifier = Modifier.testTag("play_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = strings.playAyah,
                        tint = accentColor
                    )
                }
                IconButton(
                    onClick = onCopyAyah,
                    modifier = Modifier.testTag("copy_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = strings.copyAyah,
                        tint = mutedColor
                    )
                }
                IconButton(
                    onClick = onShareAyah,
                    modifier = Modifier.testTag("share_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = strings.shareAyah,
                        tint = mutedColor
                    )
                }
                IconButton(
                    onClick = onOpenTafsir,
                    modifier = Modifier.testTag("tafsir_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = strings.tafsirTitle,
                        tint = mutedColor
                    )
                }
                IconButton(
                    onClick = onOpenNote,
                    modifier = Modifier.testTag("note_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = strings.addNote, tint = mutedColor)
                }
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.testTag("bookmark_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = strings.bookmarksTab,
                        tint = if (isBookmarked) accentColor else mutedColor
                    )
                }
                IconButton(
                    onClick = onMarkLastRead,
                    modifier = Modifier.testTag("lastread_ayah_${ayah.numberInSurah}")
                ) {
                    Icon(
                        Icons.Default.CheckCircleOutline,
                        contentDescription = strings.markAsLastRead,
                        tint = mutedColor
                    )
                }
            }
        }
    }
}
