package com.example.parkspot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.parkspot.MainActivity
import com.example.parkspot.R
import com.example.parkspot.data.ParkingRepository
import com.example.parkspot.location.LocationHelper
import com.example.parkspot.model.ParkingRecord

class ParkingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repo = ParkingRepository.getInstance(context)
        val activeParking = repo.getActiveParking()

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, activeParking)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_CLEAR_PARKING -> {
                val repo = ParkingRepository.getInstance(context)
                repo.clearActiveParking()
                updateAllWidgets(context)
            }
            ACTION_WIDGET_NAVIGATE -> {
                val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val lon = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                if (lat != 0.0 || lon != 0.0) {
                    val navUri = Uri.parse("google.navigation:q=$lat,$lon&mode=w")
                    val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(navIntent)
                    } catch (_: Exception) {
                        val geoUri = Uri.parse("geo:0,0?q=$lat,$lon(Parked+Vehicle)")
                        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(geoIntent)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TAKE_PICTURE = "com.example.parkspot.ACTION_TAKE_PICTURE"
        const val ACTION_MARK_LOCATION = "com.example.parkspot.ACTION_MARK_LOCATION"
        const val ACTION_VIEW_PARKING = "com.example.parkspot.ACTION_VIEW_PARKING"
        const val ACTION_CLEAR_PARKING = "com.example.parkspot.ACTION_CLEAR_PARKING"
        const val ACTION_WIDGET_NAVIGATE = "com.example.parkspot.ACTION_WIDGET_NAVIGATE"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ParkingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val repo = ParkingRepository.getInstance(context)
            val activeParking = repo.getActiveParking()

            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, activeParking)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            parking: ParkingRecord?
        ) {
            val views: RemoteViews = if (parking != null && parking.isActive) {
                buildActiveViews(context, parking)
            } else {
                buildIdleViews(context)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun buildIdleViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_parking_idle)

            // Click "Take a picture" -> launch MainActivity with ACTION_TAKE_PICTURE
            val cameraIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_TAKE_PICTURE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val cameraPendingIntent = PendingIntent.getActivity(
                context,
                101,
                cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_take_picture, cameraPendingIntent)

            // Click "Mark location" -> launch MainActivity with ACTION_MARK_LOCATION
            val markIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_MARK_LOCATION
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val markPendingIntent = PendingIntent.getActivity(
                context,
                102,
                markIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_mark_location, markPendingIntent)

            // Click card background -> open main app
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                100,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.layout_text_content, mainPendingIntent)

            return views
        }

        private fun buildActiveViews(context: Context, parking: ParkingRecord): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_parking_active)

            // Format address and times
            val durationText = LocationHelper.formatDurationSince(parking.timestamp)
            views.setTextViewText(R.id.tv_active_time, "Parked $durationText")

            val displayAddress = parking.address.ifBlank {
                "Parked at ${LocationHelper.formatTime(parking.timestamp)}"
            }
            views.setTextViewText(R.id.tv_active_address, displayAddress)

            val notesText = buildString {
                if (!parking.floorLevel.isNullOrBlank()) append("Level: ${parking.floorLevel}  ")
                if (!parking.spotNumber.isNullOrBlank()) append("Bay: ${parking.spotNumber}  ")
                if (!parking.notes.isNullOrBlank()) append(parking.notes)
            }.trim()

            if (notesText.isNotBlank()) {
                views.setViewVisibility(R.id.tv_active_notes, View.VISIBLE)
                views.setTextViewText(R.id.tv_active_notes, notesText)
            } else {
                views.setViewVisibility(R.id.tv_active_notes, View.GONE)
            }

            val coordsText = String.format(
                java.util.Locale.US,
                "%.4f° N, %.4f° W",
                parking.latitude,
                parking.longitude
            )
            views.setTextViewText(R.id.tv_active_coords, coordsText)

            // Load photo thumbnail if available
            var photoLoaded = false
            if (!parking.photoUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(parking.photoUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // downsample for widget memory safety
                        }
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream.close()
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.iv_widget_photo, bitmap)
                            photoLoaded = true
                        }
                    }
                } catch (_: Exception) {}
            }
            if (!photoLoaded) {
                views.setImageViewResource(R.id.iv_widget_photo, R.drawable.ic_car)
            }

            // Button 1: Navigate directly in Google Maps
            val navIntent = Intent(context, ParkingWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NAVIGATE
                putExtra(EXTRA_LATITUDE, parking.latitude)
                putExtra(EXTRA_LONGITUDE, parking.longitude)
            }
            val navPendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                navIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_navigate, navPendingIntent)

            // Button 2: Details / Open App
            val detailsIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_VIEW_PARKING
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val detailsPendingIntent = PendingIntent.getActivity(
                context,
                202,
                detailsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_details, detailsPendingIntent)

            // Button 3: Clear / Found Car
            val clearIntent = Intent(context, ParkingWidgetProvider::class.java).apply {
                action = ACTION_CLEAR_PARKING
            }
            val clearPendingIntent = PendingIntent.getBroadcast(
                context,
                203,
                clearIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_clear, clearPendingIntent)

            // Clicking card background opens details
            views.setOnClickPendingIntent(R.id.layout_active_body, detailsPendingIntent)

            return views
        }
    }
}
