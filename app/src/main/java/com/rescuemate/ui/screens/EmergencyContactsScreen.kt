package com.rescuemate.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.ui.theme.*


@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    onAddContact: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { EmergencyRepository(context) }

    // Use mutableStateOf instead of mutableStateListOf for better reactivity
    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load contacts when screen appears
    LaunchedEffect(Unit) {
        Log.d("EmergencyContactsScreen", "🎨 Screen loaded - Loading contacts from database")
        isLoading = true
        contacts = repository.getAllContacts()
        isLoading = false
        Log.d("EmergencyContactsScreen", "✅ Loaded ${contacts.size} contacts from database")
    }

    // Refresh contacts when returning from add contact screen
    DisposableEffect(Unit) {
        onDispose {
            Log.d("EmergencyContactsScreen", "🔄 Screen disposing")
        }
    }

    com.rescuemate.ui.components.CosmicScaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CosmicTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    com.rescuemate.ui.components.CosmicHeader(
                        text = stringResource(R.string.emergency_contacts)
                    )
                    com.rescuemate.ui.components.CosmicSubHeader(
                        text = stringResource(R.string.your_safety_network)
                    )
                }
                IconButton(onClick = onAddContact) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Contact",
                        tint = CosmicPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            // Show loading, empty state or contacts list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CosmicPrimary)
                    }
                }
                contacts.isEmpty() -> {
                    EmptyContactsState(onAddContact = onAddContact)
                }
                else -> {
                    Log.d("EmergencyContactsScreen", "📋 Displaying ${contacts.size} contacts")

                    // Contacts List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(contacts) { index, contact ->
                            ContactCard(
                                contact = contact,
                                modifier = Modifier.fillMaxWidth(),
                                onDelete = {
                                    Log.d("EmergencyContactsScreen", "🗑️ Delete requested for: ${contact.name}")
                                    val success = repository.deleteContact(contact.id)
                                    if (success) {
                                        // Refresh list
                                        contacts = repository.getAllContacts()
                                        Log.d("EmergencyContactsScreen", "✅ Contact deleted, list refreshed")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Note
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
                                text = stringResource(R.string.auto_alert),
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextSecondary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.auto_alert_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyContactsState(
    onAddContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Empty state icon
        Icon(
            imageVector = Icons.Default.PersonAddAlt,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = CosmicTextSecondary.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "No Emergency Contacts",
            style = MaterialTheme.typography.headlineSmall,
            color = CosmicTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add trusted contacts who will be notified\nin case of an emergency",
            style = MaterialTheme.typography.bodyLarge,
            color = CosmicTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        com.rescuemate.ui.components.CosmicButton(
            text = "Add Your First Contact",
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
            isPrimary = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CosmicCardHover.copy(alpha = 0.5f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = CosmicPrimary
                )
                Column {
                    Text(
                        text = "Emergency Services (911)",
                        style = MaterialTheme.typography.titleSmall,
                        color = CosmicTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "911 is always available as your default emergency contact",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContactCard(
    contact: EmergencyContact,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = CosmicTextPrimary
                        )
                        if (contact.isPrimaryContact) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Primary Contact",
                                modifier = Modifier.size(14.dp),
                                tint = CosmicPrimary
                            )
                        }
                    }
                    Text(
                        text = contact.relationship,
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextPrimary.copy(alpha = 0.7f)
                    )
                    if (contact.email != null) {
                        Text(
                            text = contact.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTextPrimary.copy(alpha = 0.7f)
                        )
                    }
                }

                // Delete button
                IconButton(
                    onClick = {
                        showDeleteDialog = true
                        Log.d("ContactCard", "🗑️ Delete icon clicked for: ${contact.name}")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Log.d("ContactCard", "📞 Call button clicked for: ${contact.phoneNumber}")
                        /* Handle call */
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.call),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                OutlinedButton(
                    onClick = {
                        Log.d("ContactCard", "💬 Message button clicked for: ${contact.phoneNumber}")
                        /* Handle message */
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CosmicTextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.message),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${contact.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d("ContactCard", "✅ Delete confirmed for: ${contact.name}")
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    Log.d("ContactCard", "❌ Delete cancelled")
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

