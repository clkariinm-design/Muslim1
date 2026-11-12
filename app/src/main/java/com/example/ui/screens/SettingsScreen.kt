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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DataManifest
import com.example.data.local.UserSettings
import com.example.domain.RecitersCatalog
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    strings: AppStrings,
    manifest: DataManifest?,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit,
    onArabicSizeChange: (Float) -> Unit,
    onTranslationSizeChange: (Float) -> Unit,
    onToggleSomali: (Boolean) -> Unit,
    onToggleEnglish: (Boolean) -> Unit,
    onToggleTajweed: (Boolean) -> Unit,
    onReciterChange: (String) -> Unit
) {
    var reciterExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Language & Theme Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    label = { Text(lang.nativeName) },
                                    modifier = Modifier.testTag("lang_chip_${lang.code}")
                                )
                            }
                        }

                        Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AppThemeMode.DARK to strings.themeDark,
                                AppThemeMode.LIGHT to strings.themeLight,
                                AppThemeMode.SYSTEM to strings.themeSystem
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { onThemeChange(mode) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Typography & Translations Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                fontFamily = AmiriFontFamily,
                                fontSize = settings.arabicFontSizeSp.sp,
                                lineHeight = (settings.arabicFontSizeSp * 1.6f).sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                    }
                }
            }

            // Reciter Selection Card
            item {
                val activeReciter = RecitersCatalog.findById(settings.selectedReciterId)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.reciterLabel, style = MaterialTheme.typography.titleMedium)
                        ExposedDropdownMenuBox(
                            expanded = reciterExpanded,
                            onExpandedChange = { reciterExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "${activeReciter.englishName} (${activeReciter.bitrate}kbps)",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = reciterExpanded,
                                onDismissRequest = { reciterExpanded = false }
                            ) {
                                RecitersCatalog.reciters.forEach { rec ->
                                    DropdownMenuItem(
                                        text = { Text("${rec.englishName} • ${rec.arabicName}") },
                                        onClick = {
                                            onReciterChange(rec.id)
                                            reciterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Data Sources, Licenses & Integrity Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.dataSourcesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(strings.dataVerifiedBadge, style = MaterialTheme.typography.labelMedium)
                        if (manifest != null) {
                            Text(
                                text = "SHA-256: ${manifest.quranBundleUncompressedSha256.take(16)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            manifest.editions.forEach { (k, v) ->
                                Text(
                                    text = "• $k: $v",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Text(
                            text = "• Audio CDN: Islamic Network / EveryAyah (https://cdn.islamic.network)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Online Tafsir: Quran.com API v4 (Tafsir Ibn Kathir Abridged, ID 169)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Draft Privacy Policy Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.privacyPolicyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = strings.privacyPolicyDraftNotice,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Text(
                            text = strings.privacyPolicyBody,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
