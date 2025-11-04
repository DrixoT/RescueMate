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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var selectedVoice by remember { mutableStateOf<Voice?>(null) }
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
                                text = "AI-Powered Emergency Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose a voice that will guide you through emergencies with real-time AI assistance",
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

                                scope.launch {
                                    val result = voiceService.previewVoice(voice.id)
                                    isLoading = false

                                    if (result.isSuccess) {
                                        isPlaying = true
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                        playingVoiceId = null
                                    }
                                }
                            }
                        }
                    )
                }

                // Action Button
                Button(
                    onClick = onComplete,
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

