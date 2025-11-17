package com.rescuemate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable Empty State Component
 * Shows when lists or content areas are empty
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

/**
 * Empty Contacts State
 */
@Composable
fun EmptyContactsState(
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.PersonAdd,
        title = "No Emergency Contacts",
        description = "Add emergency contacts to be notified during emergencies",
        actionText = "Add Contact",
        onActionClick = onAddContact,
        modifier = modifier
    )
}

/**
 * Empty Emergency Events State
 */
@Composable
fun EmptyEmergencyEventsState(modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = Icons.Default.CheckCircle,
        title = "No Emergency Events",
        description = "You haven't had any emergency events yet. Stay safe!",
        modifier = modifier
    )
}

/**
 * Empty Medical Conditions State
 */
@Composable
fun EmptyMedicalConditionsState(
    onAddCondition: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.MedicalServices,
        title = "No Medical Conditions",
        description = "Add any medical conditions to help emergency responders",
        actionText = "Add Condition",
        onActionClick = onAddCondition,
        modifier = modifier
    )
}

/**
 * Empty Medications State
 */
@Composable
fun EmptyMedicationsState(
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.LocalPharmacy,
        title = "No Medications",
        description = "Add your current medications for emergency reference",
        actionText = "Add Medication",
        onActionClick = onAddMedication,
        modifier = modifier
    )
}

/**
 * Generic Loading Indicator
 */
@Composable
fun LoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

