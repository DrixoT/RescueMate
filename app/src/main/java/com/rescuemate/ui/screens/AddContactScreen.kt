package com.rescuemate.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.data.repository.EmergencyRepository
import com.rescuemate.emergency.data.EmergencyContact
import com.rescuemate.ui.theme.*

@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    initialName: String = "",
    initialPhone: String = ""
) {
    val context = LocalContext.current
    val repository = remember { EmergencyRepository(context) }

    var isPrimary by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initialName) }
    var relationship by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf("") }
    var notificationPref by remember { mutableStateOf(EmergencyContact.NotificationPreference.ALL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Log.d("AddContactScreen", "🎨 Screen rendered - isPrimary: $isPrimary")

    // Function to save contact
    fun saveContact() {
        Log.d("AddContactScreen", "💾 SAVE BUTTON CLICKED - Starting validation and save process")
        Log.d(
            "AddContactScreen",
            "📝 Input values: name='$name', phone='$phone', relationship='$relationship', email='$email', isPrimary=$isPrimary"
        )

        // Validate inputs
        val validation = repository.validateContact(name, phone, relationship)
        if (!validation.isValid) {
            Log.w("AddContactScreen", "❌ Validation failed: ${validation.message}")
            errorMessage = validation.message
            Toast.makeText(context, "❌ ${validation.message}", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("AddContactScreen", "✅ Validation passed - Creating contact object")

        // Validate email if provided
        if (email.isNotBlank()) {
            val emailValidation = repository.validateEmail(email)
            if (!emailValidation.isValid) {
                Log.w("AddContactScreen", "❌ Email validation failed: ${emailValidation.message}")
                errorMessage = emailValidation.message
                Toast.makeText(context, "❌ ${emailValidation.message}", Toast.LENGTH_LONG).show()
                return
            }
        }

        isLoading = true
        errorMessage = null

        // Create contact object
        val contact = EmergencyContact(
            name = name.trim(),
            phoneNumber = phone.trim(),
            relationship = relationship,
            email = email.trim().ifBlank { null },
            isPrimaryContact = isPrimary,
            priority = if (isPrimary) 1 else 2,
            notificationPreference = notificationPref
        )

        Log.d("AddContactScreen", "📞 Contact object created: $contact")
        Log.d("AddContactScreen", "💾 Calling repository.addContact()...")

        // Save to database
        val success = repository.addContact(contact)

        isLoading = false

        if (success) {
            Log.d("AddContactScreen", "✅ Contact saved successfully to database!")
            Toast.makeText(context, "✅ Contact saved: ${contact.name}", Toast.LENGTH_SHORT).show()

            // Navigate back after successful save
            Log.d("AddContactScreen", "🧭 Navigating back to contacts list")
            onSave()
        } else {
            Log.e("AddContactScreen", "❌ Failed to save contact to database")
            errorMessage = "Failed to save contact. Please try again."
            Toast.makeText(context, "❌ Failed to save contact", Toast.LENGTH_LONG).show()
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
                        text = "Add Emergency Contact"
                    )
                    com.rescuemate.ui.components.CosmicSubHeader(
                        text = "Expand Your Safety Network"
                    )
                }
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = CosmicPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

                // Primary Contact Toggle
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CosmicCard
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isPrimary) CosmicPrimary else CosmicTextSecondary
                            )
                            Column {
                                Text(
                                    text = "Primary Contact",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CosmicTextPrimary
                                )
                                Text(
                                    text = "Auto-notified during emergencies",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CosmicTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isPrimary,
                            onCheckedChange = { isPrimary = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CosmicPrimary
                            )
                        )
                    }
                }

                // Contact Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Contact Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTextSecondary,
                        letterSpacing = 2.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                            Log.d("AddContactScreen", "📝 Name input changed: '$it'")
                        },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        isError = errorMessage?.contains("name", ignoreCase = true) == true
                    )

                    // Relationship Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = relationship,
                            onValueChange = {},
                            label = { Text("Relationship *") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors(),
                            readOnly = true,
                            isError = errorMessage?.contains("relationship", ignoreCase = true) == true,
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf(
                                "Family Member",
                                "Friend",
                                "Spouse/Partner",
                                "Doctor",
                                "Colleague",
                                "Neighbor",
                                "Other"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        relationship = option
                                        expanded = false
                                        errorMessage = null
                                        Log.d(
                                            "AddContactScreen",
                                            "📝 Relationship selected: '$option'"
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Phone Number (REQUIRED)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            errorMessage = null
                            Log.d("AddContactScreen", "📝 Phone input changed: '$it'")
                        },
                        label = { Text("Phone Number *") },
                        placeholder = { Text("1234567890") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = CosmicPrimary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        isError = errorMessage?.contains("phone", ignoreCase = true) == true
                    )

                    // Email (OPTIONAL)
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                            Log.d("AddContactScreen", "📝 Email input changed: '$it'")
                        },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("contact@example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CosmicTextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        singleLine = true,
                        isError = errorMessage?.contains("email", ignoreCase = true) == true
                    )

                    // Notification Preference Dropdown
                    var prefExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = when(notificationPref) {
                                EmergencyContact.NotificationPreference.ALL -> "All (Voice + SMS + Email)"
                                EmergencyContact.NotificationPreference.VOICE_SMS -> "Voice & SMS"
                                EmergencyContact.NotificationPreference.SMS_ONLY -> "SMS Only"
                                EmergencyContact.NotificationPreference.VOICE_ONLY -> "Voice Call Only"
                            },
                            onValueChange = {},
                            label = { Text("Notification Method") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { prefExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = prefExpanded,
                            onDismissRequest = { prefExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All (Voice + SMS + Email)") },
                                onClick = {
                                    notificationPref = EmergencyContact.NotificationPreference.ALL
                                    prefExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Voice & SMS") },
                                onClick = {
                                    notificationPref = EmergencyContact.NotificationPreference.VOICE_SMS
                                    prefExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("SMS Only") },
                                onClick = {
                                    notificationPref = EmergencyContact.NotificationPreference.SMS_ONLY
                                    prefExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Voice Call Only") },
                                onClick = {
                                    notificationPref = EmergencyContact.NotificationPreference.VOICE_ONLY
                                    prefExpanded = false
                                }
                            )
                        }
                    }

                    // Info Box
                    Card(
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
                                text = "Important",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTextSecondary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This contact will receive your location and status updates when you activate SOS. Make sure they can be reached 24/7.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmicTextPrimary
                            )
                        }
                    }

                    // Error Message Display
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Log.d("AddContactScreen", "❌ Cancel button clicked")
                                onBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CosmicTextPrimary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                            )
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { saveContact() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicPrimary
                            ),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save Contact")
                            }
                        }
                    }
                }
            }
        }
    }

