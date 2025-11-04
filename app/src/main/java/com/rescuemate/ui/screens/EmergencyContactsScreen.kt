package com.rescuemate.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rescuemate.R
import com.rescuemate.ui.theme.*

data class Contact(
    val id: Int,
    val name: String,
    val relationship: String,
    val phone: String,
    val isPrimary: Boolean
)

@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit,
    onAddContact: () -> Unit
) {
    val contacts = remember {
        listOf(
            Contact(1, "Sarah Mitchell", "Primary Contact", "+1 (555) 0123", true),
            Contact(2, "Dr. James Chen", "Emergency Physician", "+1 (555) 0456", false),
            Contact(3, "Alex Johnson", "Family", "+1 (555) 0789", false),
            Contact(4, "911 Emergency", "Emergency Services", "911", false)
        )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.emergency_contacts),
                        style = MaterialTheme.typography.titleLarge,
                        color = CosmicTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.your_safety_network),
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
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

            Spacer(modifier = Modifier.height(16.dp))

            // Contacts List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(contacts) { index, contact ->
                    ContactCard(
                        contact = contact,
                        modifier = Modifier.fillMaxWidth()
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

@Composable
fun ContactCard(
    contact: Contact,
    modifier: Modifier = Modifier
) {
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
                        if (contact.isPrimary) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
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
                        text = contact.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTextPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Handle call */ },
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
                    onClick = { /* Handle message */ },
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
}

