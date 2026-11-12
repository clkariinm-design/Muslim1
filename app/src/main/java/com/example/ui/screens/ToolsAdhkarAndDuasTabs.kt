package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AdhkarItem
import com.example.data.local.AyahEntity
import com.example.data.local.UserSettings
import com.example.domain.PrayerTimesCalculator
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.AmiriFontFamily

@Composable
internal fun AdhkarAndHijriTab(
    settings: UserSettings,
    strings: AppStrings,
    adhkarList: List<AdhkarItem>,
    adhkarCounters: Map<Int, Int>,
    onIncrementDhikr: (Int, Int) -> Unit
) {
    var filterType by remember { mutableIntStateOf(-1) }
    val hijri = remember(settings.timezoneId) {
        PrayerTimesCalculator.currentHijriDate(settings.timezoneId)
    }
    val filtered = remember(adhkarList, filterType) {
        if (filterType == -1) adhkarList else adhkarList.filter { it.type == filterType || it.type == 0 }
    }
    val isArabic = settings.language == AppLanguage.ARABIC

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        strings.hijriCalendarTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${hijri.day} ${hijri.monthNameLatin} ${hijri.year} AH",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "${hijri.day} ${hijri.monthNameAr} ${hijri.year}",
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (settings.language == AppLanguage.SOMALI) {
            item {
                Text(
                    text = strings.adhkarSomaliNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterType == -1,
                    onClick = { filterType = -1 },
                    label = { Text(strings.filterAll) }
                )
                FilterChip(
                    selected = filterType == 1,
                    onClick = { filterType = 1 },
                    label = { Text(strings.morningAdhkar) }
                )
                FilterChip(
                    selected = filterType == 2,
                    onClick = { filterType = 2 },
                    label = { Text(strings.eveningAdhkar) }
                )
            }
        }

        items(filtered, key = { it.order }) { item ->
            val currentCount = adhkarCounters[item.order] ?: 0
            val countDesc = if (isArabic && item.countDescAr.isNotBlank()) {
                item.countDescAr
            } else {
                item.countDescEn
            }
            val sourceText = if (isArabic && item.sourceAr.isNotBlank()) {
                item.sourceAr
            } else {
                item.sourceEn
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = item.arabic,
                            fontFamily = AmiriFontFamily,
                            fontSize = 22.sp,
                            lineHeight = 38.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!isArabic && item.transliteration.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.transliteration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!isArabic && item.english.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.english,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (sourceText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onIncrementDhikr(item.order, item.count) },
                        modifier = Modifier.testTag("dhikr_counter_${item.order}")
                    ) {
                        Text("$currentCount / ${item.count} ($countDesc)")
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuranicDuasTab(
    strings: AppStrings,
    duas: List<AyahEntity>,
    onOpenReader: (Int, Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.quranicDuasTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(duas, key = { it.globalNumber }) { ayah ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReader(ayah.surahNumber, ayah.numberInSurah) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${strings.surahLabel} ${ayah.surahNumber}:${ayah.numberInSurah}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = ayah.textUthmani,
                            fontFamily = AmiriFontFamily,
                            fontSize = 23.sp,
                            lineHeight = 38.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = ayah.textSomali, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ayah.textEnglish,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
