package com.example.parkspot.auto

import android.content.Intent
import android.net.Uri
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.example.parkspot.R
import com.example.parkspot.data.ParkingRepository
import com.example.parkspot.location.LocationHelper
import com.example.parkspot.model.ParkingRecord

class ParkingCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = ParkingRepository.getInstance(carContext)
    private var isSaving = false

    override fun onGetTemplate(): Template {
        val activeParking = repository.getActiveParking()

        return if (activeParking != null && activeParking.isActive) {
            buildActiveParkingTemplate(activeParking)
        } else {
            buildIdleTemplate()
        }
    }

    private fun buildIdleTemplate(): Template {
        val paneBuilder = Pane.Builder()

        val statusRow = Row.Builder()
            .setTitle("Save Vehicle Location")
            .addText(
                if (isSaving) {
                    "Acquiring high-accuracy GPS coordinates from vehicle..."
                } else {
                    "ParkSpot automatically records your location when Android Auto disconnects, or you can save it right now."
                }
            )
            .setImage(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_mark_location)
                ).build()
            )
            .build()

        paneBuilder.addRow(statusRow)

        if (!isSaving) {
            val saveAction = Action.Builder()
                .setTitle("Save Parking Spot")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener {
                    isSaving = true
                    invalidate()

                    AutoParkingRecorder.recordCurrentParking(
                        carContext,
                        sourceDescription = "Android Auto Car Display"
                    ) { record ->
                        isSaving = false
                        carContext.getCarService(AppManager::class.java)
                            .showToast(
                                if (record != null) "Parking spot recorded!" else "GPS signal unavailable",
                                1
                            )
                        invalidate()
                    }
                }
                .build()

            paneBuilder.addAction(saveAction)
        }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("ParkSpot")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }

    private fun buildActiveParkingTemplate(parking: ParkingRecord): Template {
        val paneBuilder = Pane.Builder()

        val addressRow = Row.Builder()
            .setTitle(parking.address.ifBlank { "Vehicle Parked" })
            .addText("Parked ${LocationHelper.formatDurationSince(parking.timestamp)}")
            .addText(
                String.format(
                    java.util.Locale.US,
                    "%.4f° N, %.4f° W",
                    parking.latitude,
                    parking.longitude
                )
            )
            .setImage(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_car)
                ).build()
            )
            .build()

        paneBuilder.addRow(addressRow)

        // Action: Navigate in Google Maps
        val navAction = Action.Builder()
            .setTitle("Navigate (Google Maps)")
            .setBackgroundColor(CarColor.BLUE)
            .setOnClickListener {
                val navUri = Uri.parse("google.navigation:q=${parking.latitude},${parking.longitude}&mode=w")
                val navIntent = Intent(CarContext.ACTION_NAVIGATE, navUri)
                try {
                    carContext.startCarApp(navIntent)
                } catch (_: Exception) {
                    val viewIntent = Intent(Intent.ACTION_VIEW, navUri)
                    try {
                        carContext.startCarApp(viewIntent)
                    } catch (_: Exception) {}
                }
            }
            .build()

        // Action: Update location to current car spot
        val updateAction = Action.Builder()
            .setTitle("Update Location")
            .setOnClickListener {
                isSaving = true
                invalidate()
                AutoParkingRecorder.recordCurrentParking(
                    carContext,
                    sourceDescription = "Updated from Car Display"
                ) {
                    isSaving = false
                    invalidate()
                }
            }
            .build()

        // Action: Clear / Found Car
        val clearAction = Action.Builder()
            .setTitle("Found Car (Clear)")
            .setOnClickListener {
                repository.clearActiveParking()
                invalidate()
            }
            .build()

        paneBuilder.addAction(navAction)
        paneBuilder.addAction(updateAction)
        paneBuilder.addAction(clearAction)

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("Parked Vehicle")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
