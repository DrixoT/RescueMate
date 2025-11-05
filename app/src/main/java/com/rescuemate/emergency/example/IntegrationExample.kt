package com.rescuemate.emergency.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rescuemate.emergency.ui.*

/**
 * EXAMPLE: How to integrate Emergency SOS into your existing app
 *
 * Simply copy this code into your existing navigation setup
 */

@Composable
fun EmergencyIntegrationExample() {
    val navController = rememberNavController()

    // Replace these with actual user data
    val userId = "current-user-id"
    val userName = "John Doe"
    val userAge = 30
    val userPhone = "+1234567890"

    NavHost(navController, startDestination = "home") {

        // Your existing home screen
        composable("home") {
            HomeScreenWithEmergency(
                onEmergencyClick = { navController.navigate("emergency_panic") },
                onContactsClick = { navController.navigate("emergency_contacts") },
                onConfigClick = { navController.navigate("emergency_config") }
            )
        }

        // Emergency Panic Button
        composable("emergency_panic") {
            EmergencyPanicButton(
                userId = userId,
                userName = userName,
                userAge = userAge,
                userPhone = userPhone,
                onEmergencyTriggered = {
                    // Handle emergency triggered
                    navController.navigate("emergency_active")
                }
            )
        }

        // Emergency Contacts Management
        composable("emergency_contacts") {
            EmergencyContactManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddContact = { navController.navigate("add_contact") }
            )
        }

        // Add Emergency Contact
        composable("add_contact") {
            AddEmergencyContactScreen(
                onNavigateBack = { navController.popBackStack() },
                onContactAdded = {
                    navController.popBackStack()
                    // Show success message
                }
            )
        }

        // Emergency Configuration
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

/**
 * Example: Home screen with Emergency SOS integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithEmergency(
    onEmergencyClick: () -> Unit,
    onContactsClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RescueMate") },
                actions = {
                    IconButton(onClick = onConfigClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            // Large Emergency Button
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
            // Emergency Status Card (shows if emergency is active)
            EmergencyStatusCard()

            // Emergency Contacts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContactsClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        Icon(Icons.Default.Person, "Contacts")
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Emergency Contacts")
                    }
                    Icon(Icons.Default.KeyboardArrowRight, "Go")
                }
            }

            // Your other home screen content...
        }
    }
}

/**
 * INTEGRATION INSTRUCTIONS:
 *
 * 1. Copy the emergency routes from EmergencyIntegrationExample to your existing NavHost
 * 2. Add the emergency FAB to your home screen
 * 3. Add EmergencyStatusCard() to show active emergencies
 * 4. Add navigation to emergency_contacts in your menu/settings
 * 5. Done! Emergency SOS is now integrated.
 *
 * MINIMAL INTEGRATION (3 lines):
 *
 * In your MainActivity or composable:
 * ```kotlin
 * FloatingActionButton(onClick = { navController.navigate("emergency_panic") }) {
 *     Icon(Icons.Default.Warning, "Emergency", tint = Color.White)
 * }
 * ```
 *
 * That's it! The emergency system will handle the rest.
 */

