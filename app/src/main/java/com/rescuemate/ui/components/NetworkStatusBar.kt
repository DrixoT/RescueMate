package com.rescuemate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Network Status Bar Component
 * Shows connection status and offline queue information
 */
@Composable
fun NetworkStatusBar(
    isConnected: Boolean,
    pendingOperationsCount: Int = 0,
    onSyncClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isConnected || pendingOperationsCount > 0,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = if (isConnected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            },
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CloudQueue else Icons.Default.WifiOff,
                        contentDescription = if (isConnected) "Online with pending" else "Offline",
                        tint = if (isConnected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onError
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = if (isConnected) {
                                if (pendingOperationsCount > 0) "Syncing..." else "Connected"
                            } else {
                                "No Internet Connection"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) {
                                    MaterialTheme.colorScheme.onTertiary
                                } else {
                                    MaterialTheme.colorScheme.onError
                                }
                            )
                        )
                        
                        if (pendingOperationsCount > 0) {
                            Text(
                                text = "$pendingOperationsCount operation${if (pendingOperationsCount > 1) "s" else ""} pending",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isConnected) {
                                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onError.copy(alpha = 0.8f)
                                    }
                                )
                            )
                        }
                    }
                }
                
                if (pendingOperationsCount > 0 && onSyncClick != null) {
                    FilledTonalButton(
                        onClick = onSyncClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isConnected) {
                                MaterialTheme.colorScheme.onTertiary
                            } else {
                                MaterialTheme.colorScheme.onError
                            },
                            contentColor = if (isConnected) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync now",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Compact Network Status Indicator (for smaller displays)
 */
@Composable
fun CompactNetworkStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isConnected) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

/**
 * Offline Queue Status Card
 */
@Composable
fun OfflineQueueCard(
    pendingEmergencies: Int,
    pendingMessages: Int,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pendingEmergencies + pendingMessages > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Queued items",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Offline Queue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                
                Text(
                    text = buildString {
                        if (pendingEmergencies > 0) {
                            append("$pendingEmergencies emergency event${if (pendingEmergencies > 1) "s" else ""}")
                        }
                        if (pendingMessages > 0) {
                            if (pendingEmergencies > 0) append("\n")
                            append("$pendingMessages message${if (pendingMessages > 1) "s" else ""}")
                        }
                        append(" waiting to sync")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Sync")
                }
            }
        }
    }
}

