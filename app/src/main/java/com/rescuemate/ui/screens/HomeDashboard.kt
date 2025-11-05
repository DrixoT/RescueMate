package com.rescuemate.ui.screens

import androidx.compose.animation.core.*
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.rescuemate.R
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.service.EmergencyBackgroundService
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeDashboard(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val emergencyManager = remember { EmergencyManager(context) }
    val healthMonitoring = remember { HealthMonitoringService(context) }
    
    // Cleanup resources when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            Log.d("HomeDashboard", "Cleaning up resources")
            emergencyManager.cleanup()
        }
    }
    
    // Monitoring state
    var isMonitoringActive by remember { mutableStateOf(false) }
    var currentHeartRate by remember { mutableStateOf<Int?>(null) }
    var lastHealthCheck by remember { mutableStateOf<Long?>(null) }
    var currentEmergency by remember { mutableStateOf(emergencyManager.getCurrentEmergency()) }
    
    // AI Conversation state
    var isInAIConversation by remember { mutableStateOf(false) }
    var aiConversationMode by remember { mutableStateOf("idle") } // idle, listening, speaking
    
    // Check service status periodically with error handling
    LaunchedEffect(Unit) {
        while (true) {
            try {
                // Check if service is running (simplified check)
                isMonitoringActive = checkServiceRunning(context)
                
                // Get current heart rate from SharedPreferences (real-time)
                val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                val savedHeartRate = prefs.getInt("current_heart_rate", 0)
                val lastUpdate = prefs.getLong("last_heart_rate_update", 0)
                val timeSinceUpdate = System.currentTimeMillis() - lastUpdate
                
                // Only use saved heart rate if it's recent (less than 30 seconds old)
                currentHeartRate = if (savedHeartRate > 0 && timeSinceUpdate < 30000) {
                    savedHeartRate
                } else if (isMonitoringActive) {
                    null // Show "Connecting..." while waiting for first reading
                } else {
                    null // No monitoring active
                }
                
                // Update emergency status
                currentEmergency = emergencyManager.getCurrentEmergency()
                
                // Check AI conversation state from SharedPreferences
                val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
                isInAIConversation = conversationPrefs.getBoolean("is_active", false)
                aiConversationMode = conversationPrefs.getString("mode", "idle") ?: "idle"
                
                delay(500L) // Update every 500ms for responsive animation
            } catch (e: Exception) {
                Log.e("HomeDashboard", "Error in monitoring loop", e)
                // Continue loop even if there's an error
                delay(500L)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.rescuemate),
                        style = MaterialTheme.typography.headlineMedium,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = if (isMonitoringActive) "Protection Active" else "Monitoring Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMonitoringActive) Color(0xFF4CAF50) else CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )
                }
                IconButton(
                    onClick = { onNavigate("settings") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = CosmicTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Emergency Status Card
            if (currentEmergency != null) {
                EmergencyStatusCard(
                    emergency = currentEmergency!!,
                    onConfirmSafe = {
                        emergencyManager.userConfirmSafe()
                        currentEmergency = null
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Monitoring Status Card
            MonitoringStatusCard(
                isActive = isMonitoringActive,
                currentHeartRate = currentHeartRate,
                lastHealthCheck = lastHealthCheck,
                onStartMonitoring = {
                    try {
                        // Check if smartwatch is connected
                        val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                        val smartwatchConnected = prefs.getBoolean(EmergencyConstants.PREF_KEY_SMARTWATCH_CONNECTED, false)
                        
                        if (!smartwatchConnected) {
                            Log.w("HomeDashboard", "Smartwatch not connected")
                            Toast.makeText(
                                context,
                                "Please connect a smartwatch in Settings → Devices to start monitoring",
                                Toast.LENGTH_LONG
                            ).show()
                            return@MonitoringStatusCard
                        }
                        
                        // Check for required permissions before starting service
                        if (checkMonitoringPermissions(context)) {
                            startMonitoringService(context)
                            isMonitoringActive = true
                            Log.d("HomeDashboard", "Monitoring service started successfully")
                            Toast.makeText(context, "Monitoring started - Stay safe!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w("HomeDashboard", "Missing required permissions for monitoring")
                            Toast.makeText(context, "Please grant all permissions in Settings", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("HomeDashboard", "Failed to start monitoring service", e)
                        isMonitoringActive = false
                        Toast.makeText(context, "Error starting monitoring: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onStopMonitoring = {
                    try {
                        stopMonitoringService(context)
                        isMonitoringActive = false
                        Log.d("HomeDashboard", "Monitoring service stopped")
                        Toast.makeText(context, "Monitoring stopped", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("HomeDashboard", "Failed to stop monitoring service", e)
                        Toast.makeText(context, "Error stopping monitoring: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status Indicators + User Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    icon = Icons.Default.LocationOn,
                    text = stringResource(R.string.location_active)
                )
                StatusBadge(
                    icon = Icons.Default.Wifi,
                    text = stringResource(R.string.network_secure)
                )
                Spacer(modifier = Modifier.weight(1f))

                // User Profile Icon
                IconButton(
                    onClick = { onNavigate("profile") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = CosmicCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            CosmicBorder
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = CosmicTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main SOS Button - Centered
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SOSButton(
                    onClick = {
                        // Trigger manual emergency
                        val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                        val userId = prefs.getString("user_id", "user_${System.currentTimeMillis()}") ?: "user_${System.currentTimeMillis()}"
                        val userName = prefs.getString("user_name", "User") ?: "User"
                        val userAge = prefs.getInt("user_age", 0)
                        val userPhone = prefs.getString("user_phone", "") ?: ""
                        
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val userInfo = com.rescuemate.emergency.data.UserInfo(
                                    userId = userId,
                                    name = userName,
                                    age = userAge,
                                    phoneNumber = userPhone,
                                    medicalInfo = emergencyManager.database.getMedicalInfo(userId) 
                                        ?: com.rescuemate.emergency.data.MedicalInfo(userId = userId)
                                )
                                emergencyManager.triggerManualEmergency(userId, userInfo)
                            } catch (e: Exception) {
                                Log.e("HomeDashboard", "Error triggering manual emergency", e)
                            }
                        }
                    },
                    isInAIConversation = isInAIConversation,
                    aiConversationMode = aiConversationMode
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.People,
                    text = stringResource(R.string.contacts),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("contacts") }
                )
                QuickActionButton(
                    icon = Icons.Default.LocationOn,
                    text = stringResource(R.string.live_location),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("location") }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Safety Tip
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
                        text = stringResource(R.string.safety_tip_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.safety_tip_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = CosmicCard,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = CosmicTextPrimary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = CosmicTextPrimary
            )
        }
    }
}

@Composable
fun SOSButton(
    onClick: () -> Unit,
    isInAIConversation: Boolean = false,
    aiConversationMode: String = "idle"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalView.current
    
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    
    // Base animation - always active
    val infiniteTransition = rememberInfiniteTransition(label = "sos_glow")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isInAIConversation) 1.3f else 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isInAIConversation) 1000 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isInAIConversation) 1.25f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isInAIConversation) 1000 else 2000, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )
    
    // AI Conversation animations
    val aiPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isInAIConversation) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_pulse"
    )
    
    val aiRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isInAIConversation) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_rotation"
    )
    
    val aiGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isInAIConversation) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_glow"
    )
    
    // Color transitions based on mode
    val buttonColor = when {
        aiConversationMode == "listening" -> Color(0xFF4CAF50) // Green for listening
        aiConversationMode == "speaking" -> CosmicPrimary // Purple for speaking
        else -> CosmicPrimary
    }

    Box(
        modifier = Modifier.size(224.dp),
        contentAlignment = Alignment.Center
    ) {
        // AI Conversation - Extra animated glow rings
        if (isInAIConversation) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(aiPulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = aiGlowAlpha * 0.5f),
                                Color.Transparent
                            ),
                            radius = 350f
                        ),
                        shape = CircleShape
                    )
            )
            
            // Voice activity indicator rings
            if (aiConversationMode == "listening" || aiConversationMode == "speaking") {
                repeat(3) { index ->
                    val delay = index * 400
                    val ringScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, delayMillis = delay, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ring_$index"
                    )
                    val ringAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, delayMillis = delay, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "ring_alpha_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(224.dp)
                            .scale(ringScale)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        buttonColor.copy(alpha = ringAlpha),
                                        Color.Transparent
                                    ),
                                    radius = 280f
                                ),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
        
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(224.dp)
                .scale(scale1)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = if (isInAIConversation) 0.6f else 0.4f),
                            Color.Transparent
                        ),
                        radius = 280f
                    ),
                    shape = CircleShape
                )
        )

        // Middle glow ring
        Box(
            modifier = Modifier
                .size(224.dp)
                .scale(scale2)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CosmicPrimary.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 240f
                    ),
                    shape = CircleShape
                )
        )

        // SOS Button with dual functionality: tap for wellness AI, hold for emergency
        Box(
            modifier = Modifier
                .size(224.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Single tap - launch wellness AI conversation
                            onNavigate("wellness_ai")
                        },
                        onPress = {
                            isPressed = true
                            holdProgress = 0f
                            
                            // Haptic feedback on press start
                            hapticFeedback.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            
                            // Start hold timer
                            holdJob?.cancel()
                            holdJob = scope.launch {
                                val holdDuration = 3000L // 3 seconds
                                val updateInterval = 50L
                                val steps = (holdDuration / updateInterval).toInt()
                                
                                for (i in 1..steps) {
                                    delay(updateInterval)
                                    holdProgress = i.toFloat() / steps
                                }
                                
                                // Hold completed - trigger emergency with error handling
                                if (holdProgress >= 1f) {
                                    // Strong haptic feedback on completion
                                    hapticFeedback.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    
                                    // Check if emergency contacts exist
                                    try {
                                        val contacts = emergencyManager.database.getAllContacts()
                                        if (contacts.isEmpty()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Please add at least one emergency contact first",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            return@launch
                                        }
                                        
                                        // Trigger emergency
                                        onClick()
                                    } catch (e: Exception) {
                                        Log.e("HomeDashboard", "Error triggering emergency", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Failed to trigger SOS: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                            
                            // Wait for release
                            tryAwaitRelease()
                            
                            // Released before completion - cancel
                            isPressed = false
                            holdJob?.cancel()
                            holdProgress = 0f
                        }
                    )
                }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(CosmicPrimary, CosmicPrimaryDark)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Progress indicator when holding
                    if (holdProgress > 0f) {
                        CircularProgressIndicator(
                            progress = holdProgress,
                            modifier = Modifier.size(240.dp),
                            color = Color.White,
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                    
                    // Draw outlined shield instead of filled icon
                    Canvas(
                        modifier = Modifier.size(125.dp)
                    ) {
                        val shieldPath = Path().apply {
                            // Start at top center
                            moveTo(size.width * 0.5f, size.height * 0.05f)

                            // Top right
                            lineTo(size.width * 0.85f, size.height * 0.2f)

                            // Right side down
                            lineTo(size.width * 0.85f, size.height * 0.55f)

                            // Curve to bottom point
                            cubicTo(
                                size.width * 0.85f, size.height * 0.75f,
                                size.width * 0.65f, size.height * 0.92f,
                                size.width * 0.5f, size.height * 0.95f
                            )

                            // Curve from bottom to left
                            cubicTo(
                                size.width * 0.35f, size.height * 0.92f,
                                size.width * 0.15f, size.height * 0.75f,
                                size.width * 0.15f, size.height * 0.55f
                            )

                            // Left side up
                            lineTo(size.width * 0.15f, size.height * 0.2f)

                            // Close path
                            close()
                        }

                        // Draw thick outline
                        drawPath(
                            path = shieldPath,
                            color = Color.White,
                            style = Stroke(
                                width = 10f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    
                    // Instruction text
                    if (!isPressed) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Tap: Talk to AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Hold 3s: Emergency",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = "Hold for 3s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CosmicCard,
            contentColor = CosmicTextPrimary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Check if emergency background service is running
 */
private fun checkServiceRunning(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
    return runningServices.any { 
        it.service.className == EmergencyBackgroundService::class.java.name 
    }
}

/**
 * Check if required permissions for monitoring are granted
 */
private fun checkMonitoringPermissions(context: Context): Boolean {
    val requiredPermissions = mutableListOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.FOREGROUND_SERVICE
    )

    // Check for Android 13+ notification permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    // Check all permissions
    val missingPermissions = requiredPermissions.filter { permission ->
        androidx.core.content.ContextCompat.checkSelfPermission(context, permission) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    if (missingPermissions.isNotEmpty()) {
        Log.w("HomeDashboard", "Missing permissions: ${missingPermissions.joinToString()}")
        return false
    }

    return true
}

/**
 * Start monitoring service
 */
private fun startMonitoringService(context: Context) {
    val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
    val userId = prefs.getString("user_id", "user_${System.currentTimeMillis()}") ?: "user_${System.currentTimeMillis()}"
    val userName = prefs.getString("user_name", "User") ?: "User"
    val userAge = prefs.getInt("user_age", 0)
    val userPhone = prefs.getString("user_phone", "") ?: ""
    val llmApiKey = prefs.getString("openai_api_key", null)
    
    val intent = Intent(context, EmergencyBackgroundService::class.java).apply {
        action = EmergencyBackgroundService.ACTION_START_MONITORING
        putExtra(EmergencyBackgroundService.EXTRA_USER_ID, userId)
        putExtra(EmergencyBackgroundService.EXTRA_USER_NAME, userName)
        putExtra(EmergencyBackgroundService.EXTRA_USER_AGE, userAge)
        putExtra(EmergencyBackgroundService.EXTRA_USER_PHONE, userPhone)
        putExtra(EmergencyBackgroundService.EXTRA_ENABLE_SHAKE, true)
        putExtra(EmergencyBackgroundService.EXTRA_ENABLE_VOLUME, true)
        putExtra(EmergencyBackgroundService.EXTRA_ENABLE_HEALTH, true)
        llmApiKey?.let { putExtra(EmergencyBackgroundService.EXTRA_LLM_API_KEY, it) }
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    
    Log.d("HomeDashboard", "Monitoring service started")
}

/**
 * Stop monitoring service
 */
private fun stopMonitoringService(context: Context) {
    val intent = Intent(context, EmergencyBackgroundService::class.java).apply {
        action = EmergencyBackgroundService.ACTION_STOP_MONITORING
    }
    context.stopService(intent)
    Log.d("HomeDashboard", "Monitoring service stopped")
}

/**
 * Emergency Status Card - Shows active emergency info
 */
@Composable
fun EmergencyStatusCard(
    emergency: com.rescuemate.emergency.data.EmergencyEvent,
    onConfirmSafe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF5252).copy(alpha = 0.2f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF5252)))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Emergency Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF5252)
                    )
                }
                Text(
                    text = "Phase ${emergency.currentPhase}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextSecondary
                )
            }
            
            Text(
                text = emergency.healthData.alertReason,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmicTextPrimary
            )
            
            Button(
                onClick = onConfirmSafe,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("I'm Safe - Cancel Emergency")
            }
        }
    }
}

/**
 * Monitoring Status Card - Shows monitoring service status
 */
@Composable
fun MonitoringStatusCard(
    isActive: Boolean,
    currentHeartRate: Int?,
    lastHealthCheck: Long?,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFFFF5252) else CosmicTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isActive) "Monitoring Active" else "Monitoring Inactive",
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicTextPrimary
                    )
                }
                
                if (isActive) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "●",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Heart Rate:",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary
                    )
                    if (currentHeartRate != null) {
                        Text(
                            text = "$currentHeartRate BPM",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Connecting...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextSecondary.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            
            if (lastHealthCheck != null) {
                val timeAgo = (System.currentTimeMillis() - lastHealthCheck) / 1000
                Text(
                    text = "Last check: ${timeAgo}s ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicTextSecondary
                )
            }
            
            Button(
                onClick = if (isActive) onStopMonitoring else onStartMonitoring,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFFFF5252) else CosmicPrimary
                )
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isActive) "Stop Monitoring" else "Start Monitoring")
            }
        }
    }
}

