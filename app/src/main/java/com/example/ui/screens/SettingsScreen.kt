package com.nuur.muslim.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuur.muslim.data.local.DataManifest
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.domain.RecitersCatalog
import com.nuur.muslim.localization.AppLanguage
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.ui.theme.AppThemeMode
import com.nuur.muslim.ui.theme.ReadingTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onReciterChange: (String) -> Unit,
    onReadingThemeChange: (ReadingTheme) -> Unit = {},
    onDailyGoalChange: (Int) -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    backupFeedback: String? = null,
    onOpenLocationSettings: (() -> Unit)? = null
) {
    var reciterExpanded by remember { mutableStateOf(false) }
    var sourcesExpanded by remember { mutableStateOf(false) }
    var privacyExpanded by remember { mutableStateOf(false) }

    val readingThemeLabels = mapOf(
        ReadingTheme.DEFAULT to strings.readingThemeDefault,
        ReadingTheme.EMERALD_NIGHT to strings.readingThemeEmerald,
        ReadingTheme.WARM_SEPIA to strings.readingThemeSepia,
        ReadingTheme.AMOLED_DARK to strings.readingThemeAmoled,
        ReadingTheme.CLASSIC_PARCHMENT to strings.readingThemeParchment
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("settings_lazy_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Compact Luxury Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.settingsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(20.dp)
                        )
                    }
                }
            }

            // 1. Language, App Appearance & Reading Theme Swatches
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Palette,
                    title = strings.appearanceSectionTitle
                ) {
                    Text(
                        text = strings.languageLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = settings.language == lang
                            FilterChip(
                                selected = selected,
                                onClick = { onLanguageChange(lang) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = { Text(lang.nativeName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }

                    Text(
                        text = strings.themeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple(AppThemeMode.DARK, strings.themeDark, Icons.Default.DarkMode),
                            Triple(AppThemeMode.LIGHT, strings.themeLight, Icons.Default.LightMode),
                            Triple(AppThemeMode.SYSTEM, strings.themeSystem, Icons.Default.BrightnessAuto)
                        ).forEach { (mode, label, modeIcon) ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                leadingIcon = {
                                    Icon(modeIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("theme_chip_${mode.id}")
                            )
                        }
                    }

                    Text(
                        text = strings.readingThemeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReadingTheme.entries.forEach { rt ->
                            ReadingThemeSwatchChip(
                                theme = rt,
                                label = readingThemeLabels[rt] ?: rt.name,
                                selected = settings.readingTheme == rt,
                                onClick = { onReadingThemeChange(rt) }
                            )
                        }
                    }
                }
            }

            // 2. Reading Typography & Live Mushaf Preview
            item {
                SettingsSectionCard(
                    icon = Icons.Default.FormatSize,
                    title = strings.readingDisplaySectionTitle
                ) {
                    LiveMushafPreviewCard(settings = settings, strings = strings)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.arabicTextSize, style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${settings.arabicFontSizeSp.toInt()} sp",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Slider(
                            value = settings.arabicFontSizeSp,
                            onValueChange = onArabicSizeChange,
                            valueRange = 18f..44f
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.translationTextSize, style = MaterialTheme.typography.bodyMedium)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${settings.translationFontSizeSp.toInt()} sp",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Slider(
                            value = settings.translationFontSizeSp,
                            onValueChange = onTranslationSizeChange,
                            valueRange = 12f..24f
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsToggleRow(
                        icon = Icons.Default.Palette,
                        title = strings.tajweedColors,
                        checked = settings.showTajweedColors,
                        onCheckedChange = onToggleTajweed
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Translate,
                        title = strings.somaliTranslation,
                        checked = settings.showSomaliTranslation,
                        onCheckedChange = onToggleSomali
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Subtitles,
                        title = strings.englishTranslation,
                        checked = settings.showEnglishTranslation,
                        onCheckedChange = onToggleEnglish
                    )
                }
            }

            // 3. Recitation, Playback Speed & Daily Ayah Goal
            item {
                val activeReciter = RecitersCatalog.findById(settings.selectedReciterId)
                SettingsSectionCard(
                    icon = Icons.Default.GraphicEq,
                    title = strings.audioGoalsSectionTitle,
                    badgeText = "${settings.dailyAyahGoal} ${strings.ayahsWord}"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.dailyGoalLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${settings.dailyAyahGoal} ${strings.ayahsWord}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(10, 20, 40, 80, 100).forEach { goal ->
                            FilterChip(
                                selected = settings.dailyAyahGoal == goal,
                                onClick = { onDailyGoalChange(goal) },
                                label = { Text("$goal") },
                                modifier = Modifier.testTag("daily_goal_chip_$goal")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = strings.reciterLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = reciterExpanded,
                        onExpandedChange = { reciterExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${activeReciter.englishName} (${activeReciter.bitrate}kbps)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reciterExpanded) },
                            shape = RoundedCornerShape(14.dp),
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
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${rec.englishName} • ${rec.arabicName}", modifier = Modifier.weight(1f))
                                            if (rec.id == activeReciter.id) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onReciterChange(rec.id)
                                        reciterExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = strings.playbackSpeedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            FilterChip(
                                selected = kotlin.math.abs(settings.playbackSpeed - speed) < 0.05f,
                                onClick = { onPlaybackSpeedChange(speed) },
                                label = { Text("${speed}x") }
                            )
                        }
                    }
                }
            }

            // 4. Active Location & Prayer Timing Shortcut Card
            if (onOpenLocationSettings != null) {
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.LocationOn,
                        title = strings.locationSectionTitle,
                        badgeText = if (settings.useCustomGpsCoords) strings.gpsLocationBadge else strings.manualCityBadge
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(onClick = onOpenLocationSettings)
                                .testTag("settings_location_card"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.activeLocationDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${settings.timezoneId} • ${strings.selectCity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = strings.selectCity,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 5. Local JSON Backup & Restore Card
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Backup,
                    title = strings.backupSectionTitle,
                    badgeText = "JSON"
                ) {
                    Text(
                        text = strings.backupSectionDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportBackup,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exportBackup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onImportBackup,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.importBackup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    AnimatedVisibility(visible = !backupFeedback.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = backupFeedback.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 5. Data Sources & Licenses (Collapsible Accordion)
            item {
                ExpandableSettingsCard(
                    icon = Icons.Default.Info,
                    title = strings.dataSourcesTitle,
                    subtitle = strings.dataVerifiedBadge,
                    expanded = sourcesExpanded,
                    onToggleExpand = { sourcesExpanded = !sourcesExpanded },
                    expandLabel = strings.expandDetails
                ) {
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

            // 6. Local-First Privacy Policy Card (Always keeps badge visible + collapsible full details)
            item {
                ExpandableSettingsCard(
                    icon = Icons.Default.VerifiedUser,
                    title = strings.privacyPolicyTitle,
                    subtitle = strings.navSettings,
                    expanded = privacyExpanded,
                    onToggleExpand = { privacyExpanded = !privacyExpanded },
                    expandLabel = strings.expandDetails,
                    modifier = Modifier.testTag("privacy_policy_card"),
                    headerContent = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = strings.privacyPolicyCommitment,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                ) {
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
