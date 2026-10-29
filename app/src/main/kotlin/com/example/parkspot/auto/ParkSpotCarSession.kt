package com.example.parkspot.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.parkspot.data.ParkingRepository

class ParkSpotCarSession : Session() {

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // Triggered when Android Auto disconnects (car engine turned off / unplugged)
                val repo = ParkingRepository.getInstance(carContext.applicationContext)
                val current = repo.getActiveParking()
                val isRecentlySaved = current != null && current.isActive &&
                        (System.currentTimeMillis() - current.timestamp < 120_000L)

                if (!isRecentlySaved) {
                    AutoParkingRecorder.recordCurrentParking(
                        carContext.applicationContext,
                        sourceDescription = "Android Auto Disconnected"
                    )
                }
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        return ParkingCarScreen(carContext)
    }
}
