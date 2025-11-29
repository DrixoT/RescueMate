package com.rescuemate.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rescuemate.R
import com.rescuemate.emergency.EmergencyConstants
import com.rescuemate.emergency.EmergencyManager
import com.rescuemate.emergency.service.EmergencyBackgroundService
import com.rescuemate.emergency.health.HealthMonitoringService
import com.rescuemate.services.ElevenLabsConversationalService
import com.rescuemate.ui.components.*
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
    
    // Safe initialization of services
    var startupError by remember { mutableStateOf<String?>(null) }
    
    val emergencyManager = remember {
        try {
            EmergencyManager(context)
        } catch (e: Exception) {
            Log.e("HomeDashboard", "CRITICAL: Failed to initialize EmergencyManager", e)
            startupError = "Emergency System Error: ${e.message}"
            null
        }
    }
    
    val healthMonitoring = remember {
        try {
            HealthMonitoringService(context)
        } catch (e: Exception) {
            Log.w("HomeDashboard", "Failed to initialize HealthMonitoringService", e)
            null
        }
    }
    
    val conversationalService = remember {
        try {
            ElevenLabsConversationalService(context)
        } catch (e: Exception) {
            Log.w("HomeDashboard", "Failed to initialize ElevenLabsConversationalService", e)
            null
        }
    }
    
    // Cleanup resources when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            Log.d("HomeDashboard", "Cleaning up resources")
            try {
                emergencyManager?.cleanup()
                conversationalService?.cleanup()
            } catch (e: Exception) {
                Log.e("HomeDashboard", "Error during cleanup", e)
            }
        }
    }
    
    // Monitoring state
    var isMonitoringActive by remember { mutableStateOf(false) }
    var currentHeartRate by remember { mutableStateOf<Int?>(null) }
    var lastHealthCheck by remember { mutableStateOf<Long?>(null) }
    var currentEmergency by remember { mutableStateOf(emergencyManager?.getCurrentEmergency()) }
    
    // Voice Conversation state
    var isVoiceConversationActive by remember { mutableStateOf(false) }
    var aiAudioLevel by remember { mutableStateOf(0f) }
    var conversationStatus by remember { mutableStateOf("") }
    var aiConversationMode by remember { mutableStateOf("idle") } // idle, listening, speaking
    var userTextInput by remember { mutableStateOf("") }
    
    // Error handling state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    
    // Show startup error if present
    LaunchedEffect(startupError) {
        if (startupError != null) {
            errorMessage = startupError
            showErrorDialog = true
        }
    }
    
    // SOS Confirmation Dialog state
    var showSOSConfirmation by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(10) }
    var autoConfirmJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    // Toast state
    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    // Permission launcher for RECORD_AUDIO
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && conversationalService != null) {
            // Permission granted, start conversation
            scope.launch {
                startVoiceConversation(
                    context = context,
                    conversationalService = conversationalService,
                    onSuccess = { conversationId ->
                        isVoiceConversationActive = true
                        conversationStatus = "connected"
                        val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
                        conversationPrefs.edit().putBoolean("is_active", true).apply()
                        Log.d("HomeDashboard", "Voice conversation connected: $conversationId")
                    },
                    onModeChange = { mode ->
                        aiConversationMode = mode
                        conversationStatus = mode
                        Log.d("HomeDashboard", "Mode changed: $mode")
                    },
                    onStatusChange = { status ->
                        conversationStatus = status
                        Log.d("HomeDashboard", "Status: $status")
                    },
                    onMessage = { source, messageJson ->
                        Log.d("HomeDashboard", "Message from $source: $messageJson")
                    },
                    onError = { error ->
                        Log.e("HomeDashboard", "Conversation error: $error")
                        isVoiceConversationActive = false
                        conversationStatus = ""
                        aiConversationMode = "idle"
                        val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
                        conversationPrefs.edit().putBoolean("is_active", false).apply()
                        errorMessage = error
                        showErrorDialog = true
                    },
                    onDisconnect = {
                        isVoiceConversationActive = false
                        conversationStatus = ""
                        aiConversationMode = "idle"
                        val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
                        conversationPrefs.edit().putBoolean("is_active", false).apply()
                        Log.d("HomeDashboard", "Voice conversation disconnected")
                    },
                    onCanSendFeedback = { canSend ->
                        Log.d("HomeDashboard", "Can send feedback: $canSend")
                    },
                    onAudioLevelChange = { level ->
                        aiAudioLevel = level
                    }
                )
            }
        } else {
            // Permission denied or service null
            errorMessage = if (conversationalService == null) 
                "Voice AI service is unavailable." 
            else 
                "Microphone permission is required for voice conversation."
            showErrorDialog = true
        }
    }
    
    // Check service status periodically
    LaunchedEffect(Unit) {
        while (true) {
            try {
                isMonitoringActive = checkServiceRunning(context)
                val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                val savedHeartRate = prefs.getInt("current_heart_rate", 0)
                val lastUpdate = prefs.getLong("last_heart_rate_update", 0)
                val timeSinceUpdate = System.currentTimeMillis() - lastUpdate
                
                currentHeartRate = if (savedHeartRate > 0 && timeSinceUpdate < 30000) {
                    savedHeartRate
                } else if (isMonitoringActive) {
                    null
                } else {
                    null
                }
                
                if (emergencyManager != null) {
                    currentEmergency = emergencyManager.getCurrentEmergency()
                }
                
                val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
                isVoiceConversationActive = conversationPrefs.getBoolean("is_active", false)
                aiConversationMode = conversationPrefs.getString("mode", "idle") ?: "idle"
                
                delay(500L)
            } catch (e: Exception) {
                Log.e("HomeDashboard", "Error in monitoring loop", e)
                delay(500L)
            }
        }
    }
    
    CosmicScaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            text = stringResource(R.string.rescuemate).uppercase(),
                            style = AsciiLarge,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = if (isMonitoringActive) "PROTECTION ACTIVE" else "MONITORING INACTIVE",
                            style = AsciiSmall,
                            color = if (isMonitoringActive) Color(0xFF4CAF50) else CosmicTextSecondary
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
                
                Text(
                    text = AsciiArt.GALAXY_DIVIDER,
                    style = AsciiSmall,
                    color = CosmicTextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Emergency Status Card
                if (currentEmergency != null) {
                    EmergencyStatusCard(
                        emergency = currentEmergency!!,
                        onConfirmSafe = {
                            emergencyManager?.userConfirmSafe()
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
                            if (checkMonitoringPermissions(context)) {
                                startMonitoringService(context)
                                isMonitoringActive = true
                                toastMessage = "Monitoring Started"
                            } else {
                                Log.w("HomeDashboard", "Missing required permissions")
                                toastMessage = "Missing Permissions"
                            }
                        } catch (e: Exception) {
                            Log.e("HomeDashboard", "Failed to start monitoring", e)
                            isMonitoringActive = false
                            toastMessage = "Failed to Start"
                        }
                    },
                    onStopMonitoring = {
                        try {
                            stopMonitoringService(context)
                            isMonitoringActive = false
                            toastMessage = "Monitoring Stopped"
                        } catch (e: Exception) {
                            Log.e("HomeDashboard", "Failed to stop monitoring", e)
                        }
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Planetary SOS Button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PlanetarySOSButton(
                        onClick = { /* Handled by hold/tap */ },
                        onTap = {
                            if (isVoiceConversationActive) {
                                conversationalService?.endConversation()
                                isVoiceConversationActive = false
                                conversationStatus = ""
                                aiConversationMode = "idle"
                                val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
                                conversationPrefs.edit().putBoolean("is_active", false).apply()
                                toastMessage = "Voice Session Ended"
                            } else {
                                if (conversationalService == null) {
                                    errorMessage = "Voice service unavailable. Please restart app."
                                    showErrorDialog = true
                                    return@PlanetarySOSButton
                                }
                                
                                val prefs = context.getSharedPreferences("voice_ai_prefs", Context.MODE_PRIVATE)
                                val agentId = prefs.getString("agent_id", com.rescuemate.BuildConfig.ELEVEN_AGENT_ID) 
                                    ?: com.rescuemate.BuildConfig.ELEVEN_AGENT_ID
                                
                                if (agentId.isBlank() || agentId == "YOUR_AGENT_ID_HERE") {
                                    errorMessage = "Voice AI not configured. Check Settings."
                                    showErrorDialog = true
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (!hasPermission) {
                                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        scope.launch {
                                            startVoiceConversation(
                                                context = context,
                                                conversationalService = conversationalService,
                                                onSuccess = { 
                                                    isVoiceConversationActive = true 
                                                    toastMessage = "Voice Connected"
                                                },
                                                onModeChange = { aiConversationMode = it },
                                                onStatusChange = { conversationStatus = it },
                                                onMessage = { _, _ -> },
                                                onError = { 
                                                    errorMessage = it
                                                    showErrorDialog = true
                                                },
                                                onDisconnect = { 
                                                    isVoiceConversationActive = false 
                                                    toastMessage = "Voice Disconnected"
                                                },
                                                onCanSendFeedback = {},
                                                onAudioLevelChange = { aiAudioLevel = it }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        isInAIConversation = isVoiceConversationActive,
                        aiConversationMode = aiConversationMode,
                        audioLevel = aiAudioLevel,
                        onShowConfirmation = {
                            if (emergencyManager == null) {
                                errorMessage = "Cannot initiate SOS: Emergency Manager failed to initialize."
                                showErrorDialog = true
                                return@PlanetarySOSButton
                            }
                            
                            showSOSConfirmation = true
                            remainingSeconds = 10
                            autoConfirmJob?.cancel()
                            autoConfirmJob = scope.launch {
                                while (remainingSeconds > 0 && showSOSConfirmation) {
                                    delay(1000)
                                    remainingSeconds--
                                }
                                if (showSOSConfirmation && remainingSeconds == 0) {
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
                                                medicalInfo = emergencyManager.database.getMedicalInfo(userId).getOrNull() 
                                                    ?: com.rescuemate.emergency.data.MedicalInfo(userId = userId)
                                            )
                                            emergencyManager.triggerManualEmergency(userId, userInfo)
                                        } catch (e: Exception) {
                                            Log.e("SOS", "Error triggering emergency", e)
                                        }
                                    }
                                    showSOSConfirmation = false
                                }
                            }
                        }
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
            }
            
            // Overlays
            CosmicOverlay(
                visible = showSOSConfirmation,
                title = "PLANETARY DISTRESS",
                message = "Initiating emergency sequence in $remainingSeconds seconds.\n\nStand by for extraction protocol.",
                confirmText = "INITIATE NOW",
                dismissText = "ABORT",
                onConfirm = {
                    autoConfirmJob?.cancel()
                    showSOSConfirmation = false
                    val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
                    val userId = prefs.getString("user_id", "user_${System.currentTimeMillis()}") ?: "user_${System.currentTimeMillis()}"
                    val userName = prefs.getString("user_name", "User") ?: "User"
                    
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        try {
                                    val userInfo = com.rescuemate.emergency.data.UserInfo(
                                        userId = userId,
                                        name = userName,
                                        age = 0, // Default or fetch from preferences
                                        phoneNumber = "", // Default or fetch from preferences
                                        medicalInfo = com.rescuemate.emergency.data.MedicalInfo(userId = userId) // Default empty medical info
                                    )
                            emergencyManager?.triggerManualEmergency(userId, userInfo)
                        } catch (e: Exception) {
                            Log.e("SOS", "Error", e)
                        }
                    }
                },
                onDismiss = {
                    autoConfirmJob?.cancel()
                    showSOSConfirmation = false
                },
                icon = {
                    RotatingStar(modifier = Modifier.size(48.dp), color = Color(0xFFFF5252))
                }
            )
            
            CosmicOverlay(
                visible = showErrorDialog,
                title = "TRANSMISSION ERROR",
                message = errorMessage ?: "Unknown error occurred.",
                dismissText = "ACKNOWLEDGE",
                onDismiss = {
                    showErrorDialog = false
                    errorMessage = null
                },
                icon = {
                    Text(text = AsciiArt.WARNING, style = AsciiLarge, color = Color(0xFFFF9800))
                }
            )
            
            CosmicToast(
                message = toastMessage,
                onDismiss = { toastMessage = null }
            )
        }
    }
}

@Composable
fun PlanetarySOSButton(
    onClick: () -> Unit,
    onTap: () -> Unit,
    isInAIConversation: Boolean,
    aiConversationMode: String,
    audioLevel: Float,
    onShowConfirmation: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalView.current
    
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    // Pulse animation for the button
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = Modifier
            .size(240.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onPress = {
                        isPressed = true
                        holdProgress = 0f
                        hapticFeedback.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        
                        holdJob = scope.launch {
                            delay(300) // Wait for tap distinction
                            val duration = 1500L
                            val startTime = System.currentTimeMillis()
                            while (System.currentTimeMillis() - startTime < duration) {
                                holdProgress = (System.currentTimeMillis() - startTime) / duration.toFloat()
                                delay(16)
                            }
                            holdProgress = 1f
                            hapticFeedback.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            withContext(Dispatchers.Main) {
                                onShowConfirmation()
                            }
                        }
                        
                        tryAwaitRelease()
                        isPressed = false
                        holdJob?.cancel()
                        holdProgress = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer Pulse Ring (when idle)
        if (!isPressed && !isInAIConversation) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .border(1.dp, CosmicPrimary.copy(alpha = 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulseScale)
                    .border(1.dp, CosmicPrimary.copy(alpha = 0.5f), CircleShape)
            )
        }

        // Main Button Circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (holdProgress > 0) Color(0xFFFF5252) else CosmicPrimary,
                            if (holdProgress > 0) Color(0xFFD32F2F) else CosmicPrimaryDark
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (holdProgress > 0) Color.White else CosmicPrimaryLight,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isInAIConversation) {
                 // AI Listening Visualizer
                 Icon(
                     imageVector = Icons.Default.GraphicEq,
                     contentDescription = "AI Active",
                     tint = Color.White,
                     modifier = Modifier.size(48.dp)
                 )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SOS",
                        style = AsciiLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "HOLD",
                        style = AsciiSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        // Progress Ring
        if (holdProgress > 0) {
            CircularProgressIndicator(
                progress = holdProgress,
                modifier = Modifier.size(170.dp),
                color = Color.White,
                strokeWidth = 4.dp
            )
        }
    }
}

@Composable
fun EmergencyStatusCard(
    emergency: com.rescuemate.emergency.data.EmergencyEvent,
    onConfirmSafe: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFFF5252)), RoundedCornerShape(4.dp))
            .background(Color(0xFFFF5252).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "EMERGENCY PROTOCOL",
                    style = AsciiMedium,
                    color = Color(0xFFFF5252)
                )
                Text(
                    text = "PHASE ${emergency.currentPhase}",
                    style = AsciiSmall,
                    color = Color(0xFFFF5252)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = emergency.healthData.alertReason.uppercase(),
                style = AsciiSmall,
                color = CosmicTextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            CosmicButton(
                text = "CONFIRM SAFETY",
                onClick = onConfirmSafe,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MonitoringStatusCard(
    isActive: Boolean,
    currentHeartRate: Int?,
    lastHealthCheck: Long?,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(4.dp))
            .background(CosmicCard, RoundedCornerShape(4.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isActive) "SYSTEM ACTIVE" else "SYSTEM OFFLINE",
                    style = AsciiMedium,
                    color = if (isActive) Color(0xFF4CAF50) else CosmicTextSecondary
                )
                if (isActive) {
                    Text(text = "[ON]", style = AsciiSmall, color = Color(0xFF4CAF50))
                }
            }
            
            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HEART_RATE", style = AsciiSmall, color = CosmicTextSecondary)
                    Text(
                        text = if (currentHeartRate != null) "$currentHeartRate BPM" else "SEARCHING...",
                        style = AsciiSmall,
                        color = CosmicTextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            CosmicButton(
                text = if (isActive) "DEACTIVATE" else "ACTIVATE",
                onClick = if (isActive) onStopMonitoring else onStartMonitoring,
                isPrimary = !isActive,
                modifier = Modifier.fillMaxWidth()
            )
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
    Box(
        modifier = modifier
            .height(80.dp)
            .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(8.dp))
            .background(CosmicCard, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = CosmicTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text.uppercase(),
                style = AsciiSmall.copy(fontSize = 10.sp),
                color = CosmicTextPrimary
            )
        }
    }
}

