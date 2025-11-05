package com.rescuemate.emergency.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.data.MedicalInfo
import com.rescuemate.emergency.data.UserInfo
import kotlinx.coroutines.launch

/**
 * Emergency Panic Button Screen
 * Large, prominent button for manual emergency triggering
 */
@Composable
fun EmergencyPanicButton(
    userId: String,
    userName: String,
    userAge: Int,
    userPhone: String,
    onEmergencyTriggered: () -> Unit = {}
) {
    val context = LocalContext.current
    val emergencyManager = remember { EmergencyManager(context) }
    val scope = rememberCoroutineScope()
    var isTriggering by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large Emergency Button
            Button(
                onClick = { showConfirmDialog = true },
                enabled = !isTriggering,
                modifier = Modifier
                    .size(200.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SOS",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "EMERGENCY",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Hold to trigger emergency response",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            if (isTriggering) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
                Text(
                    text = "Triggering emergency...",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Trigger Emergency?") },
            text = {
                Text("This will notify all your emergency contacts and start the emergency response workflow.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isTriggering = true
                        scope.launch {
                            try {
                                val userInfo = UserInfo(
                                    userId = userId,
                                    name = userName,
                                    age = userAge,
                                    phoneNumber = userPhone,
                                    medicalInfo = MedicalInfo(userId = userId)
                                )

                                val result = emergencyManager.triggerManualEmergency(
                                    userId = userId,
                                    userInfo = userInfo
                                )

                                if (result.isSuccess) {
                                    onEmergencyTriggered()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isTriggering = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("TRIGGER EMERGENCY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Emergency Status Card - Shows current emergency status
 */
@Composable
fun EmergencyStatusCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val emergencyManager = remember { EmergencyManager(context) }
    val currentEmergency = emergencyManager.getCurrentEmergency()

    if (currentEmergency != null && currentEmergency.isActive()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Red.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Active Emergency",
                        tint = Color.Red,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "ACTIVE EMERGENCY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Red
                        )
                        Text(
                            text = "Phase ${currentEmergency.currentPhase}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Emergency Type: ${currentEmergency.emergencyType.displayName}",
                    fontSize = 14.sp
                )

                Text(
                    text = "Duration: ${currentEmergency.getDurationSeconds() / 60} minutes",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        emergencyManager.userConfirmSafe()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("I'M SAFE - CANCEL EMERGENCY")
                }
            }
        }
    }
}

/**
 * Quick Emergency Actions Bottom Sheet
 */
@Composable
fun EmergencyQuickActions(
    userId: String,
    userName: String,
    userAge: Int,
    userPhone: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Emergency Quick Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Panic Button
            EmergencyPanicButton(
                userId = userId,
                userName = userName,
                userAge = userAge,
                userPhone = userPhone
            )
        }
    }
}

