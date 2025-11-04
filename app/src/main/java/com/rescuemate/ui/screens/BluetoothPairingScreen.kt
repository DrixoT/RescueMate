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
import com.rescuemate.R
import com.rescuemate.ui.theme.*
import com.rescuemate.utils.BluetoothHelper
import com.rescuemate.utils.BluetoothDeviceInfo
import com.rescuemate.utils.rememberBluetoothPermissionsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPairingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bluetoothHelper = remember { BluetoothHelper(context) }
    val bluetoothPermissionsState = rememberBluetoothPermissionsState()
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
    var pairingDevice by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }

    LaunchedEffect(Unit) {
        if (!bluetoothPermissionsState.allPermissionsGranted) {
            bluetoothPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(bluetoothPermissionsState.allPermissionsGranted) {
        if (bluetoothPermissionsState.allPermissionsGranted && bluetoothHelper.isBluetoothEnabled()) {
            isScanning = true
            val pairedDevices = bluetoothHelper.getPairedDevices()
            val smartwatchDevices = bluetoothHelper.filterSmartwatchDevices(pairedDevices)
            devices = smartwatchDevices
            isScanning = false
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
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(devices) { device ->
                            BluetoothDeviceCard(
                                device = device,
                                isPairing = pairingDevice?.address == device.address,
                                onPairClick = {
                                    pairingDevice = device
                                    // Handle pairing logic here
                                    // For now, just simulate pairing
                                    pairingDevice = null
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
    onPairClick: () -> Unit
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
            } else if (!device.isPaired) {
                Button(
                    onClick = onPairClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.pair),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.paired),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicPrimary
                )
            }
        }
    }
}

