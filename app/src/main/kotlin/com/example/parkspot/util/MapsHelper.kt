package com.example.parkspot.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapsHelper {

    fun getGoogleMapsUrl(latitude: Double, longitude: Double): String {
        return "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    }

    fun navigateToParking(context: Context, latitude: Double, longitude: Double, walkingMode: Boolean = true) {
        val mode = if (walkingMode) "w" else "d"
        val navUri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=$mode")
        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                return
            }
        } catch (_: Exception) {}

        // Fallback to standard geo URI
        val geoUri = Uri.parse("geo:0,0?q=$latitude,$longitude(Parked+Vehicle)")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            if (geoIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(geoIntent)
                return
            }
        } catch (_: Exception) {}

        // Final fallback: Browser / Web Maps
        try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=${if (walkingMode) "walking" else "driving"}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch map navigation", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareParkingLocation(context: Context, latitude: Double, longitude: Double, address: String?, notes: String?) {
        val mapsUrl = getGoogleMapsUrl(latitude, longitude)
        val sb = StringBuilder()
        sb.append("📍 My Parked Vehicle Location:\n")
        if (!address.isNullOrBlank()) {
            sb.append("Address: $address\n")
        }
        if (!notes.isNullOrBlank()) {
            sb.append("Spot/Notes: $notes\n")
        }
        sb.append("Coordinates: $latitude, $longitude\n")
        sb.append("Google Maps: $mapsUrl")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Parking Location").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(shareIntent)
    }

    fun formatCoordinates(latitude: Double, longitude: Double): String {
        val latDirection = if (latitude >= 0) "N" else "S"
        val lonDirection = if (longitude >= 0) "E" else "W"
        return String.format(
            java.util.Locale.US,
            "%.4f° %s, %.4f° %s",
            Math.abs(latitude),
            latDirection,
            Math.abs(longitude),
            lonDirection
        )
    }
}
