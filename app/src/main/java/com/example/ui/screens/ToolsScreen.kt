package com.nuur.muslim.ui.screens

import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuur.muslim.data.local.AdhkarItem
import com.nuur.muslim.data.local.AyahEntity
import com.nuur.muslim.data.local.UserSettings
import com.nuur.muslim.domain.AsrMadhhab
import com.nuur.muslim.domain.CalculationMethod
import com.nuur.muslim.domain.CuratedCity
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.ui.viewmodel.LocationUiStatus

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
    onOpenReader: (Int, Int) -> Unit,
    compassAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM,
    onSelectAsrMadhhab: (AsrMadhhab) -> Unit = {},
    onTogglePrayerNotifications: (Boolean) -> Unit = {},
    isAdhanPlaying: Boolean = false,
    adhanFeedback: String? = null,
    onStopAdhan: () -> Unit = {},
    onToggleFavoriteAdhkar: (Int) -> Unit = {},
    onIncrementTasbih: () -> Unit = {},
    onResetTasbih: () -> Unit = {},
    onSelectTasbihTarget: (Int) -> Unit = {},
    onSelectTasbihLabel: (String) -> Unit = {},
    onToggleTasbihHaptic: (Boolean) -> Unit = {},
    locationStatus: LocationUiStatus = LocationUiStatus(),
    onSearchWorldCities: suspend (String) -> List<CuratedCity> = { emptyList() }
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val tabs = listOf(
        strings.openPrayerTimes,
        strings.openQibla,
        strings.openAdhkar,
        strings.openTasbih,
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
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp
            ) {
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
                    onLocationDenied = onLocationDenied,
                    onSelectAsrMadhhab = onSelectAsrMadhhab,
                    onTogglePrayerNotifications = onTogglePrayerNotifications,
                    isAdhanPlaying = isAdhanPlaying,
                    adhanFeedback = adhanFeedback,
                    onStopAdhan = onStopAdhan,
                    locationStatus = locationStatus,
                    onSearchWorldCities = onSearchWorldCities
                )
                1 -> QiblaTab(
                    settings = settings,
                    strings = strings,
                    deviceAzimuth = deviceAzimuth,
                    hasCompassSensor = hasCompassSensor,
                    compassAccuracy = compassAccuracy
                )
                2 -> AdhkarAndHijriTab(
                    settings = settings,
                    strings = strings,
                    adhkarList = adhkarList,
                    adhkarCounters = adhkarCounters,
                    onIncrementDhikr = onIncrementDhikr,
                    onToggleFavoriteAdhkar = onToggleFavoriteAdhkar
                )
                3 -> TasbihTab(
                    settings = settings,
                    strings = strings,
                    onIncrement = onIncrementTasbih,
                    onReset = onResetTasbih,
                    onSelectTarget = onSelectTasbihTarget,
                    onSelectLabel = onSelectTasbihLabel,
                    onToggleHaptic = onToggleTasbihHaptic
                )
                4 -> QuranicDuasTab(
                    strings = strings,
                    duas = quranicDuas,
                    onOpenReader = onOpenReader
                )
            }
        }
    }
}
