package com.example.parkspot.ui

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.parkspot.data.ParkingRepository
import com.example.parkspot.location.LocationHelper
import com.example.parkspot.model.ParkingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingRepository.getInstance(application)

    val activeParking: StateFlow<ParkingRecord?> = repository.activeParking
    val history: StateFlow<List<ParkingRecord>> = repository.history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _distanceToCar = MutableStateFlow<Float?>(null)
    val distanceToCar: StateFlow<Float?> = _distanceToCar.asStateFlow()

    private var pendingPhotoUri: Uri? = null

    init {
        startDistanceMonitoring()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun createPhotoUri(): Uri {
        val context: Context = getApplication()
        val photosDir = File(context.filesDir, "parking_photos").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(photosDir, "PARK_${timeStamp}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        pendingPhotoUri = uri
        return uri
    }

    fun onPhotoTaken(success: Boolean, floor: String? = null, spot: String? = null, notes: String? = null) {
        if (!success) {
            _statusMessage.value = "Photo capture cancelled"
            return
        }
        val photoUri = pendingPhotoUri ?: return
        saveParkingWithLocation(photoUri.toString(), floor, spot, notes)
    }

    fun markCurrentLocation(floor: String? = null, spot: String? = null, notes: String? = null) {
        saveParkingWithLocation(photoUri = null, floor = floor, spot = spot, notes = notes)
    }

    private fun saveParkingWithLocation(
        photoUri: String?,
        floor: String?,
        spot: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Acquiring GPS location..."

            val context: Context = getApplication()
            val location = LocationHelper.getCurrentLocation(context)

            if (location == null) {
                // If location could not be determined, but photo was taken
                if (photoUri != null) {
                    val record = ParkingRecord(
                        latitude = 0.0,
                        longitude = 0.0,
                        accuracyMeters = 0f,
                        address = "Indoor Parking (No GPS signal)",
                        photoUri = photoUri,
                        floorLevel = floor,
                        spotNumber = spot,
                        notes = notes,
                        isActive = true
                    )
                    repository.saveParking(record)
                    _statusMessage.value = "Indoor parking photo saved!"
                } else {
                    _statusMessage.value = "Unable to get GPS fix. Please ensure GPS is enabled."
                }
                _isLoading.value = false
                return@launch
            }

            _statusMessage.value = "Finding address..."
            val address = LocationHelper.getAddressFromLocation(
                context,
                location.latitude,
                location.longitude
            )

            val record = ParkingRecord(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                address = address,
                photoUri = photoUri,
                floorLevel = floor,
                spotNumber = spot,
                notes = notes,
                isActive = true
            )

            repository.saveParking(record)
            _isLoading.value = false
            _statusMessage.value = "Vehicle parking location saved!"
            updateDistance(location)
        }
    }

    fun updateNotes(floor: String?, spot: String?, notes: String?) {
        val current = activeParking.value ?: return
        val updated = current.copy(
            floorLevel = floor?.ifBlank { null },
            spotNumber = spot?.ifBlank { null },
            notes = notes?.ifBlank { null }
        )
        repository.updateActiveParking(updated)
        _statusMessage.value = "Parking notes updated"
    }

    fun setParkingTimer(minutes: Int) {
        val current = activeParking.value ?: return
        val expiry = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val updated = current.copy(timerExpiryMillis = expiry)
        repository.updateActiveParking(updated)
        _statusMessage.value = "Parking meter reminder set for $minutes minutes"
    }

    fun clearActiveParking() {
        repository.clearActiveParking()
        _distanceToCar.value = null
        _statusMessage.value = "Parking cleared. Have a safe drive!"
    }

    fun deleteHistory(id: String) {
        repository.deleteHistoryItem(id)
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val context: Context = getApplication()
            val loc = LocationHelper.getCurrentLocation(context)
            if (loc != null) {
                _currentLocation.value = loc
                updateDistance(loc)
            }
        }
    }

    private fun updateDistance(userLoc: Location) {
        val parking = activeParking.value ?: return
        if (parking.latitude != 0.0 || parking.longitude != 0.0) {
            val dist = LocationHelper.calculateDistanceMeters(
                userLoc.latitude,
                userLoc.longitude,
                parking.latitude,
                parking.longitude
            )
            _distanceToCar.value = dist
        }
    }

    private fun startDistanceMonitoring() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val parking = activeParking.value
                if (parking != null && (parking.latitude != 0.0 || parking.longitude != 0.0)) {
                    val context: Context = getApplication()
                    val loc = LocationHelper.getCurrentLocation(context)
                    if (loc != null) {
                        withContext(Dispatchers.Main) {
                            _currentLocation.value = loc
                            updateDistance(loc)
                        }
                    }
                }
                delay(15000) // update every 15 seconds
            }
        }
    }
}
