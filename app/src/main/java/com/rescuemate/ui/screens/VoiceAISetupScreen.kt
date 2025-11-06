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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.viewbinding.BuildConfig
import com.rescuemate.services.ElevenLabsVoiceService
import com.rescuemate.ui.theme.*
import kotlinx.coroutines.launch

data class Voice(
    val id: String,
    val name: String,
    val description: String,
    val gender: String,
    val accent: String
)

// ElevenLabs voices - Actual voice IDs from your account
val availableVoices = listOf(
    Voice(
        id = "scOwDtmlUjD3prqpp97I",
        name = "Sam",
        description = "Professional and clear voice for emergency guidance",
        gender = "Male",
        accent = "American"
    ),
    Voice(
        id = "ChO6kqkVouUn0s7HMunx",
        name = "Pete",
        description = "Calm and reassuring voice for crisis situations",
        gender = "Male",
        accent = "American"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAISetupScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceService = remember { ElevenLabsVoiceService(context) }
    val prefs = context.getSharedPreferences("voice_ai_prefs", Context.MODE_PRIVATE)

    // Load API key from .env or SharedPreferences fallback
    var apiKey by remember { 
        mutableStateOf(
            prefs.getString("manual_api_key", null) ?: com.rescuemate.BuildConfig.ELEVEN_API_KEY
        ) 
    }
    
    // Check if API key is valid
    val isApiKeyValid = apiKey.isNotEmpty() && apiKey != "YOUR_API_KEY_HERE" && apiKey != ""

    // Set API key immediately
    LaunchedEffect(apiKey) {
        if (isApiKeyValid) {
            voiceService.setApiKey(apiKey)
            android.util.Log.d("VoiceAISetup", "API Key set: ${apiKey.take(10)}...")
        }
    }

    var selectedVoice by remember { 
        mutableStateOf<Voice?>(
            prefs.getString("selected_voice_id", null)?.let { savedId ->
                availableVoices.find { it.id == savedId }
            }
        ) 
    }
    var isPlaying by remember { mutableStateOf(false) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var wakeWordEnabled by remember { mutableStateOf(true) }
    var customWakeWord by remember { mutableStateOf("Hey RescueMate") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceService.cleanup()
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
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CosmicCard.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CosmicTextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice AI Setup",
                            style = MaterialTheme.typography.titleLarge,
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure emergency voice assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicTextSecondary,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // Hero Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicPrimary.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = CosmicPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                text = "AI-Powered Conversational Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This app uses ElevenLabs Conversational AI for real-time voice interactions. Create an agent in your ElevenLabs dashboard to customize voice, personality, and knowledge base.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Wake Word Settings
                Text(
                    text = "Wake Word",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Voice Activation",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CosmicTextPrimary
                                )
                                Text(
                                    text = "Activate assistant with wake word",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary
                                )
                            }
                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = { wakeWordEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CosmicPrimary,
                                    checkedTrackColor = CosmicPrimary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (wakeWordEnabled) {
                            OutlinedTextField(
                                value = customWakeWord,
                                onValueChange = { customWakeWord = it },
                                label = { Text("Custom Wake Word") },
                                placeholder = { Text("e.g., Hey RescueMate") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicCard,
                                    unfocusedContainerColor = CosmicCard,
                                    focusedIndicatorColor = CosmicPrimary,
                                    unfocusedIndicatorColor = CosmicBorder
                                )
                            )
                        }
                    }
                }

                // Voice Selection
                Text(
                    text = "Select Voice",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Powered by ElevenLabs AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextSecondary,
                    letterSpacing = 1.sp
                )

                // Voice Cards with preview functionality
                availableVoices.forEach { voice ->
                    VoiceCard(
                        voice = voice,
                        isSelected = selectedVoice?.id == voice.id,
                        isPlaying = playingVoiceId == voice.id && isPlaying,
                        onSelect = { selectedVoice = voice },
                        onPlay = {
                            if (playingVoiceId == voice.id && isPlaying) {
                                // Stop playing
                                voiceService.stopAudio()
                                isPlaying = false
                                playingVoiceId = null
                            } else {
                                // Start playing preview
                                isLoading = true
                                errorMessage = null
                                playingVoiceId = voice.id

                                android.util.Log.d("VoiceAISetup", "Starting voice preview for: ${voice.name}")

                                scope.launch {
                                    try {
                                        // Generate audio from text
                                        val testMessage = "Hey! I'm ${voice.name}, I'm glad I could be of service. How can I help?"
                                        android.util.Log.d("VoiceAISetup", "Generating audio...")

                                        val audioResult = voiceService.textToSpeech(
                                            text = testMessage,
                                            voiceId = voice.id,
                                            useCache = true
                                        )

                                        if (audioResult.isSuccess) {
                                            val audioPath = audioResult.getOrNull()
                                            android.util.Log.d("VoiceAISetup", "Audio generated: $audioPath")

                                            if (audioPath != null) {
                                                // Play the audio
                                                val playResult = voiceService.playAudio(audioPath)

                                                if (playResult.isSuccess) {
                                                    isPlaying = true
                                                    android.util.Log.d("VoiceAISetup", "Playing audio successfully")
                                                } else {
                                                    errorMessage = "Failed to play audio: ${playResult.exceptionOrNull()?.message}"
                                                    android.util.Log.e("VoiceAISetup", errorMessage!!)
                                                    playingVoiceId = null
                                                }
                                            }
                                        } else {
                                            val error = audioResult.exceptionOrNull()?.message ?: "Unknown error"
                                            errorMessage = "Failed to generate audio: $error"
                                            android.util.Log.e("VoiceAISetup", errorMessage!!)
                                            playingVoiceId = null
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                        android.util.Log.e("VoiceAISetup", "Exception during voice preview", e)
                                        playingVoiceId = null
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    )
                }

                // Action Button
                Button(
                    onClick = {
                        // Save selected voice to SharedPreferences
                        selectedVoice?.let { voice ->
                            prefs.edit().apply {
                                putString("selected_voice_id", voice.id)
                                putString("selected_voice_name", voice.name)
                                apply()
                            }
                        }
                        onComplete()
                    },
                    enabled = selectedVoice != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary,
                        disabledContainerColor = CosmicBorder
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete Setup",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCard(
    voice: Voice,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CosmicPrimary.copy(alpha = 0.15f) else CosmicCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CosmicPrimary else CosmicBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (isSelected) CosmicPrimary.copy(alpha = 0.2f) else CosmicCardHover
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (voice.gender == "Female") Icons.Default.Person
                                     else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isSelected) CosmicPrimary else CosmicTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Voice Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = voice.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = CosmicPrimary.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = voice.gender,
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmicPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = voice.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmicTextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${voice.accent} accent",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextSecondary.copy(alpha = 0.7f)
                )
            }

            // Play Button
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = CosmicPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Selection Indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = CosmicPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

