package com.rescuemate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Voice Conversation Panel that slides up from the bottom
 * Displays conversation status and provides text input for the user
 */
@Composable
fun VoiceConversationPanel(
    isVisible: Boolean,
    conversationStatus: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 400)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top edge indicator (for swipe down gesture visual)
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status indicator at the top
                StatusIndicator(conversationStatus)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Text input field at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = {
                            Text(
                                text = "Type your message...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE91E63),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color(0xFFE91E63)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Send button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (textInput.isNotBlank()) {
                                    Color(0xFFE91E63)
                                } else {
                                    Color.White.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(28.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status indicator showing the current conversation state
 */
@Composable
private fun StatusIndicator(status: String) {
    val (statusText, statusColor) = when (status.lowercase()) {
        "listening" -> "Listening..." to Color(0xFF4CAF50)
        "speaking" -> "AI is speaking..." to Color(0xFFE91E63)
        "connected" -> "Connected" to Color(0xFF2196F3)
        "connecting" -> "Connecting..." to Color(0xFFFFC107)
        "disconnected" -> "Disconnected" to Color(0xFF9E9E9E)
        else -> status to Color.White
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = statusColor,
                    shape = RoundedCornerShape(5.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Status text
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