// Helper functions
private fun checkServiceRunning(context: Context): Boolean {
    val prefs = context.getSharedPreferences(EmergencyConstants.PREF_NAME_EMERGENCY, Context.MODE_PRIVATE)
    return prefs.getBoolean("service_running", false)
}

private fun checkMonitoringPermissions(context: Context): Boolean {
    val requiredPermissions = mutableListOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.FOREGROUND_SERVICE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    return requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

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
        llmApiKey?.let { putExtra(EmergencyBackgroundService.EXTRA_OPENAI_API_KEY, it) }
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopMonitoringService(context: Context) {
    val intent = Intent(context, EmergencyBackgroundService::class.java).apply {
        action = EmergencyBackgroundService.ACTION_STOP_MONITORING
    }
    context.stopService(intent)
}

// Duplicated helper for startVoiceConversation to keep file self-contained or if not imported
private fun startVoiceConversation(
    context: Context,
    conversationalService: ElevenLabsConversationalService,
    onSuccess: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onMessage: (String, String) -> Unit,
    onError: (String) -> Unit,
    onDisconnect: () -> Unit,
    onCanSendFeedback: (Boolean) -> Unit,
    onAudioLevelChange: (Float) -> Unit
) {
    val prefs = context.getSharedPreferences("voice_ai_prefs", Context.MODE_PRIVATE)
    val agentId = prefs.getString("agent_id", com.rescuemate.BuildConfig.ELEVEN_AGENT_ID) 
        ?: com.rescuemate.BuildConfig.ELEVEN_AGENT_ID
    val voiceId = prefs.getString("selected_voice_id", null)
    
    conversationalService.startConversation(
        agentId = agentId,
        voiceId = voiceId,
        callbacks = object : ElevenLabsConversationalService.ConversationCallbacks {
            override fun onConnect(conversationId: String) { onSuccess(conversationId) }
            override fun onModeChange(mode: String) { onModeChange(mode) }
            override fun onStatusChange(status: String) { onStatusChange(status) }
            override fun onMessage(source: String, messageJson: String) { onMessage(source, messageJson) }
            override fun onError(error: String) { onError(error) }
            override fun onDisconnect() { onDisconnect() }
            override fun onCanSendFeedback(canSend: Boolean) { onCanSendFeedback(canSend) }
            override fun onAudioLevelChange(level: Float) { onAudioLevelChange(level) }
        }
    )
}
