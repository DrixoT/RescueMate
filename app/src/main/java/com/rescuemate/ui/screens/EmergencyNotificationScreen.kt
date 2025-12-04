package com.rescuemate.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.rescuemate.data.UserPreferences
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.twilio.TwilioEmergencyService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyNotificationScreen(
    emergencyId: String? = null,
    userId: String? = null,
    userName: String? = null,
    emergencyType: String? = null,
    alertReason: String? = null,
    location: String? = null,
    timestamp: String? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val displayName = userName ?: "User"
    val displayType = emergencyType ?: "Emergency"
    val displayReason = alertReason ?: "Emergency protocol initiated"
    val displayLocation = location ?: "Location unavailable"
    val displayTimestamp = timestamp ?: "Just now"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Alert") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emergency Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$displayName has initiated an SOS Protocol",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Emergency Details
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Emergency Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "User",
                        value = displayName
                    )
                    
                    DetailRow(
                        icon = Icons.Default.Info,
                        label = "Emergency Type",
                        value = displayType
                    )
                    
                    DetailRow(
                        icon = Icons.Default.Report,
                        label = "How Triggered",
                        value = displayReason
                    )
                    
                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = displayLocation
                    )
                    
                    DetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Time",
                        value = displayTimestamp
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // View Location Button
                if (location != null && location.isNotBlank() && location != "Location unavailable") {
                    Button(
                        onClick = {
                            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(location))
                            context.startActivity(mapsIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Location on Map")
                    }
                }

                // Call User Button
                Button(
                    onClick = {
                        // Extract phone number from location or use stored user phone
                        val phoneNumber = extractPhoneFromLocation(location) ?: ""
                        if (phoneNumber.isNotEmpty()) {
                            val callIntent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:$phoneNumber")
                            }
                            context.startActivity(callIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call $displayName")
                }

                // Mark Safe Button
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            emergencyId?.let { id ->
                                val twilioService = TwilioEmergencyService(context)
                                val contactPhone = userPrefs.getUserPhone() ?: ""
                                val result = twilioService.submitContactResponse(
                                    emergencyId = id,
                                    contactPhone = contactPhone,
                                    response = EmergencyConstants.ContactResponse.USER_FINE,
                                    notes = "Marked safe via app notification"
                                )
                                
                                if (result.isSuccess) {
                                    onBack()
                                }
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && emergencyId != null
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Safe")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun extractPhoneFromLocation(location: String?): String? {
    // Try to extract phone number from location string if it contains one
    // This is a simple implementation - adjust based on your location format
    return null
}
