package com.rescuemate.emergency.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rescuemate.emergency.ui.*

/**
 * UPDATED COMPLETE INTEGRATION
 * All Emergency SOS features with validation and proper data handling
 */

@Composable
fun UpdatedCompleteEmergencyIntegration() {
    val navController = rememberNavController()

    val userId = "current-user-id"
    val userName = "John Doe"
    val userAge = 30
    val userPhone = "+1234567890"

    NavHost(navController, startDestination = "home") {

        composable("home") {
            UpdatedHomeScreen(
                onProfileClick = { navController.navigate("user_profile") },
                onEmergencyClick = { navController.navigate("emergency_panic") },
                onContactsClick = { navController.navigate("emergency_contacts") },
                onConfigClick = { navController.navigate("emergency_config") }
            )
        }

        composable("user_profile") {
            UserProfileSetupScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onProfileComplete = { navController.popBackStack() }
            )
        }

        composable("medical_profile") {
            UserMedicalProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onProfileSaved = { navController.popBackStack() }
            )
        }

        composable("emergency_panic") {
            EmergencyPanicButton(
                userId = userId,
                userName = userName,
                userAge = userAge,
                userPhone = userPhone,
                onEmergencyTriggered = { navController.popBackStack() }
            )
        }

        composable("emergency_contacts") {
            EmergencyContactManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddContact = { navController.navigate("add_contact") }
            )
        }

        composable("add_contact") {
            AddEmergencyContactScreen(
                onNavigateBack = { navController.popBackStack() },
                onContactAdded = { navController.popBackStack() }
            )
        }

        composable("emergency_config") {
            EmergencyConfigurationScreen(
                userId = userId,
                userName = userName,
                userAge = userAge,
                userPhone = userPhone,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedHomeScreen(
    onProfileClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onContactsClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RescueMate") },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, "Profile")
                    }
                    IconButton(onClick = onConfigClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEmergencyClick,
                containerColor = Color.Red,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency SOS",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmergencyStatusCard()

            Text("Setup", style = MaterialTheme.typography.titleMedium)

            // Profile
            MenuCard(
                icon = Icons.Default.Person,
                title = "My Profile",
                onClick = onProfileClick
            )

            // Contacts
            MenuCard(
                icon = Icons.Default.Contacts,
                title = "Emergency Contacts",
                onClick = onContactsClick
            )

            // Configuration
            MenuCard(
                icon = Icons.Default.Settings,
                title = "Configuration",
                onClick = onConfigClick
            )
        }
    }
}

@Composable
fun MenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, title)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title)
            }
            Icon(Icons.Default.KeyboardArrowRight, "Go")
        }
    }
}

