package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.NuurQuranApplication
import com.example.data.local.AdhkarItem
import com.example.data.local.SettingsRepository
import com.example.data.repository.AdhkarRepository
import com.example.domain.CalculationMethod
import com.example.domain.CuratedCity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                _deviceAzimuth.value = deg
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
            _deviceAzimuth.value = deg
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun incrementDhikrCounter(order: Int, maxCount: Int) {
        _adhkarCounters.update { map ->
            val current = map[order] ?: 0
            val next = if (current >= maxCount) 0 else current + 1
            map + (order to next)
        }
    }

    fun selectManualCity(city: CuratedCity) {
        _locationFeedback.value = null
        viewModelScope.launch {
            settingsRepository.setManualCity(city.id, city.latitude, city.longitude, city.timezoneId)
        }
    }

    fun setTimezone(tzId: String) {
        viewModelScope.launch { settingsRepository.setTimezone(tzId) }
    }

    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch { settingsRepository.setCalculationMethod(method) }
    }

    @SuppressLint("MissingPermission")
    fun applyDeviceCoarseLocation() {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            _locationFeedback.value = "Location service unavailable"
            return
        }
        val loc = runCatching {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull()

        if (loc != null) {
            _locationFeedback.value = null
            viewModelScope.launch {
                settingsRepository.setDeviceCoordinates(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    timezoneId = TimeZone.getDefault().id
                )
            }
        } else {
            _locationFeedback.value = "No cached device location yet; select a city manually or enable GPS."
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
