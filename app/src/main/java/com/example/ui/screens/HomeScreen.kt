package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ReadingPlanEntity
import com.example.data.local.SurahEntity
import com.example.data.local.UserSettings
import com.example.domain.CuratedCities
import com.example.domain.PrayerTimesCalculator
import com.example.localization.AppStrings
import com.example.ui.components.IslamicNumberMedallion
import com.example.ui.theme.AmiriFontFamily

@Composable
fun HomeScreen(
    settings: UserSettings,
    strings: AppStrings,
    surahs: List<SurahEntity>,
    readingPlans: List<ReadingPlanEntity>,
    onOpenReader: (Int, Int) -> Unit,
    onBrowseSurahs: () -> Unit,
    onOpenToolsTab: (Int) -> Unit,
    onOpenStudy: () -> Unit
) {
    val lastSurah = remember(surahs, settings.lastReadSurah) {
        surahs.firstOrNull { it.number == settings.lastReadSurah }
    }
    val hijri = remember(settings.timezoneId) {
        PrayerTimesCalculator.currentHijriDate(settings.timezoneId)
    }
    val prayerTimes = remember(
        settings.customLatitude,
        settings.customLongitude,
        settings.timezoneId,
        settings.calculationMethod
    ) {
        PrayerTimesCalculator.calculatePrayerTimes(
            latitude = settings.customLatitude,
            longitude = settings.customLongitude,
            timezoneId = settings.timezoneId,
            method = settings.calculationMethod
        )
    }
    val featuredSurahs = remember(surahs) {
        val ids = setOf(1, 2, 18, 36, 55, 67)
        surahs.filter { it.number in ids }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Luxury Hero Card with Architectural Arch Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(228.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_emerald_arch),
                            contentDescription = strings.appTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xBB041711),
                                            Color(0x99041711),
                                            Color(0xEE041711)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${hijri.day} ${hijri.monthNameLatin} ${hijri.year} H",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFF3D98B)
                                    )
                                    Text(
                                        text = "${hijri.day} ${hijri.monthNameAr} ${hijri.year}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = AmiriFontFamily,
                                        color = Color(0xFFF6F2E6)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x66041711),
                                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFFD4AF37),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "114 • 6,236",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF6F2E6)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color(0xFFF3D98B)
                                )
                                Text(
                                    text = strings.surahCountSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF6F2E6).copy(alpha = 0.88f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onOpenReader(settings.lastReadSurah, settings.lastReadAyah)
                                    },
                                    modifier = Modifier.testTag("home_continue_reading_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD4AF37),
                                        contentColor = Color(0xFF041711)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val label = if (lastSurah != null) {
                                        "${strings.continueReading}: ${lastSurah.englishName} (${settings.lastReadSurah}:${settings.lastReadAyah})"
                                    } else {
                                        strings.startReading
                                    }
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Access 4-tile Grid
            item {
                Text(
                    text = strings.quickAccess,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickToolTile(
                        title = strings.openPrayerTimes,
                        subtitle = prayerTimes.fajr,
                        icon = Icons.Default.AccessTime,
                        tag = "quick_prayer_tile",
                        onClick = { onOpenToolsTab(0) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolTile(
                        title = strings.openQibla,
                        subtitle = "${PrayerTimesCalculator.calculateQiblaBearing(settings.customLatitude, settings.customLongitude).toInt()}°",
                        icon = Icons.Default.Explore,
                        tag = "quick_qibla_tile",
                        onClick = { onOpenToolsTab(1) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickToolTile(
                        title = strings.openAdhkar,
                        subtitle = strings.morningAdhkar,
                        icon = Icons.Default.SelfImprovement,
                        tag = "quick_adhkar_tile",
                        onClick = { onOpenToolsTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolTile(
                        title = strings.openDuas,
                        subtitle = "Rabbana",
                        icon = Icons.Default.VolunteerActivism,
                        tag = "quick_duas_tile",
                        onClick = { onOpenToolsTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Khatam Progress & Active Plan Card
            item {
                val readCount = settings.khatamAyahsRead.coerceIn(0, 6236)
                val progress = readCount.toFloat() / 6236f
                val activePlan = readingPlans.firstOrNull { it.isActive }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStudy() }
                        .testTag("home_khatam_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = strings.khatamProgress,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$readCount / 6,236 ${strings.ayahsReadLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activePlan != null) {
                            val remaining = (6236 - readCount).coerceAtLeast(0)
                            val daily = (remaining / activePlan.targetDays.coerceAtLeast(1)).coerceAtLeast(1)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${activePlan.title} • ${strings.dailyTargetLabel}: $daily ${strings.ayahsWord}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Prayer Snapshot Banner
            item {
                val cityLabel = if (settings.useCustomGpsCoords) {
                    "GPS (${"%.2f".format(settings.customLatitude)}, ${"%.2f".format(settings.customLongitude)})"
                } else {
                    CuratedCities.findById(settings.selectedCityId).name
                }
                HomePrayerSnapshotCard(
                    cityLabel = cityLabel,
                    prayerTimes = prayerTimes,
                    strings = strings,
                    onClick = { onOpenToolsTab(0) }
                )
            }

            // Featured Surahs Horizontal Rail
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.surahsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = onBrowseSurahs,
                        modifier = Modifier.testTag("home_all_surahs_button")
                    ) {
                        Text(strings.browseAllSurahs)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(featuredSurahs, key = { it.number }) { surah ->
                        Card(
                            modifier = Modifier
                                .width(172.dp)
                                .clickable { onOpenReader(surah.number, 1) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IslamicNumberMedallion(number = surah.number, size = 34.dp)
                                    Text(
                                        text = surah.arabicName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = AmiriFontFamily,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = surah.englishName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${surah.ayahCount} ${strings.ayahsWord}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
