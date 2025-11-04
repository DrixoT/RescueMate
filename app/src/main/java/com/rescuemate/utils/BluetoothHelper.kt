package com.rescuemate.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val device: BluetoothDevice,
    val isPaired: Boolean = false
)

class BluetoothHelper(private val context: Context) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun hasBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBluetoothPermissionsLegacy(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        if (!isBluetoothEnabled()) return emptyList()

        val devices = mutableListOf<BluetoothDeviceInfo>()
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            devices.add(
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    device = device,
                    isPaired = true
                )
            )
        }
        return devices
    }

    fun filterSmartwatchDevices(devices: List<BluetoothDeviceInfo>): List<BluetoothDeviceInfo> {
        val smartwatchKeywords = listOf(
            "watch", "band", "wear", "fitbit", "galaxy watch", "apple watch",
            "mi band", "amazfit", "garmin", "fitbit", "fossil", "huawei"
        )
        return devices.filter { device ->
            smartwatchKeywords.any { keyword ->
                device.name.lowercase().contains(keyword)
            }
        }
    }
}

