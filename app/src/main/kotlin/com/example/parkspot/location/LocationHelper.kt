package com.example.parkspot.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        // 1. Try Google Play Services FusedLocationProviderClient first
        try {
            val fusedClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            // If current location is null, try last known location
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                cont.resume(lastLoc)
                            }.addOnFailureListener {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
                cont.invokeOnCancellation { cts.cancel() }
            }

            if (location != null) return location
        } catch (_: Exception) {
            // Google Play Services unavailable, fallback to Android LocationManager
        }

        // 2. Fallback: Android Native LocationManager (works on any Android phone without Play Services)
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var bestLoc: Location? = null
            if (locationManager != null) {
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null && (bestLoc == null || loc.accuracy < bestLoc.accuracy)) {
                            bestLoc = loc
                        }
                    }
                }
            }
            bestLoc
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            cont.resume(formatAddress(addr, latitude, longitude))
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(fallbackAddress(latitude, longitude))
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                formatAddress(addr, latitude, longitude)
            }
        } catch (_: Exception) {
            fallbackAddress(latitude, longitude)
        }
    }

    private fun formatAddress(address: Address?, lat: Double, lon: Double): String {
        if (address == null) return fallbackAddress(lat, lon)
        val street = address.thoroughfare ?: address.subThoroughfare
        val feature = address.featureName
        val locality = address.locality ?: address.subLocality
        val admin = address.adminArea

        val parts = mutableListOf<String>()
        if (!street.isNullOrBlank()) {
            if (!address.subThoroughfare.isNullOrBlank() && street != address.subThoroughfare) {
                parts.add("${address.subThoroughfare} $street")
            } else {
                parts.add(street)
            }
        } else if (!feature.isNullOrBlank() && feature != locality) {
            parts.add(feature)
        }

        if (!locality.isNullOrBlank()) {
            parts.add(locality)
        } else if (!admin.isNullOrBlank()) {
            parts.add(admin)
        }

        return if (parts.isNotEmpty()) parts.joinToString(", ") else fallbackAddress(lat, lon)
    }

    private fun fallbackAddress(lat: Double, lon: Double): String {
        return "Spot (${String.format(Locale.US, "%.4f, %.4f", lat, lon)})"
    }

    fun calculateDistanceMeters(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(fromLat, fromLon, toLat, toLon, results)
        return results[0]
    }

    fun formatDistance(meters: Float): String {
        return when {
            meters < 1000 -> "${meters.toInt()} m"
            else -> String.format(Locale.US, "%.1f km", meters / 1000f)
        }
    }

    fun formatDurationSince(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        if (diff < 0) return "Just now"
        val minutes = (diff / 60000).toInt()
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ${minutes % 60}m ago"
            else -> "${days}d ago"
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
