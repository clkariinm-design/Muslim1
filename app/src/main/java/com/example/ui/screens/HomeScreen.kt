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
import androidx.compose.material.icons.filled.RadioButtonChecked
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.EmeraldDeepest
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.HeroOverlayBottom
import com.example.ui.theme.HeroOverlayMid
import com.example.ui.theme.HeroOverlayTop
import com.example.ui.theme.IvoryText
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

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
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val lastSurah = remember(surahs, settings.lastReadSurah) {
        surahs.firstOrNull { it.number == settings.lastReadSurah }
    }
    val hijri = remember(settings.timezoneId) {
        PrayerTimesCalculator.currentHijriDate(settings.timezoneId)
    }
    val gregorianDate = remember(settings.timezoneId, nowMillis / 60_000L) {
        val tz = runCatching { TimeZone.getTimeZone(settings.timezoneId) }.getOrElse { TimeZone.getDefault() }
        val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = tz }
        fmt.format(Date(nowMillis))
    }
    val greeting = remember(settings.timezoneId, strings, nowMillis / 300_000L) {
        val tz = runCatching { TimeZone.getTimeZone(settings.timezoneId) }.getOrElse { TimeZone.getDefault() }
        val hour = Calendar.getInstance(tz).get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> strings.greetingMorning
            hour < 17 -> strings.greetingAfternoon
            else -> strings.greetingEvening
        }
    }

    val prayerTimes = remember(
        settings.customLatitude,
        settings.customLongitude,
        settings.timezoneId,
        settings.calculationMethod,
        settings.asrMadhhab
    ) {
        PrayerTimesCalculator.calculatePrayerTimes(
            latitude = settings.customLatitude,
            longitude = settings.customLongitude,
            timezoneId = settings.timezoneId,
            method = settings.calculationMethod,
            asrMadhhab = settings.asrMadhhab
        )
    }

    val countdown = remember(
        settings.customLatitude,
        settings.customLongitude,
        settings.timezoneId,
        settings.calculationMethod,
        settings.asrMadhhab,
        nowMillis
    ) {
        PrayerTimesCalculator.calculateNextPrayerCountdown(
            latitude = settings.customLatitude,
            longitude = settings.customLongitude,
            timezoneId = settings.timezoneId,
            method = settings.calculationMethod,
            asrMadhhab = settings.asrMadhhab,
            epochMillis = nowMillis
        )
    }

    val nextPrayerLabel = when (countdown.prayerIndex) {
        0 -> strings.fajrLabel.substringBefore(" (")
        1 -> strings.sunriseLabel.substringBefore(" (")
        2 -> strings.dhuhrLabel.substringBefore(" (")
        3 -> strings.asrLabel.substringBefore(" (")
        4 -> strings.maghribLabel.substringBefore(" (")
        else -> strings.ishaLabel.substringBefore(" (")
    }

    val cityLabel = if (settings.useCustomGpsCoords) {
        "GPS (${"%.2f".format(settings.customLatitude)}, ${"%.2f".format(settings.customLongitude)})"
    } else {
        CuratedCities.findById(settings.selectedCityId).name
    }

    val featuredSurahs = remember(surahs) {
        val ids = setOf(1, 2, 18, 36, 55, 67)
        surahs.filter { it.number in ids }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("home_lazy_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Luxury Hero Card with Architectural Arch Banner & Personalized Greeting
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(252.dp)
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
                                            HeroOverlayTop,
                                            HeroOverlayMid,
                                            HeroOverlayBottom
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
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${hijri.day} ${hijri.monthNameLatin} ${hijri.year} H • $gregorianDate",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GoldLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${hijri.day} ${hijri.monthNameAr} ${hijri.year}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = AmiriFontFamily,
                                        color = IvoryText
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = HeroOverlayMid,
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "114 • 6,236",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IvoryText
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = greeting,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldLight.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = GoldLight
                                )
                                Text(
                                    text = strings.surahCountSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IvoryText.copy(alpha = 0.88f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        onOpenReader(settings.lastReadSurah, settings.lastReadAyah)
                                    },
                                    modifier = Modifier.testTag("home_continue_reading_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = EmeraldDeepest
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

            // Live Next Prayer Countdown Banner
            item {
                HomeNextPrayerCountdownBanner(
                    nextPrayerName = nextPrayerLabel,
                    countdown = countdown,
                    cityLabel = cityLabel,
                    strings = strings,
                    onClick = { onOpenToolsTab(0) }
                )
            }

            // Daily Worship Overview + Goal + Streak + Tasbih Shortcut
            item {
                HomeDailyWorshipOverviewCard(
                    settings = settings,
                    strings = strings,
                    onOpenTasbih = { onOpenToolsTab(3) },
                    onOpenAdhkar = { onOpenToolsTab(2) }
                )
            }

            // Quick Access 4+1 Worship Grid
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
                        title = strings.openTasbih,
                        subtitle = "${settings.tasbihCount} / ${if (settings.tasbihTarget > 0) settings.tasbihTarget else "∞"}",
                        icon = Icons.Default.RadioButtonChecked,
                        tag = "quick_tasbih_tile",
                        onClick = { onOpenToolsTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                QuickToolTile(
                    title = strings.openDuas,
                    subtitle = "Rabbana • 22 Duas",
                    icon = Icons.Default.VolunteerActivism,
                    tag = "quick_duas_tile",
                    onClick = { onOpenToolsTab(4) },
                    modifier = Modifier.fillMaxWidth()
                )
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
