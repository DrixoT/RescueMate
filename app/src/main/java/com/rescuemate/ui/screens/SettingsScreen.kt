package com.rescuemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.rescuemate.R
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.ui.theme.*
import com.rescuemate.ui.navigation.Screen

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBluetooth: () -> Unit,
    onNavigateToVoiceAI: () -> Unit = {},
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
    
    var autoSendAlert by remember { mutableStateOf(true) }
    var locationTracking by remember { mutableStateOf(true) }
    var soundAlerts by remember { mutableStateOf(true) }
    
    // Smartwatch connection state
    var smartwatchConnected by remember { 
        mutableStateOf(prefs.getBoolean(EmergencyConstants.PREF_KEY_SMARTWATCH_CONNECTED, false)) 
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
                Column {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.configure_protection),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Emergency Settings
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.emergency_settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        Column {
                            SettingItem(
                                icon = Icons.Default.Notifications,
                                title = stringResource(R.string.auto_send_alert),
                                description = stringResource(R.string.auto_send_alert_desc),
                                checked = autoSendAlert,
                                onCheckedChange = { autoSendAlert = it }
                            )
                            Divider(color = CosmicBorder)
                            SettingItem(
                                icon = Icons.Default.LocationOn,
                                title = stringResource(R.string.location_tracking),
                                description = stringResource(R.string.location_tracking_desc),
                                checked = locationTracking,
                                onCheckedChange = { locationTracking = it }
                            )
                            Divider(color = CosmicBorder)
                            SettingItem(
                                icon = Icons.Default.Shield,
                                title = stringResource(R.string.sound_alerts),
                                description = stringResource(R.string.sound_alerts_desc),
                                checked = soundAlerts,
                                onCheckedChange = { soundAlerts = it }
                            )
                        }
                    }
                }

                // AI & Automation
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_automation),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        SettingButton(
                            icon = Icons.Default.Mic,
                            title = stringResource(R.string.setup_voice_ai),
                            description = stringResource(R.string.setup_voice_ai_desc),
                            onClick = onNavigateToVoiceAI
                        )
                    }
                }

                // Devices
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Devices",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Device Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = CosmicCardHover,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Watch,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(20.dp),
                                            tint = CosmicPrimary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "RescueMate Watch Pro",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = CosmicTextPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (smartwatchConnected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                                contentDescription = null,
                                                tint = if (smartwatchConnected) Color(0xFF4CAF50) else CosmicTextSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = if (smartwatchConnected) "Connected" else "Not Connected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (smartwatchConnected) Color(0xFF4CAF50) else CosmicTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Connection Button
                            Button(
                                onClick = {
                                    smartwatchConnected = !smartwatchConnected
                                    prefs.edit().apply {
                                        putBoolean(EmergencyConstants.PREF_KEY_SMARTWATCH_CONNECTED, smartwatchConnected)
                                        if (smartwatchConnected) {
                                            putString(EmergencyConstants.PREF_KEY_SMARTWATCH_NAME, "RescueMate Watch Pro")
                                        }
                                        apply()
                                    }
                                    val message = if (smartwatchConnected) {
                                        "Smartwatch connected successfully"
                                    } else {
                                        "Smartwatch disconnected"
                                    }
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (smartwatchConnected) Color(0xFFFF5252) else CosmicPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (smartwatchConnected) Icons.Default.LinkOff else Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (smartwatchConnected) "Disconnect" else "Connect")
                            }
                            
                            if (!smartwatchConnected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: Smartwatch required for health monitoring",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                // Appearance
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.appearance),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        SettingButton(
                            icon = Icons.Default.Palette,
                            title = stringResource(R.string.theme),
                            description = stringResource(R.string.cosmic_dark_default),
                            onClick = { /* Handle theme */ }
                        )
                    }
                }

                // Account Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        SettingButton(
                            icon = Icons.Default.Logout,
                            title = "Sign Out",
                            description = "Log out of your account",
                            onClick = {
                                val userPrefs = com.rescuemate.data.UserPreferences(context)
                                userPrefs.logout()
                                userPrefs.setOnboardingComplete(false)
                                android.widget.Toast.makeText(context, "Signed out successfully", android.widget.Toast.LENGTH_SHORT).show()
                                
                                // Navigate to SignIn screen and clear back stack
                                navController?.navigate(Screen.SignIn.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                // About
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicCard
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                        )
                    ) {
                        SettingButton(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.app_information),
                            description = stringResource(R.string.version),
                            onClick = { /* Handle app info */ }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy Notice
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
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.privacy_security),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.privacy_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            Surface(
                color = CosmicCardHover,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = CosmicPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CosmicTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicTextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CosmicPrimary
            )
        )
    }
}

@Composable
fun SettingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
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
            Surface(
                color = CosmicCardHover,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = CosmicPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = CosmicTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicTextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = CosmicTextSecondary
        )
        }
    }
}

