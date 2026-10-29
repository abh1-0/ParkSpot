package com.example.parkspot.model

import org.json.JSONObject
import java.util.UUID

data class ParkingRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val address: String = "",
    val photoUri: String? = null,
    val floorLevel: String? = null,
    val spotNumber: String? = null,
    val notes: String? = null,
    val timerExpiryMillis: Long? = null,
    val isActive: Boolean = true
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("timestamp", timestamp)
        json.put("latitude", latitude)
        json.put("longitude", longitude)
        json.put("accuracyMeters", accuracyMeters.toDouble())
        json.put("address", address)
        json.put("photoUri", photoUri ?: "")
        json.put("floorLevel", floorLevel ?: "")
        json.put("spotNumber", spotNumber ?: "")
        json.put("notes", notes ?: "")
        json.put("timerExpiryMillis", timerExpiryMillis ?: -1L)
        json.put("isActive", isActive)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): ParkingRecord? {
            return try {
                val json = JSONObject(jsonStr)
                ParkingRecord(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    latitude = json.getDouble("latitude"),
                    longitude = json.getDouble("longitude"),
                    accuracyMeters = json.optDouble("accuracyMeters", 0.0).toFloat(),
                    address = json.optString("address", ""),
                    photoUri = json.optString("photoUri").takeIf { it.isNotBlank() },
                    floorLevel = json.optString("floorLevel").takeIf { it.isNotBlank() },
                    spotNumber = json.optString("spotNumber").takeIf { it.isNotBlank() },
                    notes = json.optString("notes").takeIf { it.isNotBlank() },
                    timerExpiryMillis = json.optLong("timerExpiryMillis", -1L).takeIf { it > 0 },
                    isActive = json.optBoolean("isActive", true)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
