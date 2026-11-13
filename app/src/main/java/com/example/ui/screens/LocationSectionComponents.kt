package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.UserSettings
import com.example.domain.AsrMadhhab
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCities
import com.example.domain.CuratedCity
import com.example.domain.PrayerTimesCalculator
import com.example.domain.WorldRegion
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.viewmodel.LocationUiStatus
import kotlinx.coroutines.delay
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LocationAndCalculationCard(
    settings: UserSettings,
    strings: AppStrings,
    locationFeedback: String?,
    locationStatus: LocationUiStatus,
    onSelectCity: (CuratedCity) -> Unit,
    onSelectMethod: (CalculationMethod) -> Unit,
    onSelectAsrMadhhab: (AsrMadhhab) -> Unit,
    onSelectTimezone: (String) -> Unit,
    onRequestDeviceLocation: () -> Unit,
    onLocationDenied: (String) -> Unit,
    showCityPickerSheet: Boolean,
    onToggleCityPickerSheet: (Boolean) -> Unit,
    onSearchWorldCities: suspend (String) -> List<CuratedCity> = { emptyList() }
) {
    val context = LocalContext.current
    var showTimezoneSheet by remember { mutableStateOf(false) }
    var methodMenuOpen by remember { mutableStateOf(false) }
    var madhhabMenuOpen by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onRequestDeviceLocation()
        } else {
            onLocationDenied(strings.locationPermissionDenied)
        }
    }

    val activeName = settings.activeLocationDisplayName()
    val qiblaBearing = remember(settings.customLatitude, settings.customLongitude) {
        PrayerTimesCalculator.calculateQiblaBearing(settings.customLatitude, settings.customLongitude)
    }

    val statusMessage = locationStatus.message ?: locationFeedback
    val isErrorStatus = locationStatus.isError || (locationFeedback != null && !locationStatus.isLocating)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_location_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(7.dp).size(18.dp)
                    )
                }
                Text(
                    text = strings.locationSectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Active Location Summary Box (clean place name, Qibla bearing & timezone — no raw coordinates)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.currentLocationLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (settings.useCustomGpsCoords) strings.gpsLocationBadge else strings.manualCityBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = activeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${strings.qiblaBearingLabel}: ${"%.1f".format(qiblaBearing)}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = settings.timezoneId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Two Clean Primary Actions: World Cities Picker + Auto-Detect Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onToggleCityPickerSheet(true) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("open_city_picker_button")
                ) {
                    Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.selectCity, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            onRequestDeviceLocation()
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    enabled = !locationStatus.isLocating,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("use_device_location_button")
                ) {
                    if (locationStatus.isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.useDeviceLocation,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Status Banner (Loading / Success / Error + Open Location Settings)
            AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isErrorStatus) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isErrorStatus) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isErrorStatus) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isErrorStatus) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (locationStatus.isProviderDisabled) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.openLocationSettings)
                            }
                        }
                    }
                }
            }

            Text(
                text = strings.locationPurposeNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Calculation Method Dropdown
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
                    shape = RoundedCornerShape(14.dp),
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

            // Asr Madhhab Dropdown
            ExposedDropdownMenuBox(
                expanded = madhhabMenuOpen,
                onExpandedChange = { madhhabMenuOpen = it }
            ) {
                val currentMadhhabLabel = if (settings.asrMadhhab == AsrMadhhab.SHAFI) {
                    strings.asrShafiLabel
                } else {
                    strings.asrHanafiLabel
                }
                OutlinedTextField(
                    value = currentMadhhabLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.asrMadhhabLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = madhhabMenuOpen) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("asr_madhhab_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = madhhabMenuOpen,
                    onDismissRequest = { madhhabMenuOpen = false }
                ) {
                    listOf(
                        AsrMadhhab.SHAFI to strings.asrShafiLabel,
                        AsrMadhhab.HANAFI to strings.asrHanafiLabel
                    ).forEach { (madhhab, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSelectAsrMadhhab(madhhab)
                                madhhabMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Searchable Timezone Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showTimezoneSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = strings.timezoneLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = settings.timezoneId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = strings.expandDetails,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showCityPickerSheet) {
        SearchableCityPickerSheet(
            selectedCityId = if (settings.useCustomGpsCoords || !settings.customLocationLabel.isNullOrBlank()) null else settings.selectedCityId,
            strings = strings,
            onSelectCity = { city ->
                onSelectCity(city)
                onToggleCityPickerSheet(false)
            },
            onSearchWorldCities = onSearchWorldCities,
            onDismiss = { onToggleCityPickerSheet(false) }
        )
    }

    if (showTimezoneSheet) {
        SearchableTimezonePickerSheet(
            selectedTimezone = settings.timezoneId,
            strings = strings,
            onSelectTimezone = { tz ->
                onSelectTimezone(tz)
                showTimezoneSheet = false
            },
            onDismiss = { showTimezoneSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchableCityPickerSheet(
    selectedCityId: String?,
    strings: AppStrings,
    onSelectCity: (CuratedCity) -> Unit,
    onSearchWorldCities: suspend (String) -> List<CuratedCity> = { emptyList() },
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<WorldRegion?>(null) }
    var onlineResults by remember { mutableStateOf<List<CuratedCity>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 3) {
            onlineResults = emptyList()
            isSearchingOnline = false
            return@LaunchedEffect
        }
        delay(350L)
        isSearchingOnline = true
        onlineResults = onSearchWorldCities(trimmed)
        isSearchingOnline = false
    }

    val filteredCities = remember(query, selectedRegion, onlineResults) {
        val q = query.trim()
        val base = CuratedCities.cities.filter { city ->
            (selectedRegion == null || city.region == selectedRegion) &&
                (q.isEmpty() ||
                    city.name.contains(q, ignoreCase = true) ||
                    city.country.contains(q, ignoreCase = true) ||
                    city.timezoneId.contains(q, ignoreCase = true))
        }
        if (q.length < 3 || onlineResults.isEmpty()) {
            base
        } else {
            val existingKeys = base.mapTo(HashSet()) { "${it.name.lowercase()}|${it.country.lowercase()}" }
            val extra = onlineResults.filter {
                "${it.name.lowercase()}|${it.country.lowercase()}" !in existingKeys
            }
            base + extra
        }
    }

    val regionItems = remember(strings) {
        listOf<Pair<WorldRegion?, String>>(
            null to strings.allRegionsLabel,
            WorldRegion.SOMALIA_HORN to strings.regionSomaliaHorn,
            WorldRegion.AFRICA to strings.regionAfrica,
            WorldRegion.MIDDLE_EAST to strings.regionMiddleEast,
            WorldRegion.EUROPE to strings.regionEurope,
            WorldRegion.AMERICAS to strings.regionAmericas,
            WorldRegion.ASIA_OCEANIA to strings.regionAsiaOceania
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.selectCity,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.worldCitiesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${filteredCities.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input"),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = strings.close)
                        }
                    }
                },
                placeholder = { Text(strings.searchCityPlaceholder) },
                shape = RoundedCornerShape(14.dp)
            )

            // Region Filter Chips across all continents
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                regionItems.forEach { (region, label) ->
                    val selected = selectedRegion == region
                    FilterChip(
                        selected = selected,
                        onClick = { selectedRegion = region },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }

            if (isSearchingOnline) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (filteredCities.isEmpty() && !isSearchingOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = strings.noCitiesFound,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCities, key = { it.id }) { city ->
                        val isSelected = city.id == selectedCityId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectCity(city) }
                                .testTag("city_option_${city.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = if (city.country.isNotBlank()) {
                                            "${city.country} • ${city.timezoneId}"
                                        } else {
                                            city.timezoneId
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableTimezonePickerSheet(
    selectedTimezone: String,
    strings: AppStrings,
    onSelectTimezone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allTimezones = remember {
        val priority = listOf(
            TimeZone.getDefault().id,
            "Africa/Mogadishu", "Africa/Djibouti", "Africa/Nairobi", "Africa/Addis_Ababa",
            "Africa/Kampala", "Asia/Riyadh", "Asia/Dubai", "Asia/Qatar", "Africa/Cairo",
            "Europe/Istanbul", "Europe/London", "Europe/Stockholm", "Europe/Oslo",
            "Europe/Helsinki", "Europe/Amsterdam", "America/Chicago", "America/New_York",
            "America/Los_Angeles", "America/Toronto", "Australia/Melbourne", "Asia/Kuala_Lumpur", "UTC"
        )
        (priority + TimeZone.getAvailableIDs().sorted()).distinct()
    }
    val filtered = remember(query, allTimezones) {
        val q = query.trim()
        if (q.isEmpty()) allTimezones
        else allTimezones.filter { it.contains(q, ignoreCase = true) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.timezoneLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(strings.timezoneLabel) },
                shape = RoundedCornerShape(14.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it }) { tz ->
                    val isSelected = tz == selectedTimezone
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTimezone(tz) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tz,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
