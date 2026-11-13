package com.nuur.muslim.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.domain.AsrMadhhab
import com.nuur.muslim.domain.CalculationMethod
import com.nuur.muslim.domain.CuratedCity
import com.nuur.muslim.domain.PrayerTimesCalculator
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.notifications.AdhanPlaybackService
import com.nuur.muslim.notifications.PrayerReminderScheduler
import com.nuur.muslim.ui.viewmodel.LocationUiStatus
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
internal fun PrayerTimesTab(
    settings: UserSettings,
    strings: AppStrings,
    locationFeedback: String?,
    onSelectCity: (CuratedCity) -> Unit,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectTimezone: (String) -> Unit,
    onRequestDeviceLocation: () -> Unit,
    onLocationDenied: (String) -> Unit,
    onSelectAsrMadhhab: (AsrMadhhab) -> Unit = {},
    onTogglePrayerNotifications: (Boolean) -> Unit = {},
    isAdhanPlaying: Boolean = false,
    adhanFeedback: String? = null,
    onStopAdhan: () -> Unit = {},
    locationStatus: LocationUiStatus = LocationUiStatus(),
    onSearchWorldCities: suspend (String) -> List<CuratedCity> = { emptyList() }
) {
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onTogglePrayerNotifications(true)
        }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val times = remember(
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

    val nextPrayerName = when (countdown.prayerIndex) {
        0 -> strings.fajrLabel.substringBefore(" (")
        1 -> strings.sunriseLabel.substringBefore(" (")
        2 -> strings.dhuhrLabel.substringBefore(" (")
        3 -> strings.asrLabel.substringBefore(" (")
        4 -> strings.maghribLabel.substringBefore(" (")
        else -> strings.ishaLabel.substringBefore(" (")
    }

    var showCityPickerSheet by remember { mutableStateOf(false) }
    val activeLocationName = settings.activeLocationDisplayName()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        // Next Prayer Live Countdown Card with 1-tap Location Pill
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.nextPrayerLabel}: $nextPrayerName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = countdown.timeHHmm,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.nextPrayerIn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = countdown.formattedCountdown,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showCityPickerSheet = true }
                            .testTag("prayer_active_location_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = strings.selectCity,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = activeLocationName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            val rows = listOf(
                strings.fajrLabel to times.fajr,
                strings.sunriseLabel to times.sunrise,
                strings.dhuhrLabel to times.dhuhr,
                strings.asrLabel to times.asr,
                strings.maghribLabel to times.maghrib,
                strings.ishaLabel to times.isha
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

        // Local Consent-Based Notifications & Adhan Audio Card
        item {
            val hasBundledAdhan = remember { AdhanPlaybackService.hasBundledAdhan(context) }
            val adhanSourceLabel = if (hasBundledAdhan) {
                strings.adhanAudioBundled
            } else {
                strings.adhanAudioDefault
            }
            val canExactAlarms = PrayerReminderScheduler.canScheduleExactAlarms(context)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.prayerRemindersLabel, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = strings.prayerRemindersDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.prayerNotificationsEnabled,
                            onCheckedChange = { enable ->
                                if (!enable) {
                                    onTogglePrayerNotifications(false)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onTogglePrayerNotifications(true)
                                    } else {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    onTogglePrayerNotifications(true)
                                }
                            },
                            modifier = Modifier.testTag("prayer_notifications_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adhanAudioLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = adhanSourceLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isAdhanPlaying) {
                        FilledTonalButton(
                            onClick = onStopAdhan,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stop_adhan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.adhanStopPlay,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    adhanFeedback?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (settings.prayerNotificationsEnabled &&
                        !canExactAlarms &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = strings.exactAlarmHint,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Text(strings.exactAlarmButton)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upgraded Location, Method, Asr Madhhab, Timezone Card
        item {
            LocationAndCalculationCard(
                settings = settings,
                strings = strings,
                locationFeedback = locationFeedback,
                locationStatus = locationStatus,
                onSelectCity = onSelectCity,
                onSelectMethod = onSelectMethod,
                onSelectAsrMadhhab = onSelectAsrMadhhab,
                onSelectTimezone = onSelectTimezone,
                onRequestDeviceLocation = onRequestDeviceLocation,
                onLocationDenied = onLocationDenied,
                showCityPickerSheet = false,
                onToggleCityPickerSheet = { open -> showCityPickerSheet = open },
                onSearchWorldCities = onSearchWorldCities
            )
        }
    }

    if (showCityPickerSheet) {
        SearchableCityPickerSheet(
            selectedCityId = if (settings.useCustomGpsCoords || !settings.customLocationLabel.isNullOrBlank()) null else settings.selectedCityId,
            strings = strings,
            onSelectCity = { city ->
                onSelectCity(city)
                showCityPickerSheet = false
            },
            onSearchWorldCities = onSearchWorldCities,
            onDismiss = { showCityPickerSheet = false }
        )
    }
}

@Composable
internal fun QiblaTab(
    settings: UserSettings,
    strings: AppStrings,
    deviceAzimuth: Float?,
    hasCompassSensor: Boolean,
    compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
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
    val isLowAccuracy = hasCompassSensor &&
        (compassAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
            compassAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.qiblaTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = primaryColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${strings.qiblaBearingLabel}: ${"%.1f".format(qiblaBearing)}°",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = settings.activeLocationDisplayName(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.size(196.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 10f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = surfaceColor, radius = radius, center = center)
                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 6f)
                )
                rotate(dialRotation, pivot = center) {
                    drawLine(
                        color = onSurface.copy(alpha = 0.6f),
                        start = Offset(center.x, center.y - radius + 8f),
                        end = Offset(center.x, center.y - radius + 24f),
                        strokeWidth = 5f
                    )
                    rotate(qiblaBearing.toFloat(), pivot = center) {
                        val arrow = Path().apply {
                            moveTo(center.x, center.y - radius + 24f)
                            lineTo(center.x - 14f, center.y + 16f)
                            lineTo(center.x + 14f, center.y + 16f)
                            close()
                        }
                        drawPath(arrow, color = primaryColor)
                    }
                }
                drawCircle(color = primaryColor, radius = 10f, center = center)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (isAligned) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = strings.qiblaAligned,
                    style = MaterialTheme.typography.titleMedium,
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        } else if (!hasCompassSensor || deviceAzimuth == null) {
            Text(
                text = strings.sensorUnavailableNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isLowAccuracy) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qibla_calibration_card")
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = if (isLowAccuracy) MaterialTheme.colorScheme.error else primaryColor
                )
                Column {
                    Text(
                        text = if (isLowAccuracy) strings.compassAccuracyLow else strings.compassAccuracyGood,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLowAccuracy) MaterialTheme.colorScheme.error else primaryColor
                    )
                    Text(
                        text = strings.compassCalibrationHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
