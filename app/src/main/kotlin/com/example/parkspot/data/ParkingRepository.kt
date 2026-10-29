package com.example.parkspot.data

import android.content.Context
import android.content.SharedPreferences
import com.example.parkspot.model.ParkingRecord
import com.example.parkspot.widget.ParkingWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class ParkingRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activeParking = MutableStateFlow<ParkingRecord?>(loadActiveParking())
    val activeParking: StateFlow<ParkingRecord?> = _activeParking.asStateFlow()

    private val _history = MutableStateFlow<List<ParkingRecord>>(loadHistory())
    val history: StateFlow<List<ParkingRecord>> = _history.asStateFlow()

    fun getActiveParking(): ParkingRecord? = _activeParking.value

    fun saveParking(record: ParkingRecord) {
        val updatedRecord = record.copy(isActive = true)
        prefs.edit()
            .putString(KEY_ACTIVE_PARKING, updatedRecord.toJson())
            .apply()

        _activeParking.value = updatedRecord

        // Also add to history (avoid duplicates by id)
        val currentHistory = _history.value.toMutableList()
        currentHistory.removeAll { it.id == updatedRecord.id }
        currentHistory.add(0, updatedRecord)
        if (currentHistory.size > MAX_HISTORY) {
            currentHistory.subList(MAX_HISTORY, currentHistory.size).clear()
        }
        saveHistory(currentHistory)
        _history.value = currentHistory

        // Update home screen widgets immediately
        ParkingWidgetProvider.updateAllWidgets(context)
    }

    fun updateActiveParking(record: ParkingRecord) {
        saveParking(record)
    }

    fun clearActiveParking() {
        val current = _activeParking.value
        prefs.edit().remove(KEY_ACTIVE_PARKING).apply()
        _activeParking.value = null

        if (current != null) {
            // Update history entry as inactive
            val currentHistory = _history.value.toMutableList()
            val index = currentHistory.indexOfFirst { it.id == current.id }
            if (index != -1) {
                currentHistory[index] = current.copy(isActive = false)
                saveHistory(currentHistory)
                _history.value = currentHistory
            }
        }

        // Update home screen widgets immediately
        ParkingWidgetProvider.updateAllWidgets(context)
    }

    fun deleteHistoryItem(id: String) {
        val currentHistory = _history.value.toMutableList()
        currentHistory.removeAll { it.id == id }
        saveHistory(currentHistory)
        _history.value = currentHistory
    }

    private fun loadActiveParking(): ParkingRecord? {
        val json = prefs.getString(KEY_ACTIVE_PARKING, null) ?: return null
        return ParkingRecord.fromJson(json)
    }

    private fun loadHistory(): List<ParkingRecord> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ParkingRecord>()
            for (i in 0 until arr.length()) {
                val item = ParkingRecord.fromJson(arr.getString(i))
                if (item != null) list.add(item)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<ParkingRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "parkspot_preferences"
        private const val KEY_ACTIVE_PARKING = "key_active_parking"
        private const val KEY_HISTORY = "key_parking_history"
        private const val MAX_HISTORY = 25

        @Volatile
        private var instance: ParkingRepository? = null

        fun getInstance(context: Context): ParkingRepository {
            return instance ?: synchronized(this) {
                instance ?: ParkingRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
