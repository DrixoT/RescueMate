package com.rescuemate.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rescuemate.services.ElevenLabsConversationalService
import com.rescuemate.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ConversationState {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    DISCONNECTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessAIConversationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conversationalService = remember { ElevenLabsConversationalService(context) }
    val mockService = remember { com.rescuemate.services.MockConversationService(context) }
    
    // Demo mode toggle (for emulator testing)
    var isDemoMode by remember { mutableStateOf(false) }
    
    // Load agent ID from prefs/BuildConfig
    val prefs = context.getSharedPreferences("voice_ai_prefs", Context.MODE_PRIVATE)
    var agentId by remember { 
        mutableStateOf(
            prefs.getString("manual_agent_id", null) ?: com.rescuemate.BuildConfig.ELEVEN_AGENT_ID
        ) 
    }
    
    var showAgentIdDialog by remember { mutableStateOf(false) }
    var tempAgentId by remember { mutableStateOf("") }
    
    // Check if agent ID is valid
    val isAgentIdValid = agentId.isNotEmpty() && agentId != "YOUR_AGENT_ID_HERE" && agentId != ""
    
    // Permission state
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (!isGranted) {
            android.widget.Toast.makeText(
                context,
                "Microphone permission is required for voice conversation",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    var conversationState by remember { mutableStateOf(ConversationState.IDLE) }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isMuted by remember { mutableStateOf(false) }
    var conversationId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canSendFeedback by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Text input for demo mode
    var textInput by remember { mutableStateOf("") }
    
    // Function to save conversation state to SharedPreferences
    fun saveConversationState(isActive: Boolean, mode: String) {
        val conversationPrefs = context.getSharedPreferences("ai_conversation_state", Context.MODE_PRIVATE)
        conversationPrefs.edit().apply {
            putBoolean("is_active", isActive)
            putString("mode", mode)
            apply()
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            conversationalService.cleanup()
            // Clear conversation state
            saveConversationState(false, "idle")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Wellness Companion",
                            style = MaterialTheme.typography.titleLarge,
                            color = CosmicTextPrimary
                        )
                        Text(
                            text = when (conversationState) {
                                ConversationState.CONNECTING -> "Connecting..."
                                ConversationState.LISTENING -> "AI is listening..."
                                ConversationState.SPEAKING -> "AI is speaking..."
                                ConversationState.DISCONNECTED -> "Disconnected"
                                else -> "Tap to start conversation"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmicTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicCard
                )
            )
        },
        containerColor = CosmicBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Demo Mode Indicator
                if (isDemoMode) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎭",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Demo Mode Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF1565C0),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Using simulated responses for emulator demo. On physical devices, this connects to real ElevenLabs AI.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF424242)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Agent ID Warning (if invalid)
                if (!isAgentIdValid && !isDemoMode) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3CD)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF57C00),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Agent ID Required",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF3E2723),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "To use voice conversation, you need an ElevenLabs Agent ID. Create an agent in your ElevenLabs dashboard, then enter the ID here.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5D4037),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                Button(
                                    onClick = { 
                                        tempAgentId = ""
                                        showAgentIdDialog = true 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF57C00)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enter Agent ID")
                                }
                            }
                        }
                    }
                }
                
                // Error Message
                errorMessage?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5F2120),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                if (messages.isEmpty() && isAgentIdValid) {
                    item {
                        // Welcome Message
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = CosmicPrimary.copy(alpha = 0.1f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = CosmicPrimary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "I'm here to listen",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CosmicTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDemoMode) {
                                        "Demo mode is active. Type a message to see simulated AI responses. Perfect for emulator demos!"
                                    } else {
                                        "Start the conversation by tapping the microphone button below. Share what's on your mind."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
            
            // State Indicator
            if (conversationState != ConversationState.IDLE && conversationState != ConversationState.DISCONNECTED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (conversationState) {
                            ConversationState.SPEAKING -> CosmicPrimary.copy(alpha = 0.1f)
                            ConversationState.LISTENING -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else -> CosmicCard
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (conversationState) {
                            ConversationState.SPEAKING -> CosmicPrimary
                            ConversationState.LISTENING -> Color(0xFF4CAF50)
                            else -> CosmicBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (conversationState == ConversationState.CONNECTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CosmicPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = when (conversationState) {
                                    ConversationState.SPEAKING -> Icons.Default.VolumeUp
                                    ConversationState.LISTENING -> Icons.Default.Mic
                                    else -> Icons.Default.Circle
                                },
                                contentDescription = null,
                                tint = when (conversationState) {
                                    ConversationState.SPEAKING -> CosmicPrimary
                                    ConversationState.LISTENING -> Color(0xFF4CAF50)
                                    else -> CosmicTextSecondary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when (conversationState) {
                                ConversationState.CONNECTING -> "Connecting to AI..."
                                ConversationState.LISTENING -> "AI is listening"
                                ConversationState.SPEAKING -> "AI is speaking"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (conversationState) {
                                ConversationState.SPEAKING -> CosmicPrimary
                                ConversationState.LISTENING -> Color(0xFF4CAF50)
                                else -> CosmicTextSecondary
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Text Input for Demo Mode
            if (isDemoMode && conversationState != ConversationState.IDLE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your message...") },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = CosmicCard,
                            unfocusedContainerColor = CosmicCard,
                            focusedIndicatorColor = CosmicPrimary,
                            unfocusedIndicatorColor = CosmicBorder
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val userMessage = textInput.trim()
                                textInput = ""
                                
                                // Add user message
                                messages = messages + Message(userMessage, isUser = true)
                                
                                // Simulate AI response
                                conversationState = ConversationState.SPEAKING
                                scope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                    
                                    mockService.simulateConversation(
                                        userInput = userMessage,
                                        onThinking = {
                                            // AI is thinking
                                        },
                                        onResponse = { response ->
                                            messages = messages + Message(response, isUser = false)
                                            conversationState = ConversationState.LISTENING
                                            scope.launch {
                                                listState.animateScrollToItem(messages.size - 1)
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        enabled = textInput.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = if (textInput.isNotBlank()) CosmicPrimary else CosmicTextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Conversation Button
                Button(
                    onClick = {
                        errorMessage = null
                        
                        if (isDemoMode) {
                            // Demo Mode: Start mock conversation
                            conversationState = ConversationState.CONNECTING
                            mockService.startSession()
                            
                            scope.launch {
                                delay(1000)
                                conversationState = ConversationState.LISTENING
                                saveConversationState(true, "listening")
                                android.widget.Toast.makeText(
                                    context,
                                    "Demo mode ready! Type a message to see simulated responses",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Real Mode: Validate and start real conversation
                            if (!isAgentIdValid) {
                                errorMessage = "Please enter a valid Agent ID"
                                return@Button
                            }
                            
                            if (!hasRecordPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@Button
                            }
                            
                            conversationState = ConversationState.CONNECTING
                            
                            // Load selected voice from SharedPreferences
                            val voicePrefs = context.getSharedPreferences("voice_ai_prefs", Context.MODE_PRIVATE)
                            val selectedVoiceId = voicePrefs.getString("selected_voice_id", null)
                            
                            conversationalService.startConversation(
                            agentId = agentId,
                            voiceId = selectedVoiceId,
                            callbacks = object : ElevenLabsConversationalService.ConversationCallbacks {
                                override fun onConnect(conversationId: String) {
                                    this@WellnessAIConversationScreen.conversationId = conversationId
                                    conversationState = ConversationState.LISTENING
                                    saveConversationState(true, "listening")
                                    android.widget.Toast.makeText(
                                        context,
                                        "Connected! Start speaking",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                
                                override fun onModeChange(mode: String) {
                                    conversationState = when (mode.lowercase()) {
                                        "speaking" -> {
                                            saveConversationState(true, "speaking")
                                            ConversationState.SPEAKING
                                        }
                                        "listening" -> {
                                            saveConversationState(true, "listening")
                                            ConversationState.LISTENING
                                        }
                                        else -> conversationState
                                    }
                                }
                                
                                override fun onStatusChange(status: String) {
                                    android.util.Log.d("WellnessAI", "Status: $status")
                                }
                                
                                override fun onMessage(source: String, messageJson: String) {
                                    try {
                                        val json = JSONObject(messageJson)
                                        val text = json.optString("text", json.optString("message", messageJson))
                                        val isUser = source.lowercase() == "user"
                                        
                                        messages = messages + Message(text, isUser)
                                        
                                        scope.launch {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("WellnessAI", "Error parsing message", e)
                                    }
                                }
                                
                                override fun onError(error: String) {
                                    errorMessage = error
                                    conversationState = ConversationState.DISCONNECTED
                                    saveConversationState(false, "idle")
                                }
                                
                                override fun onDisconnect() {
                                    conversationState = ConversationState.DISCONNECTED
                                    saveConversationState(false, "idle")
                                }
                                
                                override fun onCanSendFeedback(canSend: Boolean) {
                                    canSendFeedback = canSend
                                }
                            }
                        )
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = conversationState == ConversationState.IDLE || conversationState == ConversationState.DISCONNECTED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (conversationState == ConversationState.DISCONNECTED) "Reconnect" else "Start Conversation",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                // Mute/Unmute Button
                if (conversationState != ConversationState.IDLE && conversationState != ConversationState.DISCONNECTED) {
                    IconButton(
                        onClick = {
                            isMuted = conversationalService.toggleMute()
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) Color(0xFFEF5350) else CosmicPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Demo Mode Toggle
                IconButton(
                    onClick = {
                        isDemoMode = !isDemoMode
                        // Clear state when switching modes
                        if (conversationalService.isActive()) {
                            conversationalService.endConversation()
                        }
                        if (mockService.isSessionActive()) {
                            mockService.endSession()
                        }
                        conversationState = ConversationState.IDLE
                        messages = listOf()
                        errorMessage = null
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isDemoMode) Icons.Default.PhoneEnabled else Icons.Default.PhonePaused,
                        contentDescription = if (isDemoMode) "Switch to Real Mode" else "Switch to Demo Mode",
                        tint = if (isDemoMode) Color(0xFF2196F3) else CosmicTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // End Conversation Button
                OutlinedButton(
                    onClick = {
                        if (isDemoMode) {
                            if (mockService.isSessionActive()) {
                                mockService.endSession()
                                conversationState = ConversationState.DISCONNECTED
                                saveConversationState(false, "idle")
                            }
                        } else {
                            if (conversationalService.isActive()) {
                                conversationalService.endConversation()
                                conversationState = ConversationState.DISCONNECTED
                                saveConversationState(false, "idle")
                            }
                        }
                        onBack()
                    },
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CosmicTextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    // Agent ID Input Dialog
    if (showAgentIdDialog) {
        AlertDialog(
            onDismissRequest = { showAgentIdDialog = false },
            title = {
                Text(
                    text = "Enter ElevenLabs Agent ID",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your ElevenLabs Agent ID to enable voice conversation:",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary
                    )
                    OutlinedTextField(
                        value = tempAgentId,
                        onValueChange = { tempAgentId = it },
                        label = { Text("Agent ID") },
                        placeholder = { Text("your_agent_id_here") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPrimary,
                            focusedLabelColor = CosmicPrimary
                        )
                    )
                    Text(
                        text = "Create an agent in your ElevenLabs dashboard to get an Agent ID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempAgentId.isNotEmpty()) {
                            prefs.edit().putString("manual_agent_id", tempAgentId).apply()
                            agentId = tempAgentId
                            showAgentIdDialog = false
                            errorMessage = null
                            android.widget.Toast.makeText(
                                context,
                                "Agent ID saved successfully",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = tempAgentId.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAgentIdDialog = false }) {
                    Text("Cancel", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicCard,
            titleContentColor = CosmicTextPrimary,
            textContentColor = CosmicTextPrimary
        )
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) CosmicPrimary else CosmicCard
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else CosmicTextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

