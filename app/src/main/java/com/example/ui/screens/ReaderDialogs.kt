package com.nuur.muslim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.domain.TajweedParser
import com.nuur.muslim.localization.AppLanguage
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.ui.theme.AmiriFontFamily
import com.nuur.muslim.ui.theme.ReadingTheme
import com.nuur.muslim.ui.viewmodel.OnlineTafsirState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    ayah: AyahEntity,
    strings: AppStrings,
    onlineState: OnlineTafsirState,
    onLoadOnline: () -> Unit,
    onDismiss: () -> Unit,
    onCopyTafsir: (String) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.tafsirTitle} (${ayah.surahNumber}:${ayah.numberInSurah})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        val combined = buildString {
                            append("${ayah.surahNumber}:${ayah.numberInSurah}\n${ayah.textUthmani}\n\n${strings.tafsirMuyassarLabel}:\n${ayah.tafsirMuyassar}")
                            if (onlineState is OnlineTafsirState.Success) {
                                append("\n\n${strings.tafsirIbnKathirLabel}:\n${onlineState.text}")
                            }
                        }
                        onCopyTafsir(combined)
                    },
                    modifier = Modifier.testTag("copy_tafsir_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.copyTafsir)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = ayah.textUthmani,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = AmiriFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.tafsirMuyassarLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = ayah.tafsirMuyassar,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = AmiriFontFamily,
                    fontSize = 18.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.tafsirIbnKathirLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (onlineState) {
                OnlineTafsirState.Idle -> {
                    OutlinedButton(
                        onClick = onLoadOnline,
                        modifier = Modifier.testTag("load_online_tafsir_button")
                    ) {
                        Text(strings.loadOnlineTafsir)
                    }
                }
                OnlineTafsirState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(strings.downloading, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is OnlineTafsirState.Success -> {
                    Text(
                        text = onlineState.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
                is OnlineTafsirState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.onlineTafsirError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(onClick = onLoadOnline) {
                            Text(strings.retry)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun NoteEditorDialog(
    surahNumber: Int,
    ayahNumber: Int,
    initialTitle: String = "",
    initialBody: String = "",
    strings: AppStrings,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var body by remember { mutableStateOf(initialBody) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${strings.addNote} ($surahNumber:$ayahNumber)")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.noteTitleHint) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(strings.noteBodyHint) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, body) },
                enabled = body.isNotBlank(),
                modifier = Modifier.testTag("note_save_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
fun JumpToAyahDialog(
    maxAyah: Int,
    strings: AppStrings,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("1") }
    val parsed = input.toIntOrNull()?.coerceIn(1, maxAyah.coerceAtLeast(1))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.jumpToAyah) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1 — $maxAyah", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw -> input = raw.filter { it.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_ayah_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsed != null) onJump(parsed)
                    onDismiss()
                },
                enabled = parsed != null,
                modifier = Modifier.testTag("jump_ayah_confirm")
            ) {
                Text(strings.jumpToAyah)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedGuideSheet(
    language: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.tajweedGuideTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            TajweedParser.rules.forEach { rule ->
                val title = when (language) {
                    AppLanguage.SOMALI -> rule.nameSo
                    AppLanguage.ARABIC -> rule.nameAr
                    AppLanguage.ENGLISH -> rule.nameEn
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(rule.color, CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = rule.nameAr,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = AmiriFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderDisplaySheet(
    settings: UserSettings,
    strings: AppStrings,
    onArabicSizeChange: (Float) -> Unit,
    onTranslationSizeChange: (Float) -> Unit,
    onToggleSomali: (Boolean) -> Unit,
    onToggleEnglish: (Boolean) -> Unit,
    onToggleTajweed: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onReadingThemeChange: (ReadingTheme) -> Unit = {}
) {
    val themeLabels = mapOf(
        ReadingTheme.DEFAULT to strings.readingThemeDefault,
        ReadingTheme.EMERALD_NIGHT to strings.readingThemeEmerald,
        ReadingTheme.WARM_SEPIA to strings.readingThemeSepia,
        ReadingTheme.AMOLED_DARK to strings.readingThemeAmoled,
        ReadingTheme.CLASSIC_PARCHMENT to strings.readingThemeParchment
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(strings.readingThemeLabel, style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadingTheme.entries.forEach { rt ->
                    FilterChip(
                        selected = settings.readingTheme == rt,
                        onClick = { onReadingThemeChange(rt) },
                        label = { Text(themeLabels[rt] ?: rt.name) },
                        modifier = Modifier.testTag("reader_theme_${rt.id}")
                    )
                }
            }
            Text("${strings.arabicTextSize}: ${settings.arabicFontSizeSp.toInt()}sp")
            Slider(
                value = settings.arabicFontSizeSp,
                onValueChange = onArabicSizeChange,
                valueRange = 18f..44f
            )
            Text("${strings.translationTextSize}: ${settings.translationFontSizeSp.toInt()}sp")
            Slider(
                value = settings.translationFontSizeSp,
                onValueChange = onTranslationSizeChange,
                valueRange = 12f..24f
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.tajweedColors)
                Switch(checked = settings.showTajweedColors, onCheckedChange = onToggleTajweed)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.somaliTranslation)
                Switch(checked = settings.showSomaliTranslation, onCheckedChange = onToggleSomali)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.englishTranslation)
                Switch(checked = settings.showEnglishTranslation, onCheckedChange = onToggleEnglish)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
