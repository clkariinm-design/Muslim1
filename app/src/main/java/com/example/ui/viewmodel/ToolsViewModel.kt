package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NuurQuranApplication
import com.example.data.local.AdhkarItem
import com.example.data.local.SettingsRepository
import com.example.data.repository.AdhkarRepository
import com.example.domain.AsrMadhhab
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCity
import com.example.notifications.PrayerReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone

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

    fun selectManualCity(city: CuratedCity) {
        _locationFeedback.value = null
        viewModelScope.launch {
            settingsRepository.setManualCity(city.id, city.latitude, city.longitude, city.timezoneId)
            syncRemindersIfNeeded()
        }
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
            val updated = settingsRepository.settingsFlow.first()
            PrayerReminderScheduler.syncSchedule(appContext, updated)
        }
    }

    private suspend fun syncRemindersIfNeeded() {
        val current = settingsRepository.settingsFlow.first()
        PrayerReminderScheduler.syncSchedule(appContext, current)
    }

    private var locationCancelSignal: CancellationSignal? = null

    @SuppressLint("MissingPermission")
    fun applyDeviceCoarseLocation() {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            _locationFeedback.value = "Location service unavailable"
            return
        }
        val cached = runCatching {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()

        if (cached != null) {
            saveLocation(cached.latitude, cached.longitude)
        }

        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }

        if (provider != null) {
            locationCancelSignal?.cancel()
            val signal = CancellationSignal()
            locationCancelSignal = signal
            runCatching {
                LocationManagerCompat.getCurrentLocation(
                    lm,
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(appContext)
                ) { fresh ->
                    if (fresh != null) {
                        saveLocation(fresh.latitude, fresh.longitude)
                    } else if (cached == null) {
                        _locationFeedback.value = "Unable to acquire GPS/network fix; please select a city manually."
                    }
                }
            }.onFailure {
                if (cached == null) {
                    _locationFeedback.value = "No cached device location; select a city manually."
                }
            }
        } else if (cached == null) {
            _locationFeedback.value = "Location provider disabled; please select a city manually."
        }
    }

    private fun saveLocation(lat: Double, lon: Double) {
        _locationFeedback.value = null
        viewModelScope.launch {
            settingsRepository.setDeviceCoordinates(
                latitude = lat,
                longitude = lon,
                timezoneId = TimeZone.getDefault().id
            )
            syncRemindersIfNeeded()
        }
    }

    fun setLocationDeniedMessage(message: String) {
        _locationFeedback.value = message
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
