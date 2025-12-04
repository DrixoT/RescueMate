package com.rescuemate.emergency.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startForegroundService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rescuemate.emergency.service.EmergencyBackgroundService
import com.rescuemate.emergency.twilio.TwilioEmergencyService

/**
 * Emergency Configuration Screen
 * Setup and test emergency features
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EmergencyConfigurationScreen(
    userId: String,
    userName: String,
    userAge: Int,
    userPhone: String,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val twilioService = remember { TwilioEmergencyService(context) }
    val userPrefs = remember { com.rescuemate.data.UserPreferences(context) }
    var backendUrl by remember { mutableStateOf("http://10.0.2.2:3000") }
    var monitoringEnabled by remember { mutableStateOf(false) }
    var shakeEnabled by remember { mutableStateOf(true) }
    var volumeEnabled by remember { mutableStateOf(true) }
    var healthEnabled by remember { mutableStateOf(false) }
    var simulationMode by remember { mutableStateOf(userPrefs.getSimulationMode()) }
    var llmApiKey by remember { mutableStateOf("") }

    // Permission handling
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Permissions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (permissionState.allPermissionsGranted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Granted",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All permissions granted")
                        }
                    } else {
                        Button(
                            onClick = { permissionState.launchMultiplePermissionRequest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            // Backend Configuration
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Backend Configuration",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulation Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = simulationMode,
                            onCheckedChange = { 
                                simulationMode = it
                                userPrefs.setSimulationMode(it)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Emulator Simulation Mode")
                            Text(
                                "Use direct SMS/Calls for emulator testing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        label = { Text("Backend URL") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("http://10.0.2.2:3000") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            twilioService.setBackendUrl(backendUrl)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Backend URL")
                    }
                }
            }

            // LLM Configuration
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "LLM Health Analysis (Optional)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = llmApiKey,
                        onValueChange = { llmApiKey = it },
                        label = { Text("OpenAI API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-...") }
                    )

                    Text(
                        text = "Optional: For advanced health monitoring with AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Emergency Detection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Emergency Detection",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = shakeEnabled,
                            onCheckedChange = { shakeEnabled = it }
                        )
                        Text("Shake Detection (3+ shakes)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = volumeEnabled,
                            onCheckedChange = { volumeEnabled = it }
                        )
                        Text("Volume Button Sequence")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = healthEnabled,
                            onCheckedChange = { healthEnabled = it }
                        )
                        Text("Health Monitoring (requires smartwatch)")
                    }
                }
            }

            // Monitoring Control
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Background Monitoring",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (monitoringEnabled) {
                        Button(
                            onClick = {
                                val intent = Intent(context, EmergencyBackgroundService::class.java).apply {
                                    action = EmergencyBackgroundService.ACTION_STOP_MONITORING
                                }
                                context.stopService(intent)
                                monitoringEnabled = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop Monitoring")
                        }
                    } else {
                        Button(
                            onClick = {
                                val intent = Intent(context, EmergencyBackgroundService::class.java).apply {
                                    action = EmergencyBackgroundService.ACTION_START_MONITORING
                                    putExtra(EmergencyBackgroundService.EXTRA_USER_ID, userId)
                                    putExtra(EmergencyBackgroundService.EXTRA_USER_NAME, userName)
                                    putExtra(EmergencyBackgroundService.EXTRA_USER_AGE, userAge)
                                    putExtra(EmergencyBackgroundService.EXTRA_USER_PHONE, userPhone)
                                    putExtra(EmergencyBackgroundService.EXTRA_ENABLE_SHAKE, shakeEnabled)
                                    putExtra(EmergencyBackgroundService.EXTRA_ENABLE_VOLUME, volumeEnabled)
                                    putExtra(EmergencyBackgroundService.EXTRA_ENABLE_HEALTH, healthEnabled)
                                    putExtra(EmergencyBackgroundService.EXTRA_LLM_API_KEY, llmApiKey)
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(context, intent)
                                } else {
                                    context.startService(intent)
                                }
                                monitoringEnabled = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = permissionState.allPermissionsGranted
                        ) {
                            Text("Start Monitoring")
                        }
                    }

                    if (!permissionState.allPermissionsGranted) {
                        Text(
                            text = "Grant all permissions to enable monitoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // System Status
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "System Status",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StatusRow("Permissions", permissionState.allPermissionsGranted)
                    StatusRow("Simulation Mode", simulationMode)
                    StatusRow("Backend URL", backendUrl.isNotBlank())
                    StatusRow("Monitoring", monitoringEnabled)
                    StatusRow("Shake Detection", shakeEnabled)
                    StatusRow("Volume Buttons", volumeEnabled)
                    StatusRow("Health Monitoring", healthEnabled)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (enabled) "Enabled" else "Disabled",
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

