package com.nuur.muslim.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nuur.muslim.NuurQuranApplication
import com.nuur.muslim.audio.AudioPlaybackManager
import com.nuur.muslim.data.local.AdhkarItem
import com.nuur.muslim.data.local.SettingsRepository
import com.nuur.muslim.data.repository.AdhkarRepository
import com.nuur.muslim.domain.AsrMadhhab
import com.nuur.muslim.domain.CalculationMethod
import com.nuur.muslim.domain.CuratedCities
import com.nuur.muslim.domain.CuratedCity
import com.nuur.muslim.domain.WorldCitiesCatalog
import com.nuur.muslim.domain.WorldRegion
import com.nuur.muslim.localization.AppStrings
import com.nuur.muslim.notifications.AdhanPlaybackService
import com.nuur.muslim.notifications.PrayerReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

data class LocationUiStatus(
    val isLocating: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val isProviderDisabled: Boolean = false
)

class ToolsViewModel(
    private val appContext: Context,
    private val adhkarRepository: AdhkarRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel(), SensorEventListener {

    private val _adhkarList = MutableStateFlow<List<AdhkarItem>>(emptyList())
    val adhkarList: StateFlow<List<AdhkarItem>> = _adhkarList.asStateFlow()

    private val _adhkarCounters = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val adhkarCounters: StateFlow<Map<Int, Int>> = _adhkarCounters.asStateFlow()

    private val _deviceAzimuth = MutableStateFlow<Float?>(null)
    val deviceAzimuth: StateFlow<Float?> = _deviceAzimuth.asStateFlow()

    private val _hasCompassSensor = MutableStateFlow(false)
    val hasCompassSensor: StateFlow<Boolean> = _hasCompassSensor.asStateFlow()

    private val _compassAccuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
    val compassAccuracy: StateFlow<Int> = _compassAccuracy.asStateFlow()

    private val _locationFeedback = MutableStateFlow<String?>(null)
    val locationFeedback: StateFlow<String?> = _locationFeedback.asStateFlow()

    private val _locationStatus = MutableStateFlow(LocationUiStatus())
    val locationStatus: StateFlow<LocationUiStatus> = _locationStatus.asStateFlow()

    val isAdhanPlaying: StateFlow<Boolean> = AdhanPlaybackService.isPlaying

    private val _adhanFeedback = MutableStateFlow<String?>(null)
    val adhanFeedback: StateFlow<String?> = _adhanFeedback.asStateFlow()

    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val lastAccel = FloatArray(3)
    private val lastMagnet = FloatArray(3)
    private var hasAccel = false
    private var hasMagnet = false

    init {
        viewModelScope.launch {
            runCatching { adhkarRepository.loadAdhkar() }
                .onSuccess { items -> _adhkarList.value = items }
        }
        _hasCompassSensor.value = rotationSensor != null || (accelSensor != null && magnetSensor != null)
    }

    fun startCompass() {
        val sm = sensorManager ?: return
        if (rotationSensor != null) {
            sm.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelSensor != null && magnetSensor != null) {
            sm.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopCompass() {
        sensorManager?.unregisterListener(this)
    }

    override fun onCleared() {
        stopCompass()
        super.onCleared()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotMat = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                SensorManager.getOrientation(rotMat, orientation)
                val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                updateSmoothedAzimuth(deg)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccel, 0, 3)
                hasAccel = true
                updateFromAccelMagnet()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnet, 0, 3)
                hasMagnet = true
                updateFromAccelMagnet()
            }
        }
    }

    private fun updateFromAccelMagnet() {
        if (!hasAccel || !hasMagnet) return
        val rMat = FloatArray(9)
        val iMat = FloatArray(9)
        if (SensorManager.getRotationMatrix(rMat, iMat, lastAccel, lastMagnet)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rMat, orientation)
            val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            updateSmoothedAzimuth(deg)
        }
    }

    private fun updateSmoothedAzimuth(rawDeg: Float) {
        val prev = _deviceAzimuth.value
        if (prev == null) {
            _deviceAzimuth.value = rawDeg
            return
        }
        val diff = ((rawDeg - prev + 540f) % 360f) - 180f
        _deviceAzimuth.value = ((prev + 0.25f * diff) + 360f) % 360f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            _compassAccuracy.value = accuracy
        }
    }

    fun incrementDhikrCounter(order: Int, maxCount: Int) {
        val current = _adhkarCounters.value[order] ?: 0
        val next = if (current >= maxCount) 0 else current + 1
        _adhkarCounters.update { map -> map + (order to next) }
        if (maxCount > 0 && current + 1 == maxCount) {
            viewModelScope.launch { settingsRepository.markAdhkarCompleted(order) }
        }
    }

    fun toggleFavoriteAdhkar(order: Int) {
        viewModelScope.launch { settingsRepository.toggleFavoriteAdhkar(order) }
    }

    fun incrementTasbih() {
        viewModelScope.launch { settingsRepository.incrementTasbih() }
    }

    fun resetTasbih() {
        viewModelScope.launch { settingsRepository.resetTasbih() }
    }

    fun setTasbihTarget(target: Int) {
        viewModelScope.launch { settingsRepository.setTasbihTarget(target) }
    }

    fun setTasbihLabel(label: String) {
        viewModelScope.launch { settingsRepository.setTasbihLabel(label) }
    }

    fun setTasbihHaptic(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTasbihHapticEnabled(enabled) }
    }

    fun selectManualCity(city: CuratedCity, strings: AppStrings? = null) {
        _locationFeedback.value = null
        val display = if (city.id.startsWith("world_") && city.country.isNotBlank()) {
            "${city.name}, ${city.country}"
        } else {
            city.name
        }
        _locationStatus.value = LocationUiStatus(
            isLocating = false,
            message = strings?.let { "${it.locationUpdatedMsg}: $display" },
            isError = false,
            isProviderDisabled = false
        )
        viewModelScope.launch {
            if (city.id.startsWith("world_")) {
                settingsRepository.setManualWorldPlace(
                    placeLabel = display,
                    latitude = city.latitude,
                    longitude = city.longitude,
                    timezoneId = city.timezoneId
                )
            } else {
                settingsRepository.setManualCity(city.id, city.latitude, city.longitude, city.timezoneId)
            }
            syncRemindersIfNeeded()
        }
    }

    fun markLocationPermissionPrompted() {
        viewModelScope.launch {
            settingsRepository.markLocationPermissionPrompted()
        }
    }

    suspend fun searchWorldCitiesOnline(query: String): List<CuratedCity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 3 || !Geocoder.isPresent()) return@withContext emptyList()
        runCatching {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(3_500L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocationName(trimmed, 6, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(trimmed, 6)
            }
            addresses.orEmpty().mapNotNull { addr ->
                val cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
                    ?: return@mapNotNull null
                val countryName = addr.countryName ?: ""
                val countryCode = addr.countryCode ?: ""
                val tz = WorldCitiesCatalog.inferTimezoneId(countryCode, addr.latitude, addr.longitude)
                val idKey = "world_${(addr.latitude * 1000).toInt()}_${(addr.longitude * 1000).toInt()}"
                CuratedCity(
                    id = idKey,
                    name = cityName,
                    country = countryName,
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    timezoneId = tz,
                    region = WorldRegion.SOMALIA_HORN,
                    countryCode = countryCode
                )
            }.distinctBy { "${it.name.lowercase()}|${it.country.lowercase()}" }
        }.getOrDefault(emptyList())
    }

    fun setTimezone(tzId: String) {
        viewModelScope.launch {
            settingsRepository.setTimezone(tzId)
            syncRemindersIfNeeded()
        }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            settingsRepository.setCalculationMethod(method)
            syncRemindersIfNeeded()
        }
    }

    fun setAsrMadhhab(madhhab: AsrMadhhab) {
        viewModelScope.launch {
            settingsRepository.setAsrMadhhab(madhhab)
            syncRemindersIfNeeded()
        }
    }

    fun setPrayerNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationsEnabled(enabled)
            if (!enabled) {
                AdhanPlaybackService.stopAdhan(appContext)
            }
            val updated = settingsRepository.settingsFlow.first()
            PrayerReminderScheduler.syncSchedule(appContext, updated)
        }
    }

    fun importCustomAdhan(uri: Uri, strings: AppStrings) {
        viewModelScope.launch {
            val importedName = withContext(Dispatchers.IO) {
                runCatching {
                    val fileName = resolveDisplayName(uri) ?: "adhan_custom.mp3"
                    val dest = AdhanPlaybackService.customAdhanFile(appContext)
                    val stream = appContext.contentResolver.openInputStream(uri)
                        ?: error("Cannot open selected audio file")
                    AudioPlaybackManager.writeStreamAtomically(stream, dest)
                    fileName
                }
            }
            importedName.onSuccess { name ->
                settingsRepository.setCustomAdhanFileName(name)
                _adhanFeedback.value = "${strings.adhanSavedMsg}: $name"
            }.onFailure {
                _adhanFeedback.value = strings.adhanLoadErrorMsg
            }
        }
    }

    fun clearCustomAdhan(strings: AppStrings) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { AdhanPlaybackService.customAdhanFile(appContext).delete() }
            }
            settingsRepository.setCustomAdhanFileName(null)
            _adhanFeedback.value = strings.adhanRemovedMsg
        }
    }

    fun stopAdhanPlayback() {
        AdhanPlaybackService.stopAdhan(appContext)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        runCatching {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private suspend fun syncRemindersIfNeeded() {
        val current = settingsRepository.settingsFlow.first()
        PrayerReminderScheduler.syncSchedule(appContext, current)
    }

    private var locationCancelSignal: CancellationSignal? = null

    @SuppressLint("MissingPermission")
    fun applyDeviceCoarseLocation(strings: AppStrings? = null, silent: Boolean = false) {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            if (!silent) {
                val msg = strings?.locationFixFailedMsg ?: "Location service unavailable"
                _locationFeedback.value = msg
                _locationStatus.value = LocationUiStatus(isLocating = false, message = msg, isError = true)
            }
            return
        }

        val cached = listOfNotNull(
            runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull(),
            runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
            runCatching { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull()
        ).maxByOrNull { it.time }

        if (!LocationManagerCompat.isLocationEnabled(lm) && cached == null) {
            if (!silent) {
                val msg = strings?.locationProviderDisabledMsg
                    ?: "Device Location Services are turned off. Enable Location or pick a city."
                _locationFeedback.value = msg
                _locationStatus.value = LocationUiStatus(
                    isLocating = false,
                    message = msg,
                    isError = true,
                    isProviderDisabled = true
                )
            }
            return
        }

        locationCancelSignal?.cancel()
        val signal = CancellationSignal()
        locationCancelSignal = signal

        if (!silent) {
            _locationFeedback.value = null
            _locationStatus.value = LocationUiStatus(
                isLocating = true,
                message = strings?.locatingProgress,
                isError = false,
                isProviderDisabled = false
            )
        }

        viewModelScope.launch {
            if (cached != null) {
                saveResolvedLocation(cached.latitude, cached.longitude, strings, showSuccessToast = false)
            }

            val providersToTry = buildList {
                if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.NETWORK_PROVIDER)
                }
                if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.GPS_PROVIDER)
                }
            }

            var freshFix: Location? = null
            for (provider in providersToTry) {
                if (signal.isCanceled) break
                val attempt = withTimeoutOrNull(8_000L) {
                    requestSingleFix(lm, provider, signal)
                }
                if (attempt != null) {
                    freshFix = attempt
                    break
                }
            }

            val bestFix = freshFix ?: cached
            if (bestFix != null) {
                saveResolvedLocation(bestFix.latitude, bestFix.longitude, strings, showSuccessToast = !silent)
            } else if (!silent) {
                val isOff = !LocationManagerCompat.isLocationEnabled(lm)
                val msg = if (isOff) {
                    strings?.locationProviderDisabledMsg
                        ?: "Device Location Services are turned off. Enable Location or pick a city."
                } else {
                    strings?.locationFixFailedMsg
                        ?: "Could not acquire location right now. Please select a city or retry."
                }
                _locationFeedback.value = msg
                _locationStatus.value = LocationUiStatus(
                    isLocating = false,
                    message = msg,
                    isError = true,
                    isProviderDisabled = isOff
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(
        lm: LocationManager,
        provider: String,
        signal: CancellationSignal
    ): Location? = suspendCancellableCoroutine { cont ->
        runCatching {
            LocationManagerCompat.getCurrentLocation(
                lm,
                provider,
                signal,
                ContextCompat.getMainExecutor(appContext)
            ) { loc ->
                if (cont.isActive) cont.resume(loc)
            }
        }.onFailure {
            if (cont.isActive) cont.resume(null)
        }
    }

    private suspend fun saveResolvedLocation(
        lat: Double,
        lon: Double,
        strings: AppStrings?,
        showSuccessToast: Boolean
    ) {
        val resolvedName = resolvePlaceName(lat, lon)
        settingsRepository.setDeviceCoordinates(
            latitude = lat,
            longitude = lon,
            timezoneId = TimeZone.getDefault().id,
            locationLabel = resolvedName
        )
        syncRemindersIfNeeded()
        _locationFeedback.value = null
        if (showSuccessToast) {
            val baseMsg = strings?.locationUpdatedMsg ?: "Location updated"
            val labelSuffix = resolvedName ?: CuratedCities.defaultCity.name
            _locationStatus.value = LocationUiStatus(
                isLocating = false,
                message = "$baseMsg: $labelSuffix",
                isError = false,
                isProviderDisabled = false
            )
        }
    }

    private suspend fun resolvePlaceName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        val geocoded = runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            val geocoder = Geocoder(appContext, Locale.getDefault())
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(3_000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)
            }
            val first = addresses?.firstOrNull()
            val city = first?.locality ?: first?.subAdminArea ?: first?.adminArea
            val country = first?.countryName
            when {
                !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                !city.isNullOrBlank() -> city
                !country.isNullOrBlank() -> country
                else -> null
            }
        }.getOrNull()

        geocoded
            ?: CuratedCities.findNearestCity(lat, lon, maxDistanceKm = 75.0)?.name
            ?: CuratedCities.findNearestCity(lat, lon, maxDistanceKm = Double.MAX_VALUE)?.name
    }

    fun setLocationDeniedMessage(message: String) {
        _locationFeedback.value = message
        _locationStatus.value = LocationUiStatus(
            isLocating = false,
            message = message,
            isError = true,
            isProviderDisabled = false
        )
    }

    class Factory(private val app: NuurQuranApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ToolsViewModel(
                app.applicationContext,
                app.adhkarRepository,
                app.settingsRepository
            ) as T
        }
    }
}
