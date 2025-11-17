package com.rescuemate.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import com.rescuemate.emergency.data.HealthData
import java.util.UUID

/**
 * BLE Health Service
 * Handles Bluetooth Low Energy communication with health devices (smartwatches)
 * Supports standard BLE health profiles
 */
class BLEHealthService(private val context: Context) {

    companion object {
        private const val TAG = "BLEHealthService"

        // Standard BLE Service UUIDs
        val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val BLOOD_PRESSURE_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
        val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")

        // Heart Rate Characteristic UUIDs
        val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_CONTROL_POINT_UUID = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb")

        // Blood Pressure Characteristic UUIDs
        val BLOOD_PRESSURE_MEASUREMENT_UUID = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb")
        val BLOOD_PRESSURE_FEATURE_UUID = UUID.fromString("00002a49-0000-1000-8000-00805f9b34fb")

        // Battery Characteristic UUID
        val BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        // Client Characteristic Configuration Descriptor UUID
        val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isScanning = false
    private var isConnected = false

    // Callbacks
    var onHeartRateUpdate: ((Int) -> Unit)? = null
    var onBloodPressureUpdate: ((Int, Int) -> Unit)? = null  // Systolic, Diastolic
    var onBatteryUpdate: ((Int) -> Unit)? = null
    var onConnectionStateChange: ((Boolean) -> Unit)? = null
    var onDeviceFound: ((BluetoothDevice) -> Unit)? = null

    /**
     * Start scanning for BLE health devices
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startScanning() {
        if (bluetoothLeScanner == null || isScanning) {
            Log.w(TAG, "Cannot start scanning: scanner=${bluetoothLeScanner != null}, isScanning=$isScanning")
            return
        }

        try {
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID))
                    .build()
            )

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bluetoothLeScanner.startScan(filters, settings, scanCallback)
            isScanning = true
            Log.d(TAG, "Started BLE scanning for health devices")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan", e)
        }
    }

    /**
     * Stop scanning for BLE devices
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScanning() {
        if (!isScanning) return

        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Stopped BLE scanning")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan", e)
        }
    }

    /**
     * Connect to a BLE device
     */
    fun connect(device: BluetoothDevice): Boolean {
        if (isConnected && connectedDevice?.address == device.address) {
            Log.d(TAG, "Already connected to ${device.address}")
            return true
        }

        try {
            disconnect() // Disconnect from previous device if any

            connectedDevice = device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            }
            
            Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            return false
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            connectedDevice = null
            isConnected = false
            onConnectionStateChange?.invoke(false)
            Log.d(TAG, "Disconnected from device")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    /**
     * Check if connected to a device
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Get connected device
     */
    fun getConnectedDevice(): BluetoothDevice? = connectedDevice

    /**
     * GATT Callback for BLE communication
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT connected")
                    isConnected = true
                    onConnectionStateChange?.invoke(true)
                    
                    // Discover services
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT disconnected")
                    isConnected = false
                    onConnectionStateChange?.invoke(false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                setupHealthNotifications(gatt)
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                HEART_RATE_MEASUREMENT_UUID -> {
                    val heartRate = parseHeartRate(characteristic.value)
                    if (heartRate > 0) {
                        Log.d(TAG, "Heart rate update: $heartRate BPM")
                        onHeartRateUpdate?.invoke(heartRate)
                    }
                }
                BLOOD_PRESSURE_MEASUREMENT_UUID -> {
                    val (systolic, diastolic) = parseBloodPressure(characteristic.value)
                    if (systolic > 0 && diastolic > 0) {
                        Log.d(TAG, "Blood pressure update: $systolic/$diastolic mmHg")
                        onBloodPressureUpdate?.invoke(systolic, diastolic)
                    }
                }
                BATTERY_LEVEL_UUID -> {
                    val batteryLevel = characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8, 0
                    ) ?: 0
                    Log.d(TAG, "Battery update: $batteryLevel%")
                    onBatteryUpdate?.invoke(batteryLevel)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic.uuid) {
                    BATTERY_LEVEL_UUID -> {
                        val batteryLevel = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0
                        ) ?: 0
                        onBatteryUpdate?.invoke(batteryLevel)
                    }
                }
            }
        }
    }

    /**
     * Setup notifications for health characteristics
     */
    private fun setupHealthNotifications(gatt: BluetoothGatt) {
        val services = gatt.services

        for (service in services) {
            when (service.uuid) {
                HEART_RATE_SERVICE_UUID -> {
                    enableHeartRateNotifications(gatt, service)
                }
                BLOOD_PRESSURE_SERVICE_UUID -> {
                    enableBloodPressureNotifications(gatt, service)
                }
                BATTERY_SERVICE_UUID -> {
                    readBatteryLevel(gatt, service)
                }
            }
        }
    }

    /**
     * Enable heart rate notifications
     */
    private fun enableHeartRateNotifications(gatt: BluetoothGatt, service: BluetoothGattService) {
        val characteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            ?: return

        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            ?: return

        gatt.setCharacteristicNotification(characteristic, true)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
        Log.d(TAG, "Enabled heart rate notifications")
    }

    /**
     * Enable blood pressure notifications
     */
    private fun enableBloodPressureNotifications(gatt: BluetoothGatt, service: BluetoothGattService) {
        val characteristic = service.getCharacteristic(BLOOD_PRESSURE_MEASUREMENT_UUID)
            ?: return

        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            ?: return

        gatt.setCharacteristicNotification(characteristic, true)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
        Log.d(TAG, "Enabled blood pressure notifications")
    }

    /**
     * Read battery level
     */
    private fun readBatteryLevel(gatt: BluetoothGatt, service: BluetoothGattService) {
        val characteristic = service.getCharacteristic(BATTERY_LEVEL_UUID)
            ?: return

        gatt.readCharacteristic(characteristic)
        Log.d(TAG, "Reading battery level")
    }

    /**
     * Parse heart rate from characteristic value
     */
    private fun parseHeartRate(value: ByteArray): Int {
        if (value.isEmpty()) return 0

        // Heart Rate Measurement format (BLE spec):
        // Byte 0: Flags (bit 0 = heart rate format: 0 = uint8, 1 = uint16)
        val flags = value[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0

        return if (is16Bit && value.size >= 3) {
            // 16-bit heart rate
            ((value[1].toInt() and 0xFF) or ((value[2].toInt() and 0xFF) shl 8))
        } else if (value.size >= 2) {
            // 8-bit heart rate
            value[1].toInt() and 0xFF
        } else {
            0
        }
    }

    /**
     * Parse blood pressure from characteristic value
     */
    private fun parseBloodPressure(value: ByteArray): Pair<Int, Int> {
        if (value.size < 7) return Pair(0, 0)

        // Blood Pressure Measurement format (BLE spec):
        // Bytes 0-1: Flags
        // Bytes 2-3: Systolic (mmHg, uint16)
        // Bytes 4-5: Diastolic (mmHg, uint16)
        val systolic = ((value[2].toInt() and 0xFF) or ((value[3].toInt() and 0xFF) shl 8))
        val diastolic = ((value[4].toInt() and 0xFF) or ((value[5].toInt() and 0xFF) shl 8))

        return Pair(systolic, diastolic)
    }

    /**
     * Scan callback for BLE device discovery
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.d(TAG, "Found device: ${device.name} (${device.address})")
            onDeviceFound?.invoke(device)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                val device = result.device
                Log.d(TAG, "Found device (batch): ${device.name} (${device.address})")
                onDeviceFound?.invoke(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed: $errorCode")
            isScanning = false
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopScanning()
        disconnect()
    }
}

