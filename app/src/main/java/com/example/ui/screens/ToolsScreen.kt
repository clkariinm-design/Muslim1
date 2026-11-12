package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.AdhkarItem
import com.example.data.local.AyahEntity
import com.example.data.local.UserSettings
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCities
import com.example.domain.CuratedCity
import com.example.domain.PrayerTimesCalculator
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.theme.AmiriFontFamily
import kotlin.math.abs

@Composable
fun ToolsScreen(
    initialTab: Int,
    settings: UserSettings,
    strings: AppStrings,
    adhkarList: List<AdhkarItem>,
    adhkarCounters: Map<Int, Int>,
    quranicDuas: List<AyahEntity>,
    deviceAzimuth: Float?,
    hasCompassSensor: Boolean,
    locationFeedback: String?,
    onSelectCity: (CuratedCity) -> Unit,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectTimezone: (String) -> Unit,
    onRequestDeviceLocation: () -> Unit,
    onLocationDenied: (String) -> Unit,
    onStartCompass: () -> Unit,
    onStopCompass: () -> Unit,
    onIncrementDhikr: (Int, Int) -> Unit,
    onOpenReader: (Int, Int) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf(
        strings.openPrayerTimes,
        strings.openQibla,
        strings.openAdhkar,
        strings.openDuas
    )

    DisposableEffect(selectedTab) {
        if (selectedTab == 1) onStartCompass() else onStopCompass()
        onDispose { onStopCompass() }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("tools_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> PrayerTimesTab(
                    settings = settings,
                    strings = strings,
                    locationFeedback = locationFeedback,
                    onSelectCity = onSelectCity,
                    onSelectMethod = onSelectMethod,
                    onSelectTimezone = onSelectTimezone,
                    onRequestDeviceLocation = onRequestDeviceLocation,
                    onLocationDenied = onLocationDenied
                )
                1 -> QiblaTab(
                    settings = settings,
                    strings = strings,
                    deviceAzimuth = deviceAzimuth,
                    hasCompassSensor = hasCompassSensor
                )
                2 -> AdhkarAndHijriTab(
                    settings = settings,
                    strings = strings,
                    adhkarList = adhkarList,
                    adhkarCounters = adhkarCounters,
                    onIncrementDhikr = onIncrementDhikr
                )
                3 -> QuranicDuasTab(
                    strings = strings,
                    duas = quranicDuas,
                    onOpenReader = onOpenReader
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerTimesTab(
    settings: UserSettings,
    strings: AppStrings,
    locationFeedback: String?,
    onSelectCity: (CuratedCity) -> Unit,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectTimezone: (String) -> Unit,
    onRequestDeviceLocation: () -> Unit,
    onLocationDenied: (String) -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onRequestDeviceLocation()
        } else {
            onLocationDenied(strings.locationPermissionDenied)
        }
    }

    val times = remember(
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

    var cityMenuOpen by remember { mutableStateOf(false) }
    var methodMenuOpen by remember { mutableStateOf(false) }
    var tzMenuOpen by remember { mutableStateOf(false) }

    val selectedCity = CuratedCities.findById(settings.selectedCityId)
    val timezones = listOf(
        "Africa/Mogadishu", "Africa/Djibouti", "Africa/Nairobi", "Africa/Addis_Ababa",
        "Asia/Riyadh", "Africa/Cairo", "Europe/Istanbul", "Europe/London",
        "America/Chicago", "America/Toronto", "Asia/Kuala_Lumpur", "UTC"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Local Verification Disclaimer
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.prayerVerificationNotice,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Timetable Card
        item {
            val rows = listOf(
                "Fajr / Subax" to times.fajr,
                "Sunrise / Qorrax-soo-bax" to times.sunrise,
                "Dhuhr / Duhur" to times.dhuhr,
                "Asr / Casar" to times.asr,
                "Maghrib / Maqrib" to times.maghrib,
                "Isha / Cishe" to times.isha
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.prayerTimesTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    rows.forEachIndexed { idx, (label, time) ->
                        val isNext = idx == times.nextPrayerIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNext) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // City / Optional GPS / Method / Timezone Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = cityMenuOpen,
                        onExpandedChange = { cityMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = if (settings.useCustomGpsCoords) {
                                "GPS (${"%.2f".format(settings.customLatitude)}, ${"%.2f".format(settings.customLongitude)})"
                            } else {
                                selectedCity.name
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.selectCity) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = cityMenuOpen,
                            onDismissRequest = { cityMenuOpen = false }
                        ) {
                            CuratedCities.cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text("${city.name} (${city.country})") },
                                    onClick = {
                                        onSelectCity(city)
                                        cityMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = strings.locationPurposeNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onRequestDeviceLocation()
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_device_location_button")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.useDeviceLocation)
                    }

                    locationFeedback?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = methodMenuOpen,
                        onExpandedChange = { methodMenuOpen = it }
                    ) {
                        val methodLabel = when (settings.language) {
                            AppLanguage.SOMALI -> settings.calculationMethod.nameSo
                            AppLanguage.ARABIC -> settings.calculationMethod.nameAr
                            AppLanguage.ENGLISH -> settings.calculationMethod.nameEn
                        }
                        OutlinedTextField(
                            value = methodLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.calculationMethod) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = methodMenuOpen,
                            onDismissRequest = { methodMenuOpen = false }
                        ) {
                            CalculationMethod.entries.forEach { m ->
                                val label = when (settings.language) {
                                    AppLanguage.SOMALI -> m.nameSo
                                    AppLanguage.ARABIC -> m.nameAr
                                    AppLanguage.ENGLISH -> m.nameEn
                                }
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onSelectMethod(m)
                                        methodMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = tzMenuOpen,
                        onExpandedChange = { tzMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = settings.timezoneId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.timezoneLabel) },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tzMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = tzMenuOpen,
                            onDismissRequest = { tzMenuOpen = false }
                        ) {
                            timezones.forEach { tz ->
                                DropdownMenuItem(
                                    text = { Text(tz) },
                                    onClick = {
                                        onSelectTimezone(tz)
                                        tzMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QiblaTab(
    settings: UserSettings,
    strings: AppStrings,
    deviceAzimuth: Float?,
    hasCompassSensor: Boolean
) {
    val qiblaBearing = remember(settings.customLatitude, settings.customLongitude) {
        PrayerTimesCalculator.calculateQiblaBearing(settings.customLatitude, settings.customLongitude)
    }
    val heading = deviceAzimuth ?: 0f
    val dialRotation by animateFloatAsState(
        targetValue = -heading,
        animationSpec = spring(),
        label = "qiblaDial"
    )
    val delta = abs(((qiblaBearing - heading + 540.0) % 360.0) - 180.0)
    val isAligned = deviceAzimuth != null && delta <= 6.0

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.qiblaTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = primaryColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${strings.qiblaBearingLabel}: ${"%.1f".format(qiblaBearing)}°",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 12f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = surfaceColor, radius = radius, center = center)
                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 6f)
                )
                rotate(dialRotation, pivot = center) {
                    // North tick
                    drawLine(
                        color = onSurface.copy(alpha = 0.6f),
                        start = Offset(center.x, center.y - radius + 8f),
                        end = Offset(center.x, center.y - radius + 28f),
                        strokeWidth = 5f
                    )
                    // Qibla needle pointed toward qiblaBearing
                    rotate(qiblaBearing.toFloat(), pivot = center) {
                        val arrow = Path().apply {
                            moveTo(center.x, center.y - radius + 28f)
                            lineTo(center.x - 16f, center.y + 18f)
                            lineTo(center.x + 16f, center.y + 18f)
                            close()
                        }
                        drawPath(arrow, color = primaryColor)
                    }
                }
                drawCircle(color = primaryColor, radius = 12f, center = center)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        if (isAligned) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = strings.qiblaAligned,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        } else if (!hasCompassSensor || deviceAzimuth == null) {
            Text(
                text = strings.sensorUnavailableNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdhkarAndHijriTab(
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
                    Text(strings.hijriCalendarTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${hijri.day} ${hijri.monthNameLatin} ${hijri.year} AH", style = MaterialTheme.typography.headlineSmall)
                    Text("${hijri.day} ${hijri.monthNameAr} ${hijri.year}", fontFamily = AmiriFontFamily, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.transliteration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.english,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.sourceEn.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.sourceEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onIncrementDhikr(item.order, item.count) },
                        modifier = Modifier.testTag("dhikr_counter_${item.order}")
                    ) {
                        Text("$currentCount / ${item.count} (${item.countDescEn})")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranicDuasTab(
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
                        text = "Surah ${ayah.surahNumber}:${ayah.numberInSurah}",
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
