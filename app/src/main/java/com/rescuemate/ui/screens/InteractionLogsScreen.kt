package com.rescuemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.emergency.data.InteractionLog
import com.rescuemate.data.repository.FirestoreRepository
import com.rescuemate.ui.theme.CosmicBackground
import com.rescuemate.ui.theme.CosmicCard
import com.rescuemate.ui.theme.CosmicTextPrimary
import com.rescuemate.ui.theme.CosmicTextSecondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionLogsScreen(
    userId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf<List<InteractionLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val firestoreRepo = remember { FirestoreRepository() }

    LaunchedEffect(userId) {
        try {
            val result = firestoreRepo.getInteractionLogs(userId)
            result.fold(
                onSuccess = { 
                    logs = it 
                    isLoading = false
                },
                onFailure = { 
                    error = it.message 
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical History", color = CosmicTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = CosmicTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicBackground
                )
            )
        },
        containerColor = CosmicBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CosmicTextPrimary
                )
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No History found",
                        color = CosmicTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logs) { log ->
                        LogItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: InteractionLog) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    
    // Parse the summary field which may contain "Title: ... | Summary: ..." or similar structure
    // or just be a plain string. We look for the delimiter or structured format.
    // The prompt requests: "Title: [Title]\nSummary: [Summary]"
    
    val (title, summaryDetail) = remember(log.summary) {
        if (log.summary.contains("Title:") && log.summary.contains("Summary:")) {
            val titlePart = log.summary.substringAfter("Title:").substringBefore("Summary:").trim()
            val summaryPart = log.summary.substringAfter("Summary:").trim()
            titlePart to summaryPart
        } else {
            // Fallback for old logs or failure to follow format
            val shortTitle = log.summary.take(50).let { if (it.length == 50) "$it..." else it }
            shortTitle to log.summary
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CosmicCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = CosmicTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title.ifBlank { "Voice Interaction" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextPrimary
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = CosmicTextSecondary
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CosmicTextSecondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summaryDetail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextPrimary.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(
                        text = log.type,
                        color = if (log.type == "ONLINE") Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
