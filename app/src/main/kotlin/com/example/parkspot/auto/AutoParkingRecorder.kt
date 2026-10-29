package com.example.parkspot.auto

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.parkspot.MainActivity
import com.example.parkspot.R
import com.example.parkspot.data.ParkingRepository
import com.example.parkspot.location.LocationHelper
import com.example.parkspot.model.ParkingRecord
import com.example.parkspot.widget.ParkingWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AutoParkingRecorder {

    private const val CHANNEL_ID = "parkspot_auto_parking"
    private const val NOTIFICATION_ID = 4040

    fun recordCurrentParking(
        context: Context,
        sourceDescription: String = "Android Auto Disconnected",
        onCompleted: ((ParkingRecord?) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val location = LocationHelper.getCurrentLocation(context)
            if (location != null) {
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
                    photoUri = null,
                    notes = sourceDescription,
                    isActive = true
                )

                val repo = ParkingRepository.getInstance(context)
                repo.saveParking(record)

                // Show heads up notification
                showParkingSavedNotification(context, record)

                onCompleted?.invoke(record)
            } else {
                onCompleted?.invoke(null)
            }
        }
    }

    private fun showParkingSavedNotification(context: Context, record: ParkingRecord) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Parking Location Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when parking location is saved via Android Auto or widget"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap notification -> open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            501,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Navigate in Google Maps
        val navUri = Uri.parse("google.navigation:q=${record.latitude},${record.longitude}&mode=w")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val navPendingIntent = PendingIntent.getActivity(
            context,
            502,
            navIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Take photo
        val photoIntent = Intent(context, MainActivity::class.java).apply {
            action = ParkingWidgetProvider.ACTION_TAKE_PICTURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val photoPendingIntent = PendingIntent.getActivity(
            context,
            503,
            photoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle("Vehicle Parked Automatically")
            .setContentText(record.address.ifBlank { "Location saved successfully" })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Parking spot saved via ${record.notes ?: "Android Auto"}.\n" +
                            "Address: ${record.address}\n" +
                            "Coordinates: ${record.latitude}, ${record.longitude}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_navigation, "Navigate in Maps", navPendingIntent)
            .addAction(R.drawable.ic_camera, "Add Photo", photoPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
