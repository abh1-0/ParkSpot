package com.example.parkspot.auto

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.parkspot.data.ParkingRepository

class CarDisconnectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            // Check if device is car audio / handsfree
            val deviceClass = device?.bluetoothClass?.deviceClass
            val isCarAudio = deviceClass == 0x0408 || deviceClass == 0x0420 || // Handsfree / Car audio
                    device?.name?.contains("car", ignoreCase = true) == true ||
                    device?.name?.contains("auto", ignoreCase = true) == true ||
                    device?.name?.contains("media", ignoreCase = true) == true

            if (isCarAudio) {
                val repo = ParkingRepository.getInstance(context)
                val current = repo.getActiveParking()
                val isRecentlySaved = current != null && current.isActive &&
                        (System.currentTimeMillis() - current.timestamp < 120_000L)

                if (!isRecentlySaved) {
                    AutoParkingRecorder.recordCurrentParking(
                        context.applicationContext,
                        sourceDescription = "Car Bluetooth Disconnected"
                    )
                }
            }
        }
    }
}
