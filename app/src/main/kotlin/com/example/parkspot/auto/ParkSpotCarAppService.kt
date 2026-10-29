package com.example.parkspot.auto

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class ParkSpotCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Allows projection from Android Auto & Automotive OS hosts
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return ParkSpotCarSession()
    }
}
