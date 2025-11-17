package com.rescuemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import android.bluetooth.BluetoothDevice
import android.os.Build
import com.rescuemate.R
import com.rescuemate.bluetooth.SmartwatchManager
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.ui.theme.*
import com.rescuemate.utils.BluetoothHelper
import com.rescuemate.utils.BluetoothDeviceInfo
import com.rescuemate.utils.rememberBluetoothPermissionsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPairingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bluetoothHelper = remember { BluetoothHelper(context) }
    val healthMonitoringService = remember { HealthMonitoringService(context) }
    val smartwatchManager = remember { SmartwatchManager(context, healthMonitoringService) }
    val bluetoothPermissionsState = rememberBluetoothPermissionsState()
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
    var bleDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var pairingDevice by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var currentHeartRate by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        if (!bluetoothPermissionsState.allPermissionsGranted) {
            bluetoothPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Setup BLE callbacks
    LaunchedEffect(Unit) {
        // Monitor connection state
        // Note: In a real implementation, you'd use a callback or StateFlow
        // For now, we'll check periodically
    }

    // Check connection status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isConnected = smartwatchManager.isConnected()
            connectedDevice = smartwatchManager.getConnectedDevice()
            currentHeartRate = smartwatchManager.getCurrentHeartRate()
            kotlinx.coroutines.delay(1000)
        }
    }

    LaunchedEffect(bluetoothPermissionsState.allPermissionsGranted) {
        if (bluetoothPermissionsState.allPermissionsGranted && bluetoothHelper.isBluetoothEnabled()) {
            // Get paired devices
            val pairedDevices = bluetoothHelper.getPairedDevices()
            val smartwatchDevices = bluetoothHelper.filterSmartwatchDevices(pairedDevices)
            devices = smartwatchDevices
            
            // Start BLE scanning if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothHelper.isBLESupported()) {
                isScanning = true
                smartwatchManager.startScanning()
                
                // Stop scanning after 10 seconds
                kotlinx.coroutines.delay(10000)
                smartwatchManager.stopScanning()
                isScanning = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CosmicBackground,
                        CosmicCard,
                        CosmicCardHover
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CosmicTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bluetooth_pairing),
                        style = MaterialTheme.typography.titleLarge,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = "Pair your smartwatch",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission Check
            if (!bluetoothPermissionsState.allPermissionsGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicCardHover.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = CosmicTextSecondary
                        )
                        Text(
                            text = stringResource(R.string.bluetooth_permission_required),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                bluetoothPermissionsState.launchMultiplePermissionRequest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicPrimary
                            )
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else if (!bluetoothHelper.isBluetoothEnabled()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicCardHover.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = CosmicTextSecondary
                        )
                        Text(
                            text = stringResource(R.string.enable_bluetooth),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Device List
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CosmicPrimary
                            )
                            Text(
                                text = stringResource(R.string.scanning_devices),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmicTextSecondary
                            )
                        }
                    }
                } else if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = CosmicTextSecondary
                            )
                            Text(
                                text = stringResource(R.string.no_devices_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CosmicTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Connection Status Card
                    if (isConnected && connectedDevice != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF4CAF50)))
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Connected: ${connectedDevice?.name ?: "Unknown"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                if (currentHeartRate != null) {
                                    Text(
                                        text = "Heart Rate: $currentHeartRate BPM",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CosmicTextPrimary
                                    )
                                }
                                Button(
                                    onClick = {
                                        smartwatchManager.disconnect()
                                        isConnected = false
                                        connectedDevice = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF5252)
                                    )
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }
                    }
                    
                    // Scan Button
                    if (!isScanning && !isConnected) {
                        Button(
                            onClick = {
                                isScanning = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    smartwatchManager.startScanning()
                                    scope.launch {
                                        kotlinx.coroutines.delay(10000)
                                        smartwatchManager.stopScanning()
                                        isScanning = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan for Health Devices")
                        }
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(devices) { device ->
                            BluetoothDeviceCard(
                                device = device,
                                isPairing = pairingDevice?.address == device.address,
                                isConnected = isConnected && connectedDevice?.address == device.address,
                                onPairClick = {
                                    pairingDevice = device
                                    scope.launch {
                                        try {
                                            val connected = smartwatchManager.connectToDevice(device.device)
                                            if (connected) {
                                                isConnected = true
                                                connectedDevice = device.device
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("BluetoothPairing", "Connection error", e)
                                        } finally {
                                            pairingDevice = null
                                        }
                                    }
                                },
                                onDisconnectClick = {
                                    smartwatchManager.disconnect()
                                    isConnected = false
                                    connectedDevice = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceCard(
    device: BluetoothDeviceInfo,
    isPairing: Boolean,
    isConnected: Boolean = false,
    onPairClick: () -> Unit,
    onDisconnectClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CosmicCard
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = CosmicPrimary
                )
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary
                    )
                    if (device.isPaired) {
                        Text(
                            text = "Paired",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicPrimary
                        )
                    }
                }
            }
            if (isPairing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = CosmicPrimary,
                    strokeWidth = 2.dp
                )
            } else if (isConnected) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                    if (onDisconnectClick != null) {
                        TextButton(onClick = onDisconnectClick) {
                            Text(
                                text = "Disconnect",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                }
            } else if (!device.isPaired) {
                Button(
                    onClick = onPairClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Button(
                    onClick = onPairClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

