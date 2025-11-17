package com.rescuemate.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.UUID

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

    /**
     * Get BLE scanner for scanning health devices
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getBLEScanner(): BluetoothLeScanner? {
        return bluetoothAdapter?.bluetoothLeScanner
    }

    /**
     * Check if BLE is supported
     */
    fun isBLESupported(): Boolean {
        return bluetoothAdapter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    /**
     * Create scan filter for health devices (Heart Rate Service)
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createHealthDeviceScanFilter(): ScanFilter {
        val heartRateServiceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        return ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(heartRateServiceUuid))
            .build()
    }
}

